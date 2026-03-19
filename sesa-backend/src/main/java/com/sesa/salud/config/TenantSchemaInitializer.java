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
                archivado        BOOLEAN NOT NULL DEFAULT FALSE,
                fecha_archivado  TIMESTAMPTZ,
                eliminado        BOOLEAN NOT NULL DEFAULT FALSE,
                fecha_eliminado  TIMESTAMPTZ,
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

    /** S9: Glosas (rechazos de factura) y adjuntos. */
    private static final String DDL_GLOSAS = """
            CREATE TABLE IF NOT EXISTS glosas (
                id BIGSERIAL PRIMARY KEY,
                factura_id BIGINT NOT NULL REFERENCES facturas(id) ON DELETE CASCADE,
                motivo_rechazo TEXT NOT NULL,
                estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
                fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                fecha_respuesta TIMESTAMP,
                observaciones TEXT,
                creado_por_id BIGINT REFERENCES usuarios(id),
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP
            )
            """;
    private static final String DDL_GLOSA_ADJUNTOS = """
            CREATE TABLE IF NOT EXISTS glosa_adjuntos (
                id BIGSERIAL PRIMARY KEY,
                glosa_id BIGINT NOT NULL REFERENCES glosas(id) ON DELETE CASCADE,
                nombre_archivo VARCHAR(255) NOT NULL,
                tipo VARCHAR(50),
                url_o_blob TEXT,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """;
    private static final List<String> DDL_GLOSAS_INDEXES = List.of(
            "CREATE INDEX IF NOT EXISTS idx_glosas_factura ON glosas (factura_id)",
            "CREATE INDEX IF NOT EXISTS idx_glosas_estado ON glosas (estado)",
            "CREATE INDEX IF NOT EXISTS idx_glosas_fecha_registro ON glosas (fecha_registro)",
            "CREATE INDEX IF NOT EXISTS idx_glosa_adjuntos_glosa ON glosa_adjuntos (glosa_id)"
    );

    /** Detalle multiclínea de factura (cuenta médica). */
    private static final String DDL_FACTURA_ITEMS = """
            CREATE TABLE IF NOT EXISTS factura_items (
                id BIGSERIAL PRIMARY KEY,
                factura_id BIGINT NOT NULL REFERENCES facturas(id) ON DELETE CASCADE,
                item_index INT NOT NULL DEFAULT 0,
                codigo_cups VARCHAR(20),
                descripcion_cups VARCHAR(500),
                tipo_servicio VARCHAR(40),
                cantidad INT NOT NULL DEFAULT 1,
                valor_unitario NUMERIC(14,2) NOT NULL,
                valor_total NUMERIC(14,2) NOT NULL,
                orden_clinica_item_id BIGINT REFERENCES orden_clinica_items(id)
            )
            """;
    private static final List<String> DDL_FACTURA_ITEMS_INDEXES = List.of(
            "CREATE INDEX IF NOT EXISTS idx_factura_items_factura ON factura_items (factura_id)"
    );

    /** Radicación de facturas ante EPS. */
    private static final String DDL_RADICACIONES = """
            CREATE TABLE IF NOT EXISTS radicaciones (
                id BIGSERIAL PRIMARY KEY,
                factura_id BIGINT NOT NULL REFERENCES facturas(id) ON DELETE CASCADE,
                fecha_radicacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                numero_radicado VARCHAR(80),
                eps_codigo VARCHAR(20),
                eps_nombre VARCHAR(200),
                estado VARCHAR(30) NOT NULL DEFAULT 'RADICADA',
                cuv VARCHAR(100),
                observaciones TEXT,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """;
    private static final List<String> DDL_RADICACIONES_INDEXES = List.of(
            "CREATE INDEX IF NOT EXISTS idx_radicaciones_factura ON radicaciones (factura_id)",
            "CREATE INDEX IF NOT EXISTS idx_radicaciones_fecha ON radicaciones (fecha_radicacion)",
            "CREATE INDEX IF NOT EXISTS idx_radicaciones_estado ON radicaciones (estado)"
    );

    /** S10: Cuestionario pre-consulta (ePRO). */
    private static final String DDL_CUESTIONARIO_PRECONSULTA = """
            CREATE TABLE IF NOT EXISTS cuestionario_preconsulta (
                id BIGSERIAL PRIMARY KEY,
                cita_id BIGINT NOT NULL REFERENCES citas(id) ON DELETE CASCADE,
                paciente_id BIGINT NOT NULL REFERENCES pacientes(id),
                motivo_palabras TEXT,
                dolor_eva INT,
                ansiedad_eva INT,
                medicamentos_actuales TEXT,
                alergias_referidas TEXT,
                enviado_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """;
    private static final List<String> DDL_CUESTIONARIO_PRECONSULTA_INDEXES = List.of(
            "CREATE INDEX IF NOT EXISTS idx_cuestionario_preconsulta_cita ON cuestionario_preconsulta (cita_id)",
            "CREATE INDEX IF NOT EXISTS idx_cuestionario_preconsulta_paciente ON cuestionario_preconsulta (paciente_id)"
    );

    // Recordatorios de cita y portal paciente (idempotente: ADD COLUMN IF NOT EXISTS).
    // Si aparece "no existe la columna p1_0.usuario_id", reiniciar el backend para que se apliquen estas migraciones.
    private static final List<String> DDL_RECORDATORIOS_MIGRATIONS = List.of(
            "ALTER TABLE notificaciones ADD COLUMN IF NOT EXISTS cita_id BIGINT",
            "ALTER TABLE notificacion_destinatarios ADD COLUMN IF NOT EXISTS archivado BOOLEAN NOT NULL DEFAULT FALSE",
            "ALTER TABLE notificacion_destinatarios ADD COLUMN IF NOT EXISTS fecha_archivado TIMESTAMPTZ",
            "ALTER TABLE notificacion_destinatarios ADD COLUMN IF NOT EXISTS eliminado BOOLEAN NOT NULL DEFAULT FALSE",
            "ALTER TABLE notificacion_destinatarios ADD COLUMN IF NOT EXISTS fecha_eliminado TIMESTAMPTZ",
            "ALTER TABLE citas ADD COLUMN IF NOT EXISTS recordatorio_24h_enviado_at TIMESTAMPTZ",
            "ALTER TABLE citas ADD COLUMN IF NOT EXISTS recordatorio_1h_enviado_at TIMESTAMPTZ",
            "ALTER TABLE citas ADD COLUMN IF NOT EXISTS token_confirmacion VARCHAR(64)",
            "ALTER TABLE citas ADD COLUMN IF NOT EXISTS confirmado_at TIMESTAMPTZ",
            "ALTER TABLE citas ADD COLUMN IF NOT EXISTS cancelado_desde_enlace_at TIMESTAMPTZ",
            "ALTER TABLE pacientes ADD COLUMN IF NOT EXISTS usuario_id BIGINT"
    );

    /** Migraciones consultas: examen físico por subáreas (check = bien). */
    private static final List<String> DDL_CONSULTAS_MIGRATIONS = List.of(
            "ALTER TABLE consultas ADD COLUMN IF NOT EXISTS examen_fisico_estructurado TEXT"
    );

    /** Migraciones urgencias: fecha inicio atención para reporte de cumplimiento Res. 5596/2015. */
    private static final List<String> DDL_URGENCIAS_MIGRATIONS = List.of(
            "ALTER TABLE urgencias ADD COLUMN IF NOT EXISTS fecha_hora_inicio_atencion TIMESTAMP",
            "ALTER TABLE urgencias ADD COLUMN IF NOT EXISTS alta_diagnostico TEXT",
            "ALTER TABLE urgencias ADD COLUMN IF NOT EXISTS alta_tratamiento TEXT",
            "ALTER TABLE urgencias ADD COLUMN IF NOT EXISTS alta_recomendaciones TEXT",
            "ALTER TABLE urgencias ADD COLUMN IF NOT EXISTS alta_proxima_cita TEXT"
    );

    /** S11: RDA urgencias/hospitalización — columnas en rda_envios. */
    private static final List<String> DDL_RDA_URGENCIAS_HOSPITALIZACION = List.of(
            "ALTER TABLE rda_envios ADD COLUMN IF NOT EXISTS urgencia_registro_id BIGINT REFERENCES urgencias(id)",
            "ALTER TABLE rda_envios ADD COLUMN IF NOT EXISTS hospitalizacion_id BIGINT REFERENCES hospitalizaciones(id)"
    );

    /** S12: Tabla API Keys para integradores. */
    private static final List<String> DDL_API_KEYS = List.of(
            """
            CREATE TABLE IF NOT EXISTS api_keys (
                id                BIGSERIAL PRIMARY KEY,
                nombre_integrador VARCHAR(150) NOT NULL,
                api_key_hash      VARCHAR(255) NOT NULL,
                api_key_index     VARCHAR(64)  NOT NULL,
                permisos          VARCHAR(200) NOT NULL DEFAULT 'LABORATORIO',
                activo            BOOLEAN      NOT NULL DEFAULT TRUE,
                created_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """,
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_api_keys_index ON api_keys (api_key_index)"
    );

    /** S6: Referencia (consulta/atención): motivo, nivel y datos para PDF. */
    private static final List<String> DDL_ATENCIONES_REFERENCIA_MIGRATIONS = List.of(
            "ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS referencia_motivo TEXT",
            "ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS referencia_nivel VARCHAR(50)",
            "ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS referencia_diagnostico TEXT",
            "ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS referencia_tratamiento TEXT",
            "ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS referencia_recomendaciones TEXT",
            "ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS referencia_proxima_cita TEXT"
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

    /** Catálogo CUPS Colombia (procedimientos, consultas, lab, imagenología). */
    private static final String DDL_CUPS_CATALOGO = """
            CREATE TABLE IF NOT EXISTS cups_catalogo (
                id              BIGSERIAL PRIMARY KEY,
                codigo          VARCHAR(20) NOT NULL,
                descripcion     VARCHAR(500) NOT NULL,
                capitulo        VARCHAR(100),
                tipo_servicio   VARCHAR(80) NOT NULL DEFAULT 'PROCEDIMIENTO',
                precio_sugerido NUMERIC(14,2),
                activo          BOOLEAN NOT NULL DEFAULT TRUE,
                created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                CONSTRAINT uk_cups_catalogo_codigo UNIQUE (codigo)
            )
            """;
    private static final List<String> DDL_CUPS_CATALOGO_INDEXES = List.of(
            "CREATE INDEX IF NOT EXISTS idx_cups_catalogo_codigo ON cups_catalogo (codigo)",
            "CREATE INDEX IF NOT EXISTS idx_cups_catalogo_tipo ON cups_catalogo (tipo_servicio)",
            "CREATE INDEX IF NOT EXISTS idx_cups_catalogo_activo ON cups_catalogo (activo) WHERE activo = TRUE"
    );

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
            "ALTER TABLE ordenes_clinicas ADD COLUMN IF NOT EXISTS resultado_registrado_por_id BIGINT REFERENCES personal(id)",
            "ALTER TABLE ordenes_clinicas ADD COLUMN IF NOT EXISTS resultado_critico BOOLEAN DEFAULT FALSE"
    );

    private static final String DDL_RESULTADO_CRITICO_LECTURA = """
            CREATE TABLE IF NOT EXISTS resultado_critico_lectura (
                id BIGSERIAL PRIMARY KEY,
                orden_clinica_id BIGINT NOT NULL REFERENCES ordenes_clinicas(id) ON DELETE CASCADE,
                personal_id BIGINT NOT NULL REFERENCES personal(id) ON DELETE CASCADE,
                leido_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT uk_resultado_critico_lectura_orden_personal UNIQUE (orden_clinica_id, personal_id)
            )
            """;

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

    /** S15: Guías de práctica clínica (GPC). */
    private static final String DDL_GUIA_GPC = """
            CREATE TABLE IF NOT EXISTS guia_gpc (
                id BIGSERIAL PRIMARY KEY,
                codigo_cie10 VARCHAR(20) NOT NULL,
                titulo VARCHAR(300) NOT NULL,
                criterios_control TEXT,
                medicamentos_primera_linea TEXT,
                estudios_seguimiento TEXT,
                fuente VARCHAR(200),
                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
            )
            """;
    private static final String DDL_GPC_SUGERENCIA_MOSTRADA = """
            CREATE TABLE IF NOT EXISTS gpc_sugerencia_mostrada (
                id BIGSERIAL PRIMARY KEY,
                atencion_id BIGINT NOT NULL REFERENCES atenciones(id) ON DELETE CASCADE,
                codigo_cie10 VARCHAR(20) NOT NULL,
                guia_id BIGINT NOT NULL REFERENCES guia_gpc(id) ON DELETE CASCADE,
                mostrado_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                profesional_id BIGINT REFERENCES personal(id)
            )
            """;
    private static final List<String> DDL_GPC_INDEXES = List.of(
            "CREATE INDEX IF NOT EXISTS idx_guia_gpc_codigo ON guia_gpc(codigo_cie10)",
            "CREATE INDEX IF NOT EXISTS idx_gpc_sugerencia_atencion ON gpc_sugerencia_mostrada(atencion_id)",
            "CREATE INDEX IF NOT EXISTS idx_gpc_sugerencia_guia ON gpc_sugerencia_mostrada(guia_id)"
    );

    /** S5: Reconciliación de medicamentos y alergias por atención. */
    private static final String DDL_RECONCILIACION_ATENCION = """
            CREATE TABLE IF NOT EXISTS reconciliacion_atencion (
                id BIGSERIAL PRIMARY KEY,
                atencion_id BIGINT NOT NULL REFERENCES atenciones(id) ON DELETE CASCADE,
                profesional_id BIGINT NOT NULL REFERENCES personal(id),
                medicamentos_referidos TEXT,
                medicamentos_hc TEXT,
                alergias_referidas TEXT,
                alergias_hc TEXT,
                reconciliado_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                observaciones TEXT,
                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                UNIQUE(atencion_id)
            )
            """;

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
                stmt.execute(DDL_GLOSAS);
                stmt.execute(DDL_GLOSA_ADJUNTOS);
                for (String idx : DDL_GLOSAS_INDEXES) {
                    stmt.execute(idx);
                }
                stmt.execute(DDL_FACTURA_ITEMS);
                for (String idx : DDL_FACTURA_ITEMS_INDEXES) {
                    stmt.execute(idx);
                }
                stmt.execute(DDL_RADICACIONES);
                for (String idx : DDL_RADICACIONES_INDEXES) {
                    stmt.execute(idx);
                }
                stmt.execute(DDL_CUESTIONARIO_PRECONSULTA);
                for (String idx : DDL_CUESTIONARIO_PRECONSULTA_INDEXES) {
                    stmt.execute(idx);
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
                for (String migration : DDL_CONSULTAS_MIGRATIONS) {
                    stmt.execute(migration);
                }
                for (String migration : DDL_URGENCIAS_MIGRATIONS) {
                    stmt.execute(migration);
                }
                for (String migration : DDL_RDA_URGENCIAS_HOSPITALIZACION) {
                    stmt.execute(migration);
                }
                for (String ddl : DDL_API_KEYS) {
                    stmt.execute(ddl);
                }
                for (String migration : DDL_ATENCIONES_REFERENCIA_MIGRATIONS) {
                    stmt.execute(migration);
                }
                stmt.execute(DDL_SIGNOS_VITALES_URGENCIA);
                stmt.execute(DDL_PLANTILLAS_SOAP);
                // Seed plantillas SOAP por tipo de consulta (Res. 1995/412 — ver docs/PLANTILLAS-HC-COLOMBIA.md)
                try {
                    stmt.execute("""
                        INSERT INTO plantillas_soap (nombre, motivo_tipo, contenido_subjetivo, contenido_objetivo, contenido_analisis, contenido_plan, codigo_cie10_sugerido)
                        SELECT a.nombre, a.motivo_tipo, a.contenido_subjetivo, a.contenido_objetivo, a.contenido_analisis, a.contenido_plan, a.codigo_cie10_sugerido
                        FROM (VALUES
                          ('Primera Infancia / Infancia', 'PRIMERA_VEZ', 'Motivo: control crecimiento y desarrollo, vacunación o enfermedad aguda. Antecedentes perinatales, alimentación, hitos del desarrollo.', 'Peso, talla, perímetro cefálico, signos vitales. Examen físico por sistemas. Valoración antropométrica.', 'Clasificación según edad. Detección alteraciones crecimiento/desarrollo. Impresión diagnóstica.', 'Controles según edad, vacunación, educación a padres, remisiones, seguimiento.', 'Z00.1'),
                          ('Prenatal 1ra vez', 'PRIMERA_VEZ', 'FUM, gestas/partos/abortos, motivo de consulta, antecedentes personales y familiares, medicamentos, alergias.', 'Examen físico general y obstétrico. TA, peso, talla. Fondo uterino si aplica. Laboratorio inicial (grupo, Rh, hemograma, glucemia, VDRL, VIH).', 'Edad gestacional, clasificación de riesgo (bajo/alto). Impresión diagnóstica.', 'Controles prenatales según normativa. Micronutrientes (ácido fólico, hierro). Educación. Paraclínicos. Referencia si riesgo alto.', 'Z34.0'),
                          ('Prenatal Control', 'CONTROL', 'Evolución del embarazo, movimientos fetales, síntomas (edema, cefalea, sangrado). Adherencia a recomendaciones.', 'TA, peso, altura uterina, FCF, edema. Hallazgos al examen. Resultados paraclínicos del trimestre.', 'Edad gestacional, clasificación de riesgo, cumplimiento de controles. Impresión diagnóstica.', 'Próximo control, estudios de tamizaje según EG, educación, referencia si procede.', 'Z34.8'),
                          ('Planificación 1ra vez Mujeres', 'PRIMERA_VEZ', 'Motivo: inicio de método anticonceptivo. Antecedentes ginecoobstétricos, ciclos, expectativas, contraindicaciones.', 'Examen físico general. TA. Examen ginecológico si aplica. Peso/talla.', 'Riesgo reproductivo. Método recomendado según perfil y preferencia. Impresión diagnóstica.', 'Método elegido, indicaciones de uso, seguimiento, signos de alarma, prevención ITS.', 'Z30.0'),
                          ('Planificación Control Mujeres', 'CONTROL', 'Evolución con el método (tolerancia, cumplimiento, efectos adversos). Dudas, deseo de cambio de método.', 'TA, peso si aplica. Examen según método (revisión implante/DIU).', 'Efectividad y continuidad del método. Impresión diagnóstica.', 'Continuar método, cambio si procede, refuerzo educativo, próximo control.', 'Z30.4'),
                          ('Control Adolescente / Jóven', 'CONTROL', 'Motivo: control, vacunación, salud sexual, salud mental o agudo. Antecedentes, hábitos, red de apoyo, riesgos.', 'Signos vitales, peso, talla, IMC. Examen físico por sistemas. Tamizajes según edad.', 'Clasificación de riesgo. Impresión diagnóstica. Necesidades de promoción y prevención.', 'Controles, vacunación (VPH, refuerzos), educación, referencia a planificación o salud mental.', 'Z00.1'),
                          ('Control del Adulto / Vejez', 'CONTROL', 'Motivo: control, detección temprana, seguimiento crónico. Antecedentes, medicamentos, factores de riesgo cardiovascular.', 'Signos vitales, peso, talla, IMC. Examen físico. Tamizajes según edad y sexo.', 'Riesgo cardiovascular y otros. Impresión diagnóstica. Condiciones crónicas.', 'Controles periódicos, estilos de vida, medicación crónica, referencia a especialidad si aplica.', 'Z00.0'),
                          ('Urgencias / Hospitalización', 'SEGUIMIENTO_AGUDO', 'Motivo de consulta/ingreso, enfermedad actual, antecedentes relevantes, medicamentos, alergias, último momento de bienestar.', 'Signos vitales, triage si aplica. Examen físico por sistemas. Paraclínicos de urgencia.', 'Impresión diagnóstica. Gravedad. Criterios de ingreso o alta.', 'Manejo (reanimación, medicación, procedimientos). Órdenes de enfermería. Alta o referencia. Seguimiento.', 'R07.4'),
                          ('Enf Cardiovasculares 1ra vez', 'PRIMERA_VEZ', 'Valoración cardiovascular, HTA, dolor torácico o disnea. Antecedentes personales y familiares ECV. Tabaquismo, dieta, actividad física.', 'TA (varias tomas si HTA), FC, peso, talla, IMC. Examen cardiovascular. ECG si aplica. Laboratorio (glucemia, lípidos, creatinina).', 'Riesgo cardiovascular (escalas). Impresión diagnóstica (HTA, dislipidemia, riesgo alto).', 'Estilo de vida, medicación si indicada, controles, metas TA y lípidos, referencia cardiología si procede.', 'I10'),
                          ('Anexo 3 - Autorización de servicios de salud', 'OTRO', 'Motivo de consulta relacionado con el procedimiento o servicio a autorizar. Antecedentes que justifican la solicitud.', 'Hallazgos que soportan la necesidad del servicio (examen, paraclínicos).', 'Justificación clínica para el procedimiento/servicio solicitado. Impresión diagnóstica.', 'Solicitud de autorización (Anexo 3). Procedimiento/servicio indicado. Seguimiento.', NULL)
                        ) AS a(nombre, motivo_tipo, contenido_subjetivo, contenido_objetivo, contenido_analisis, contenido_plan, codigo_cie10_sugerido)
                        WHERE NOT EXISTS (SELECT 1 FROM plantillas_soap LIMIT 1)
                        """);
                } catch (SQLException e) {
                    log.trace("Plantillas SOAP seed: {}", e.getMessage());
                }
                stmt.execute(DDL_CUPS_CATALOGO);
                for (String idx : DDL_CUPS_CATALOGO_INDEXES) {
                    try { stmt.execute(idx); } catch (SQLException e) { log.trace("CUPS catalog index: {}", e.getMessage()); }
                }
                // Seed CUPS (idempotente): insertar procedimientos más usados; ON CONFLICT evita duplicados
                try {
                    stmt.execute("""
                        INSERT INTO cups_catalogo (codigo, descripcion, capitulo, tipo_servicio, precio_sugerido) VALUES
                          ('890201', 'Consulta médica general', 'Cap 15 - Consulta', 'CONSULTA', 55000),
                          ('890202', 'Consulta médica especializada', 'Cap 15 - Consulta', 'CONSULTA', 85000),
                          ('890203', 'Consulta de urgencias', 'Cap 15 - Consulta', 'CONSULTA', 95000),
                          ('890301', 'Consulta de primera vez por odontología general', 'Cap 15 - Consulta', 'CONSULTA', 45000),
                          ('890302', 'Consulta de control o seguimiento por odontología general', 'Cap 15 - Consulta', 'CONSULTA', 35000),
                          ('903801', 'Hemograma completo', 'Cap 16 - Laboratorio', 'LABORATORIO', 25000),
                          ('903802', 'Glicemia', 'Cap 16 - Laboratorio', 'LABORATORIO', 8000),
                          ('903804', 'Perfil lipídico', 'Cap 16 - Laboratorio', 'LABORATORIO', 35000),
                          ('903806', 'Creatinina', 'Cap 16 - Laboratorio', 'LABORATORIO', 12000),
                          ('903810', 'Parcial de orina', 'Cap 16 - Laboratorio', 'LABORATORIO', 10000),
                          ('881601', 'Radiografía de tórax PA', 'Cap 17 - Imagen', 'IMAGENOLOGIA', 45000),
                          ('881801', 'Ecografía abdominal', 'Cap 17 - Imagen', 'IMAGENOLOGIA', 85000),
                          ('860001', 'Electrocardiograma', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 35000),
                          ('870101', 'Curaciones', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 25000),
                          ('870102', 'Sutura simple', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 55000)
                        ON CONFLICT (codigo) DO NOTHING
                        """);
                } catch (SQLException e) {
                    log.trace("CUPS catalog seed: {}", e.getMessage());
                }
                stmt.execute(DDL_CONSENTIMIENTOS_INFORMADOS);
                for (String migration : DDL_ORDENES_RESULTADO_MIGRATIONS) {
                    stmt.execute(migration);
                }
                stmt.execute(DDL_RESULTADO_CRITICO_LECTURA);
                for (String migration : DDL_FARMACIA_ORDENES_MIGRATIONS) {
                    stmt.execute(migration);
                }
                stmt.execute(DDL_ORDEN_CLINICA_ITEMS);
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_orden_clinica_items_orden ON orden_clinica_items (orden_id)");
                stmt.execute(DDL_RECONCILIACION_ATENCION);
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
                stmt.execute(DDL_GUIA_GPC);
                stmt.execute(DDL_GPC_SUGERENCIA_MOSTRADA);
                for (String idx : DDL_GPC_INDEXES) {
                    try { stmt.execute(idx); } catch (SQLException e) { log.trace("GPC index: {}", e.getMessage()); }
                }
                // S15: asegurar que created_at tenga DEFAULT ahora() y no existan filas nulas (esquemas antiguos)
                try {
                    stmt.execute("ALTER TABLE guia_gpc ALTER COLUMN created_at SET DEFAULT NOW()");
                    stmt.execute("UPDATE guia_gpc SET created_at = NOW() WHERE created_at IS NULL");
                } catch (SQLException e) {
                    log.trace("GPC created_at migration: {}", e.getMessage());
                }
                // S15: una fila de ejemplo si la tabla está vacía
                stmt.execute("""
                    INSERT INTO guia_gpc (codigo_cie10, titulo, criterios_control, medicamentos_primera_linea, estudios_seguimiento, fuente)
                    SELECT 'E11', 'Diabetes mellitus tipo 2', 'Control de glicemia, HbA1c, presión arterial, peso y pie diabético.', 'Metformina como primera línea; considerar iDPP-4 o iSGLT2 según perfil.', 'HbA1c cada 3-6 meses; creatinina y perfil lipídico anual; fondo de ojo según criterio.', 'Guía de práctica clínica - MinSalud'
                    WHERE NOT EXISTS (SELECT 1 FROM guia_gpc LIMIT 1)
                    """);
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
