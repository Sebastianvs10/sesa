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
            "CREATE INDEX IF NOT EXISTS idx_nd_notificacion_id      ON notificacion_destinatarios(notificacion_id)",
            // EBS: índices territoriales y de riesgo
            "CREATE INDEX IF NOT EXISTS idx_ebs_households_territory  ON ebs_households(territory_id)",
            "CREATE INDEX IF NOT EXISTS idx_ebs_households_state      ON ebs_households(state)",
            "CREATE INDEX IF NOT EXISTS idx_ebs_households_risk       ON ebs_households(risk_level)",
            "CREATE INDEX IF NOT EXISTS idx_ebs_home_visits_household ON ebs_home_visits(household_id, visit_date)",
            "CREATE INDEX IF NOT EXISTS idx_ebs_risk_patient          ON ebs_risk_assessments(patient_id)",
            "CREATE INDEX IF NOT EXISTS idx_ebs_risk_level            ON ebs_risk_assessments(risk_level)"
    );

    // ── Multi-rol Personal ────────────────────────────────────────────────────
    private static final String DDL_PERSONAL_ROLES = """
            CREATE TABLE IF NOT EXISTS personal_roles (
                personal_id BIGINT NOT NULL,
                rol         VARCHAR(50) NOT NULL,
                PRIMARY KEY (personal_id, rol),
                CONSTRAINT fk_personal_roles_personal FOREIGN KEY (personal_id) REFERENCES personal(id) ON DELETE CASCADE
            )
            """;

    // ── Migraciones de columnas (ALTER TABLE idempotentes) ───────────────────
    // Columnas normativas del módulo Personal (Res. 2003/2014, Ley 23/1981, Res. 1449/2016)
    private static final List<String> DDL_PERSONAL_MIGRATIONS = List.of(
            // existentes
            "ALTER TABLE personal ADD COLUMN IF NOT EXISTS tarjeta_profesional VARCHAR(30)",
            "ALTER TABLE personal ADD COLUMN IF NOT EXISTS especialidad_formal VARCHAR(150)",
            "ALTER TABLE personal ADD COLUMN IF NOT EXISTS numero_rethus VARCHAR(30)",
            // nuevas — tipo de documento (Res. 3374/2000 RIPS)
            "ALTER TABLE personal ADD COLUMN IF NOT EXISTS tipo_documento VARCHAR(10)",
            // nuevas — datos demográficos (RIPS / SISPRO)
            "ALTER TABLE personal ADD COLUMN IF NOT EXISTS fecha_nacimiento DATE",
            "ALTER TABLE personal ADD COLUMN IF NOT EXISTS sexo VARCHAR(10)",
            // nuevas — lugar de práctica (Res. 2003/2014 habilitación)
            "ALTER TABLE personal ADD COLUMN IF NOT EXISTS municipio VARCHAR(10)",
            "ALTER TABLE personal ADD COLUMN IF NOT EXISTS departamento VARCHAR(10)",
            // nuevas — vínculo laboral (Circular 047/2007 Min. Protección Social)
            "ALTER TABLE personal ADD COLUMN IF NOT EXISTS tipo_vinculacion VARCHAR(30)",
            "ALTER TABLE personal ADD COLUMN IF NOT EXISTS fecha_ingreso DATE",
            "ALTER TABLE personal ADD COLUMN IF NOT EXISTS fecha_retiro DATE"
    );

    // Dispositivos push (notificaciones móvil/web)
    private static final String DDL_DISPOSITIVOS_PUSH = """
            CREATE TABLE IF NOT EXISTS dispositivos_push (
                id          BIGSERIAL PRIMARY KEY,
                usuario_id  BIGINT NOT NULL,
                token       VARCHAR(512) NOT NULL,
                plataforma  VARCHAR(20) NOT NULL DEFAULT 'WEB',
                created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
            )
            """;

    // Receta electrónica (QR verificable)
    private static final String DDL_RECETAS_ELECTRONICAS = """
            CREATE TABLE IF NOT EXISTS recetas_electronicas (
                id                          BIGSERIAL PRIMARY KEY,
                token_verificacion          VARCHAR(64) NOT NULL UNIQUE,
                atencion_id                 BIGINT,
                paciente_id                 BIGINT NOT NULL,
                consulta_id                 BIGINT,
                medico_nombre               VARCHAR(200) NOT NULL,
                medico_tarjeta_profesional  VARCHAR(50),
                paciente_nombre             VARCHAR(200) NOT NULL,
                paciente_documento          VARCHAR(50),
                fecha_emision               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                diagnostico                 TEXT,
                observaciones               TEXT,
                valida_hasta                TIMESTAMPTZ,
                created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW()
            )
            """;

    private static final String DDL_RECETA_MEDICAMENTOS = """
            CREATE TABLE IF NOT EXISTS receta_medicamentos (
                id          BIGSERIAL PRIMARY KEY,
                receta_id   BIGINT NOT NULL,
                medicamento VARCHAR(200) NOT NULL,
                dosis       VARCHAR(100),
                frecuencia  VARCHAR(100),
                duracion    VARCHAR(100),
                CONSTRAINT fk_receta_med_receta FOREIGN KEY (receta_id) REFERENCES recetas_electronicas(id) ON DELETE CASCADE
            )
            """;

    // Facturación electrónica DIAN (Res. 000042 / UBL 2.1)
    private static final String DDL_FACTURACION_ELECTRONICA_CONFIG = """
            CREATE TABLE IF NOT EXISTS facturacion_electronica_config (
                id                         BIGSERIAL PRIMARY KEY,
                facturacion_activa         BOOLEAN      NOT NULL DEFAULT FALSE,
                nit                        VARCHAR(20),
                razon_social               VARCHAR(255),
                nombre_comercial           VARCHAR(255),
                regimen                    VARCHAR(50),
                direccion                  VARCHAR(255),
                municipio                  VARCHAR(100),
                departamento               VARCHAR(100),
                pais                       VARCHAR(100),
                email_contacto             VARCHAR(255),
                ambiente                   VARCHAR(20)   NOT NULL DEFAULT 'HABILITACION',
                numero_resolucion          VARCHAR(50),
                fecha_resolucion           DATE,
                prefijo                    VARCHAR(10),
                rango_desde                BIGINT,
                rango_hasta                BIGINT,
                clave_tecnica              VARCHAR(128),
                software_id                VARCHAR(64),
                software_pin               VARCHAR(64),
                plantilla_pdf              VARCHAR(100),
                created_at                 TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at                 TIMESTAMPTZ
            )
            """;

    private static final List<String> DDL_FACTURAS_ELECTRONICAS_MIGRATIONS = List.of(
            "ALTER TABLE facturas ADD COLUMN IF NOT EXISTS dian_cufe        VARCHAR(128)",
            "ALTER TABLE facturas ADD COLUMN IF NOT EXISTS dian_qr_url      VARCHAR(512)",
            "ALTER TABLE facturas ADD COLUMN IF NOT EXISTS dian_estado      VARCHAR(30)",
            "ALTER TABLE facturas ADD COLUMN IF NOT EXISTS dian_mensaje     TEXT",
            "ALTER TABLE facturas ADD COLUMN IF NOT EXISTS dian_xml_path    VARCHAR(500)",
            "ALTER TABLE facturas ADD COLUMN IF NOT EXISTS dian_pdf_path    VARCHAR(500)",
            "ALTER TABLE facturas ADD COLUMN IF NOT EXISTS dian_fecha_envio TIMESTAMPTZ"
    );

    // Recordatorios de cita y portal paciente (idempotente: ADD COLUMN IF NOT EXISTS).
    // Si aparece "no existe la columna p1_0.usuario_id", reiniciar el backend para que se apliquen estas migraciones.
    private static final List<String> DDL_RECORDATORIOS_MIGRATIONS = List.of(
            "ALTER TABLE notificaciones ADD COLUMN IF NOT EXISTS cita_id BIGINT",
            "ALTER TABLE citas ADD COLUMN IF NOT EXISTS recordatorio_24h_enviado_at TIMESTAMPTZ",
            "ALTER TABLE citas ADD COLUMN IF NOT EXISTS recordatorio_1h_enviado_at TIMESTAMPTZ",
            "ALTER TABLE pacientes ADD COLUMN IF NOT EXISTS usuario_id BIGINT"
    );

    /** Migraciones urgencias: fecha inicio atención para reporte de cumplimiento Res. 5596/2015. */
    private static final List<String> DDL_URGENCIAS_MIGRATIONS = List.of(
            "ALTER TABLE urgencias ADD COLUMN IF NOT EXISTS fecha_hora_inicio_atencion TIMESTAMP"
    );

    private static final String DDL_SIGNOS_VITALES_URGENCIA = """
            CREATE TABLE IF NOT EXISTS signos_vitales_urgencia (
                id BIGSERIAL PRIMARY KEY,
                urgencia_registro_id BIGINT NOT NULL REFERENCES urgencias(id) ON DELETE CASCADE,
                fecha_hora TIMESTAMP NOT NULL,
                presion_arterial VARCHAR(20),
                frecuencia_cardiaca VARCHAR(10),
                frecuencia_respiratoria VARCHAR(10),
                temperatura VARCHAR(10),
                saturacion_o2 VARCHAR(10),
                peso VARCHAR(10),
                dolor_eva VARCHAR(5),
                glasgow_ocular INT,
                glasgow_verbal INT,
                glasgow_motor INT,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """;

    /** Plantillas SOAP para evolución HC (Res. 1995/1999). */
    private static final String DDL_PLANTILLAS_SOAP = """
            CREATE TABLE IF NOT EXISTS plantillas_soap (
                id BIGSERIAL PRIMARY KEY,
                nombre VARCHAR(150) NOT NULL,
                motivo_tipo VARCHAR(50),
                contenido_subjetivo TEXT,
                contenido_objetivo TEXT,
                contenido_analisis TEXT,
                contenido_plan TEXT,
                codigo_cie10_sugerido VARCHAR(20),
                activo BOOLEAN NOT NULL DEFAULT true,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """;

    /** Consentimientos informados (Ley 23/1981, Res. 3380/1981). */
    private static final String DDL_CONSENTIMIENTOS_INFORMADOS = """
            CREATE TABLE IF NOT EXISTS consentimientos_informados (
                id                  BIGSERIAL PRIMARY KEY,
                paciente_id         BIGINT NOT NULL REFERENCES pacientes(id),
                profesional_id      BIGINT NOT NULL REFERENCES personal(id),
                tipo                VARCHAR(30) NOT NULL,
                estado              VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
                procedimiento       VARCHAR(300),
                fecha_solicitud     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                fecha_firma         TIMESTAMPTZ,
                observaciones       TEXT,
                firma_canvas_data   TEXT,
                created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                updated_at          TIMESTAMPTZ DEFAULT NOW()
            )
            """;

    private static final List<String> DDL_EBS_IGAC_MIGRATIONS = List.of(
            "ALTER TABLE ebs_territories ADD COLUMN IF NOT EXISTS igac_departamento_codigo VARCHAR(2)",
            "ALTER TABLE ebs_territories ADD COLUMN IF NOT EXISTS igac_municipio_codigo VARCHAR(5)",
            "ALTER TABLE ebs_territories ADD COLUMN IF NOT EXISTS igac_vereda_codigo VARCHAR(20)"
    );

    private static final String DDL_EBS_BRIGADES = """
            CREATE TABLE IF NOT EXISTS ebs_brigades (
                id           BIGSERIAL PRIMARY KEY,
                name         VARCHAR(200) NOT NULL,
                territory_id BIGINT NOT NULL REFERENCES ebs_territories(id),
                date_start   DATE NOT NULL,
                date_end     DATE NOT NULL,
                status       VARCHAR(30) NOT NULL DEFAULT 'PROGRAMADA',
                notes        TEXT,
                created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
            )
            """;

    private static final String DDL_EBS_TERRITORY_TEAM = """
            CREATE TABLE IF NOT EXISTS ebs_territory_team (
                territory_id BIGINT NOT NULL REFERENCES ebs_territories(id) ON DELETE CASCADE,
                personal_id  BIGINT NOT NULL REFERENCES personal(id) ON DELETE CASCADE,
                PRIMARY KEY (territory_id, personal_id)
            )
            """;

    private static final String DDL_EBS_BRIGADE_TEAM = """
            CREATE TABLE IF NOT EXISTS ebs_brigade_team (
                brigade_id   BIGINT NOT NULL REFERENCES ebs_brigades(id) ON DELETE CASCADE,
                personal_id  BIGINT NOT NULL REFERENCES personal(id) ON DELETE CASCADE,
                PRIMARY KEY (brigade_id, personal_id)
            )
            """;

    private static final String DDL_EBS_ALERTS = """
            CREATE TABLE IF NOT EXISTS ebs_alerts (
                id                   BIGSERIAL PRIMARY KEY,
                type                 VARCHAR(50) NOT NULL,
                vereda_codigo        VARCHAR(20),
                municipio_codigo    VARCHAR(5),
                departamento_codigo  VARCHAR(2),
                title                VARCHAR(300) NOT NULL,
                description         TEXT,
                alert_date          DATE NOT NULL,
                status              VARCHAR(30) NOT NULL DEFAULT 'ACTIVA',
                external_id         VARCHAR(64),
                created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
            )
            """;

    private static final List<String> DDL_ORDENES_RESULTADO_MIGRATIONS = List.of(
            "ALTER TABLE ordenes_clinicas ADD COLUMN IF NOT EXISTS resultado TEXT",
            "ALTER TABLE ordenes_clinicas ADD COLUMN IF NOT EXISTS fecha_resultado TIMESTAMPTZ",
            "ALTER TABLE ordenes_clinicas ADD COLUMN IF NOT EXISTS resultado_registrado_por_id BIGINT REFERENCES personal(id)"
    );

    private static final List<String> DDL_FARMACIA_ORDENES_MIGRATIONS = List.of(
            "ALTER TABLE ordenes_clinicas ADD COLUMN IF NOT EXISTS estado_dispensacion_farmacia VARCHAR(30) DEFAULT 'PENDIENTE'",
            "ALTER TABLE ordenes_clinicas ADD COLUMN IF NOT EXISTS cantidad_prescrita INT",
            "ALTER TABLE ordenes_clinicas ADD COLUMN IF NOT EXISTS unidad_medida VARCHAR(30)",
            "ALTER TABLE ordenes_clinicas ADD COLUMN IF NOT EXISTS frecuencia VARCHAR(120)",
            "ALTER TABLE ordenes_clinicas ADD COLUMN IF NOT EXISTS duracion_dias INT",
            "ALTER TABLE farmacia_dispensaciones ADD COLUMN IF NOT EXISTS orden_clinica_id BIGINT REFERENCES ordenes_clinicas(id)"
    );

    private static final String DDL_ORDEN_CLINICA_ITEMS = """
            CREATE TABLE IF NOT EXISTS orden_clinica_items (
                id BIGSERIAL PRIMARY KEY,
                orden_id BIGINT NOT NULL REFERENCES ordenes_clinicas(id) ON DELETE CASCADE,
                tipo VARCHAR(50) NOT NULL,
                detalle TEXT,
                cantidad_prescrita INT,
                unidad_medida VARCHAR(30),
                frecuencia VARCHAR(120),
                duracion_dias INT,
                valor_estimado NUMERIC(14,2),
                orden_item_index INT NOT NULL DEFAULT 0
            )
            """;

    private static final List<String> DDL_EBS_VISIT_MIGRATIONS = List.of(
            "ALTER TABLE ebs_home_visits ADD COLUMN IF NOT EXISTS brigade_id BIGINT REFERENCES ebs_brigades(id)",
            "ALTER TABLE ebs_home_visits ADD COLUMN IF NOT EXISTS tipo_intervencion VARCHAR(80)",
            "ALTER TABLE ebs_home_visits ADD COLUMN IF NOT EXISTS vereda_codigo VARCHAR(20)",
            "ALTER TABLE ebs_home_visits ADD COLUMN IF NOT EXISTS diagnostico_cie10 VARCHAR(20)",
            "ALTER TABLE ebs_home_visits ADD COLUMN IF NOT EXISTS plan_cuidado TEXT"
    );

    @Override
    public void run(String... args) {
        // Aplicar también en schema public (usuarios superadmin)
        try {
            applyDdlToSchema(TenantContextHolder.PUBLIC);
            log.info("TenantSchemaInitializer: schema 'public' verificado/actualizado");
        } catch (Exception ex) {
            log.error("TenantSchemaInitializer: error al aplicar DDL en schema 'public': {}", ex.getMessage());
        }

        List<String> schemas = empresaRepository.findAll()
                .stream()
                .filter(e -> Boolean.TRUE.equals(e.getActivo()))
                .map(e -> e.getSchemaName())
                .filter(s -> !TenantContextHolder.PUBLIC.equalsIgnoreCase(s))
                .toList();

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
                stmt.execute(DDL_DISPOSITIVOS_PUSH);
                // receta electrónica (antes que receta_medicamentos por FK)
                stmt.execute(DDL_RECETAS_ELECTRONICAS);
                stmt.execute(DDL_RECETA_MEDICAMENTOS);
                // EBS: tablas territoriales y de visitas domiciliarias
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS ebs_territories (
                            id                  BIGSERIAL PRIMARY KEY,
                            code                VARCHAR(50) NOT NULL UNIQUE,
                            name                VARCHAR(200) NOT NULL,
                            type                VARCHAR(50),
                            parent_territory_id BIGINT REFERENCES ebs_territories(id),
                            geometry            TEXT,
                            assigned_team_id    BIGINT,
                            active              BOOLEAN NOT NULL DEFAULT TRUE,
                            created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
                        )
                        """);
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS ebs_households (
                            id               BIGSERIAL PRIMARY KEY,
                            territory_id     BIGINT NOT NULL REFERENCES ebs_territories(id),
                            fhir_location_id VARCHAR(64),
                            address_text     VARCHAR(255),
                            latitude         NUMERIC(10,6),
                            longitude        NUMERIC(10,6),
                            rural            BOOLEAN,
                            stratum          VARCHAR(20),
                            state            VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE_VISITA',
                            risk_level       VARCHAR(20),
                            created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
                        )
                        """);
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS ebs_family_groups (
                            id                      BIGSERIAL PRIMARY KEY,
                            household_id            BIGINT NOT NULL REFERENCES ebs_households(id),
                            fhir_group_id           VARCHAR(64),
                            main_contact_patient_id BIGINT REFERENCES pacientes(id),
                            socioeconomic_level     VARCHAR(30),
                            risk_notes              TEXT,
                            created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
                        )
                        """);
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS ebs_home_visits (
                            id              BIGSERIAL PRIMARY KEY,
                            household_id    BIGINT NOT NULL REFERENCES ebs_households(id),
                            family_group_id BIGINT REFERENCES ebs_family_groups(id),
                            professional_id BIGINT REFERENCES personal(id),
                            visit_date      TIMESTAMPTZ NOT NULL,
                            visit_type      VARCHAR(50),
                            motivo          TEXT,
                            notes           TEXT,
                            fhir_encounter_id VARCHAR(64),
                            status          VARCHAR(30) NOT NULL DEFAULT 'EN_PROCESO',
                            offline_uuid    VARCHAR(64),
                            sync_status     VARCHAR(20) NOT NULL DEFAULT 'SYNCED',
                            sync_errors     TEXT,
                            created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
                        )
                        """);
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS ebs_risk_assessments (
                            id                  BIGSERIAL PRIMARY KEY,
                            patient_id          BIGINT NOT NULL REFERENCES pacientes(id),
                            home_visit_id       BIGINT REFERENCES ebs_home_visits(id),
                            category            VARCHAR(30) NOT NULL,
                            score               NUMERIC(5,2),
                            risk_level          VARCHAR(20),
                            fhir_observation_id VARCHAR(64),
                            valid_from          TIMESTAMPTZ,
                            valid_to            TIMESTAMPTZ,
                            created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
                        )
                        """);
                // facturación electrónica DIAN (tabla + columnas en facturas)
                stmt.execute(DDL_FACTURACION_ELECTRONICA_CONFIG);
                for (String migration : DDL_FACTURAS_ELECTRONICAS_MIGRATIONS) {
                    stmt.execute(migration);
                }
                // índices
                for (String idx : DDL_INDEXES) {
                    stmt.execute(idx);
                }
                // multi-rol personal
                stmt.execute(DDL_PERSONAL_ROLES);
                // migraciones de columnas faltantes en tablas existentes
                for (String migration : DDL_PERSONAL_MIGRATIONS) {
                    stmt.execute(migration);
                }
                for (String migration : DDL_RECORDATORIOS_MIGRATIONS) {
                    stmt.execute(migration);
                }
                for (String migration : DDL_URGENCIAS_MIGRATIONS) {
                    stmt.execute(migration);
                }
                stmt.execute(DDL_SIGNOS_VITALES_URGENCIA);
                stmt.execute(DDL_PLANTILLAS_SOAP);
                stmt.execute(DDL_CONSENTIMIENTOS_INFORMADOS);
                for (String migration : DDL_ORDENES_RESULTADO_MIGRATIONS) {
                    stmt.execute(migration);
                }
                for (String migration : DDL_FARMACIA_ORDENES_MIGRATIONS) {
                    stmt.execute(migration);
                }
                stmt.execute(DDL_ORDEN_CLINICA_ITEMS);
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_orden_clinica_items_orden ON orden_clinica_items (orden_id)");
                for (String migration : DDL_EBS_IGAC_MIGRATIONS) {
                    stmt.execute(migration);
                }
                // EBS: brigadas, equipos, alertas y columnas extra en visitas
                stmt.execute(DDL_EBS_BRIGADES);
                stmt.execute(DDL_EBS_TERRITORY_TEAM);
                stmt.execute(DDL_EBS_BRIGADE_TEAM);
                stmt.execute(DDL_EBS_ALERTS);
                for (String m : DDL_EBS_VISIT_MIGRATIONS) {
                    try { stmt.execute(m); } catch (SQLException e) { log.trace("EBS visit migration: {}", e.getMessage()); }
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
