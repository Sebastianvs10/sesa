/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.config;

import com.sesa.salud.repository.master.EmpresaRepository;
import com.sesa.salud.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Al arrancar la aplicación, garantiza que todas las tablas de entidades
 * nuevas existan en cada schema de tenant registrado.
 *
 * <p>Hibernate ddl-auto=update sólo crea las tablas en el schema activo al
 * momento de inicialización (public). Este componente se ejecuta después y
 * aplica el DDL de las entidades nuevas en todos los tenants de forma
 * idempotente (CREATE TABLE IF NOT EXISTS).</p>
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class TenantSchemaInitializer implements CommandLineRunner {

    private final DataSource dataSource;
    private final EmpresaRepository empresaRepository;

    // ── Módulo Agenda de Turnos ──────────────────────────────────────────────

    private static final String DDL_PROGRAMACION_MES = """
            CREATE TABLE IF NOT EXISTS programacion_mes (
                id                  BIGSERIAL PRIMARY KEY,
                anio                INTEGER NOT NULL,
                mes                 INTEGER NOT NULL,
                estado              VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
                creado_por_id       BIGINT,
                creado_por_nombre   VARCHAR(200),
                aprobado_por_id     BIGINT,
                aprobado_por_nombre VARCHAR(200),
                fecha_aprobacion    TIMESTAMPTZ,
                observaciones       TEXT,
                created_at          TIMESTAMPTZ NOT NULL,
                updated_at          TIMESTAMPTZ,
                CONSTRAINT uk_programacion_anio_mes UNIQUE (anio, mes)
            )
            """;

    private static final String DDL_TURNOS = """
            CREATE TABLE IF NOT EXISTS turnos (
                id                  BIGSERIAL PRIMARY KEY,
                personal_id         BIGINT NOT NULL,
                programacion_mes_id BIGINT NOT NULL,
                servicio            VARCHAR(30) NOT NULL,
                tipo_turno          VARCHAR(20) NOT NULL,
                fecha_inicio        TIMESTAMP NOT NULL,
                fecha_fin           TIMESTAMP NOT NULL,
                duracion_horas      INTEGER NOT NULL,
                estado              VARCHAR(15) NOT NULL DEFAULT 'BORRADOR',
                es_festivo          BOOLEAN NOT NULL DEFAULT FALSE,
                notas               TEXT,
                modificado_por_id   BIGINT,
                created_at          TIMESTAMPTZ NOT NULL,
                updated_at          TIMESTAMPTZ,
                CONSTRAINT fk_turno_personal     FOREIGN KEY (personal_id)         REFERENCES personal(id),
                CONSTRAINT fk_turno_programacion FOREIGN KEY (programacion_mes_id) REFERENCES programacion_mes(id)
            )
            """;

    // ── Módulo Notificaciones ────────────────────────────────────────────────

    private static final String DDL_NOTIFICACIONES = """
            CREATE TABLE IF NOT EXISTS notificaciones (
                id               BIGSERIAL PRIMARY KEY,
                titulo           VARCHAR(255) NOT NULL,
                contenido        TEXT NOT NULL,
                tipo             VARCHAR(30) DEFAULT 'GENERAL',
                remitente_id     BIGINT NOT NULL,
                remitente_nombre VARCHAR(200),
                fecha_envio      TIMESTAMPTZ NOT NULL,
                created_at       TIMESTAMPTZ NOT NULL
            )
            """;

    private static final String DDL_NOTIFICACION_DESTINATARIOS = """
            CREATE TABLE IF NOT EXISTS notificacion_destinatarios (
                id               BIGSERIAL PRIMARY KEY,
                notificacion_id  BIGINT NOT NULL,
                usuario_id       BIGINT NOT NULL,
                usuario_email    VARCHAR(255),
                usuario_nombre   VARCHAR(200),
                leido            BOOLEAN NOT NULL DEFAULT FALSE,
                fecha_lectura    TIMESTAMPTZ,
                CONSTRAINT fk_nd_notificacion FOREIGN KEY (notificacion_id) REFERENCES notificaciones(id)
            )
            """;

    private static final String DDL_NOTIFICACION_ADJUNTOS = """
            CREATE TABLE IF NOT EXISTS notificacion_adjuntos (
                id               BIGSERIAL PRIMARY KEY,
                notificacion_id  BIGINT NOT NULL,
                nombre_archivo   VARCHAR(255) NOT NULL,
                content_type     VARCHAR(100),
                tamano           BIGINT,
                datos            BYTEA,
                CONSTRAINT fk_na_notificacion FOREIGN KEY (notificacion_id) REFERENCES notificaciones(id)
            )
            """;

    // ── Índices ──────────────────────────────────────────────────────────────

    private static final List<String> DDL_INDEXES = List.of(
            "CREATE INDEX IF NOT EXISTS idx_turno_personal         ON turnos(personal_id)",
            "CREATE INDEX IF NOT EXISTS idx_turno_programacion_mes  ON turnos(programacion_mes_id)",
            "CREATE INDEX IF NOT EXISTS idx_turno_fecha_inicio      ON turnos(fecha_inicio)",
            "CREATE INDEX IF NOT EXISTS idx_nd_usuario_id           ON notificacion_destinatarios(usuario_id)",
            "CREATE INDEX IF NOT EXISTS idx_nd_notificacion_id      ON notificacion_destinatarios(notificacion_id)"
    );

    @Override
    public void run(String... args) {
        List<String> schemas = empresaRepository.findAll()
                .stream()
                .filter(e -> Boolean.TRUE.equals(e.getActivo()))
                .map(e -> e.getSchemaName())
                .toList();

        if (schemas.isEmpty()) {
            log.info("TenantSchemaInitializer: no hay empresas activas, omitiendo");
            return;
        }

        for (String schema : schemas) {
            try {
                applyDdlToSchema(schema);
                log.info("TenantSchemaInitializer: schema '{}' verificado/actualizado", schema);
            } catch (Exception ex) {
                log.error("TenantSchemaInitializer: error al aplicar DDL en schema '{}': {}", schema, ex.getMessage());
            }
        }
    }

    public void applyDdlToSchema(String schema) throws SQLException {
        TenantContextHolder.setTenantSchema(schema);
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            conn.setAutoCommit(false);
            try {
                // Apuntar al schema correcto antes de cualquier DDL
                stmt.execute("SET search_path = '" + schema + "'");
                // agenda de turnos (programacion_mes debe ir antes que turnos por FK)
                stmt.execute(DDL_PROGRAMACION_MES);
                stmt.execute(DDL_TURNOS);
                // notificaciones (notificaciones antes que sus hijos por FK)
                stmt.execute(DDL_NOTIFICACIONES);
                stmt.execute(DDL_NOTIFICACION_DESTINATARIOS);
                stmt.execute(DDL_NOTIFICACION_ADJUNTOS);
                // índices
                for (String idx : DDL_INDEXES) {
                    stmt.execute(idx);
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } finally {
            TenantContextHolder.clear();
        }
    }
}
