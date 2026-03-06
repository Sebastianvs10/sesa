-- Tablas por tenant (ejecutar con search_path = schema_tenant)
CREATE TABLE IF NOT EXISTS usuarios (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nombre_completo VARCHAR(200),
    activo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS usuario_roles (
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    rol VARCHAR(50) NOT NULL,
    PRIMARY KEY (usuario_id, rol)
);

CREATE TABLE IF NOT EXISTS eps (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(200) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS pacientes (
    id BIGSERIAL PRIMARY KEY,
    tipo_documento VARCHAR(10),
    documento VARCHAR(50) NOT NULL UNIQUE,
    nombres VARCHAR(150) NOT NULL,
    apellidos VARCHAR(150),
    fecha_nacimiento DATE,
    sexo VARCHAR(20),
    grupo_sanguineo VARCHAR(10),
    telefono VARCHAR(30),
    email VARCHAR(255),
    direccion VARCHAR(255),
    eps_id BIGINT REFERENCES eps(id),
    activo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Campos normativos Res. 3374/2000 (RIPS - Archivo CT)
    municipio_residencia VARCHAR(10),
    departamento_residencia VARCHAR(10),
    zona_residencia VARCHAR(10) DEFAULT 'URBANA',
    regimen_afiliacion VARCHAR(20),
    tipo_usuario VARCHAR(30),
    -- Contacto de emergencia / acudiente
    contacto_emergencia_nombre VARCHAR(150),
    contacto_emergencia_telefono VARCHAR(30),
    -- Datos sociodemográficos para SISPRO
    estado_civil VARCHAR(20),
    escolaridad VARCHAR(50),
    ocupacion VARCHAR(100),
    pertenencia_etnica VARCHAR(50),
    -- Portal del paciente: usuario vinculado (notificaciones, consentimientos)
    usuario_id BIGINT
);

-- Columnas para pacientes existentes (migraciones incrementales)
ALTER TABLE pacientes ADD COLUMN IF NOT EXISTS municipio_residencia VARCHAR(10);
ALTER TABLE pacientes ADD COLUMN IF NOT EXISTS departamento_residencia VARCHAR(10);
ALTER TABLE pacientes ADD COLUMN IF NOT EXISTS zona_residencia VARCHAR(10) DEFAULT 'URBANA';
ALTER TABLE pacientes ADD COLUMN IF NOT EXISTS regimen_afiliacion VARCHAR(20);
ALTER TABLE pacientes ADD COLUMN IF NOT EXISTS tipo_usuario VARCHAR(30);
ALTER TABLE pacientes ADD COLUMN IF NOT EXISTS contacto_emergencia_nombre VARCHAR(150);
ALTER TABLE pacientes ADD COLUMN IF NOT EXISTS contacto_emergencia_telefono VARCHAR(30);
ALTER TABLE pacientes ADD COLUMN IF NOT EXISTS estado_civil VARCHAR(20);
ALTER TABLE pacientes ADD COLUMN IF NOT EXISTS escolaridad VARCHAR(50);
ALTER TABLE pacientes ADD COLUMN IF NOT EXISTS ocupacion VARCHAR(100);
ALTER TABLE pacientes ADD COLUMN IF NOT EXISTS pertenencia_etnica VARCHAR(50);
ALTER TABLE pacientes ADD COLUMN IF NOT EXISTS usuario_id BIGINT;

CREATE TABLE IF NOT EXISTS personal (
    id BIGSERIAL PRIMARY KEY,
    nombres VARCHAR(150) NOT NULL,
    apellidos VARCHAR(150),
    identificacion VARCHAR(50),
    primer_nombre VARCHAR(80),
    segundo_nombre VARCHAR(80),
    primer_apellido VARCHAR(80),
    segundo_apellido VARCHAR(80),
    celular VARCHAR(30),
    email VARCHAR(255),
    rol VARCHAR(50),
    foto_url VARCHAR(500),
    foto_data BYTEA,
    foto_content_type VARCHAR(100),
    firma_url VARCHAR(500),
    firma_data BYTEA,
    firma_content_type VARCHAR(100),
    usuario_id BIGINT REFERENCES usuarios(id),
    activo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Campos normativos Res. 2003/2014, Ley 23/1981, Res. 1449/2016
    tarjeta_profesional VARCHAR(30),
    especialidad_formal VARCHAR(150),
    numero_rethus VARCHAR(30)
);

ALTER TABLE personal ADD COLUMN IF NOT EXISTS tarjeta_profesional VARCHAR(30);
ALTER TABLE personal ADD COLUMN IF NOT EXISTS especialidad_formal VARCHAR(150);
ALTER TABLE personal ADD COLUMN IF NOT EXISTS numero_rethus VARCHAR(30);

CREATE TABLE IF NOT EXISTS historias_clinicas (
    id BIGSERIAL PRIMARY KEY,
    paciente_id BIGINT NOT NULL UNIQUE REFERENCES pacientes(id) ON DELETE CASCADE,
    fecha_apertura TIMESTAMP NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    grupo_sanguineo VARCHAR(10),
    alergias_generales TEXT,
    antecedentes_personales TEXT,
    antecedentes_quirurgicos TEXT,
    antecedentes_farmacologicos TEXT,
    antecedentes_traumaticos TEXT,
    antecedentes_ginecoobstetricos TEXT,
    antecedentes_familiares TEXT,
    habitos_tabaco BOOLEAN DEFAULT false,
    habitos_alcohol BOOLEAN DEFAULT false,
    habitos_sustancias BOOLEAN DEFAULT false,
    habitos_detalles TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS dolores (
    id BIGSERIAL PRIMARY KEY,
    paciente_id BIGINT NOT NULL REFERENCES pacientes(id) ON DELETE CASCADE,
    historia_clinica_id BIGINT REFERENCES historias_clinicas(id) ON DELETE SET NULL,
    zona_corporal VARCHAR(60) NOT NULL,
    zona_label VARCHAR(120) NOT NULL,
    tipo_dolor VARCHAR(40),
    intensidad INT NOT NULL,
    severidad VARCHAR(20) DEFAULT 'leve',
    estado VARCHAR(20) NOT NULL DEFAULT 'activo',
    fecha_inicio TIMESTAMP NOT NULL,
    fecha_resolucion TIMESTAMP,
    descripcion TEXT,
    factores_agravantes TEXT,
    factores_aliviantes TEXT,
    tratamiento TEXT,
    notas TEXT,
    vista VARCHAR(10) DEFAULT 'front',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS atenciones (
    id BIGSERIAL PRIMARY KEY,
    historia_id BIGINT NOT NULL REFERENCES historias_clinicas(id) ON DELETE CASCADE,
    profesional_id BIGINT NOT NULL REFERENCES personal(id),
    fecha_atencion TIMESTAMP NOT NULL,
    motivo_consulta TEXT,
    enfermedad_actual TEXT,
    version_enfermedad TEXT,
    sintomas_asociados TEXT,
    factores_mejoran TEXT,
    factores_empeoran TEXT,
    revision_sistemas TEXT,
    presion_arterial VARCHAR(20),
    frecuencia_cardiaca VARCHAR(10),
    frecuencia_respiratoria VARCHAR(10),
    temperatura VARCHAR(10),
    peso VARCHAR(10),
    talla VARCHAR(10),
    imc VARCHAR(10),
    evaluacion_general TEXT,
    hallazgos TEXT,
    diagnostico TEXT,
    codigo_cie10 VARCHAR(20),
    plan_tratamiento TEXT,
    tratamiento_farmacologico TEXT,
    ordenes_medicas TEXT,
    examenes_solicitados TEXT,
    incapacidad TEXT,
    recomendaciones TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS diagnosticos (
    id BIGSERIAL PRIMARY KEY,
    atencion_id BIGINT NOT NULL REFERENCES atenciones(id) ON DELETE CASCADE,
    codigo_cie10 VARCHAR(20) NOT NULL,
    descripcion TEXT,
    tipo VARCHAR(20) NOT NULL DEFAULT 'PRINCIPAL',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS procedimientos (
    id BIGSERIAL PRIMARY KEY,
    atencion_id BIGINT NOT NULL REFERENCES atenciones(id) ON DELETE CASCADE,
    codigo_cups VARCHAR(20) NOT NULL,
    descripcion TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS formulas_medicas (
    id BIGSERIAL PRIMARY KEY,
    atencion_id BIGINT NOT NULL REFERENCES atenciones(id) ON DELETE CASCADE,
    medicamento VARCHAR(200) NOT NULL,
    dosis VARCHAR(100),
    frecuencia VARCHAR(100),
    duracion VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS laboratorios_atencion (
    id BIGSERIAL PRIMARY KEY,
    atencion_id BIGINT NOT NULL REFERENCES atenciones(id) ON DELETE CASCADE,
    tipo_examen VARCHAR(150) NOT NULL,
    resultado TEXT,
    fecha_resultado TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS imagenes_diagnosticas (
    id BIGSERIAL PRIMARY KEY,
    atencion_id BIGINT NOT NULL REFERENCES atenciones(id) ON DELETE CASCADE,
    tipo VARCHAR(100),
    resultado TEXT,
    url_archivo VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS evoluciones (
    id BIGSERIAL PRIMARY KEY,
    atencion_id BIGINT NOT NULL REFERENCES atenciones(id) ON DELETE CASCADE,
    nota_evolucion TEXT NOT NULL,
    fecha TIMESTAMP NOT NULL,
    profesional_id BIGINT NOT NULL REFERENCES personal(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notas_enfermeria (
    id BIGSERIAL PRIMARY KEY,
    atencion_id BIGINT NOT NULL REFERENCES atenciones(id) ON DELETE CASCADE,
    nota TEXT NOT NULL,
    fecha_nota TIMESTAMP NOT NULL,
    profesional_id BIGINT REFERENCES personal(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS consentimientos (
    id BIGSERIAL PRIMARY KEY,
    atencion_id BIGINT NOT NULL REFERENCES atenciones(id) ON DELETE CASCADE,
    tipo VARCHAR(200) NOT NULL,
    descripcion TEXT,
    firma_digital TEXT,
    evidencia_url VARCHAR(500),
    fecha_consentimiento TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS auditoria_historia (
    id BIGSERIAL PRIMARY KEY,
    tabla_afectada VARCHAR(100) NOT NULL,
    registro_id BIGINT,
    accion VARCHAR(20) NOT NULL,
    usuario VARCHAR(255),
    fecha TIMESTAMP NOT NULL,
    ip VARCHAR(45),
    valor_antes TEXT,
    valor_despues TEXT
);

CREATE TABLE IF NOT EXISTS citas (
    id BIGSERIAL PRIMARY KEY,
    paciente_id BIGINT NOT NULL REFERENCES pacientes(id),
    personal_id BIGINT NOT NULL REFERENCES personal(id),
    servicio VARCHAR(100) NOT NULL,
    fecha_hora TIMESTAMP NOT NULL,
    estado VARCHAR(50) DEFAULT 'AGENDADA',
    notas TEXT,
    motivo_cancelacion TEXT,
    tipo_cita VARCHAR(20),
    numero_autorizacion_eps VARCHAR(60),
    duracion_estimada_min INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    recordatorio_24h_enviado_at TIMESTAMPTZ,
    recordatorio_1h_enviado_at TIMESTAMPTZ
);
ALTER TABLE citas ADD COLUMN IF NOT EXISTS motivo_cancelacion TEXT;
ALTER TABLE citas ADD COLUMN IF NOT EXISTS tipo_cita VARCHAR(20);
ALTER TABLE citas ADD COLUMN IF NOT EXISTS numero_autorizacion_eps VARCHAR(60);
ALTER TABLE citas ADD COLUMN IF NOT EXISTS duracion_estimada_min INT;
ALTER TABLE citas ADD COLUMN IF NOT EXISTS recordatorio_24h_enviado_at TIMESTAMPTZ;
ALTER TABLE citas ADD COLUMN IF NOT EXISTS recordatorio_1h_enviado_at TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS consultas (
    id BIGSERIAL PRIMARY KEY,
    paciente_id BIGINT NOT NULL REFERENCES pacientes(id),
    personal_id BIGINT REFERENCES personal(id),
    cita_id BIGINT REFERENCES citas(id),
    motivo_consulta TEXT,
    enfermedad_actual TEXT,
    antecedentes_personales TEXT,
    antecedentes_familiares TEXT,
    alergias TEXT,
    fecha_consulta TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Campos normativos Res. 1995/1999 y RIPS
    tipo_consulta VARCHAR(30),
    codigo_cie10 VARCHAR(20),
    codigo_cie10_secundario TEXT,
    dolor_eva VARCHAR(5),
    perimetro_abdominal VARCHAR(10),
    perimetro_cefalico VARCHAR(10),
    saturacion_o2 VARCHAR(10),
    presion_arterial VARCHAR(20),
    frecuencia_cardiaca VARCHAR(10),
    frecuencia_respiratoria VARCHAR(10),
    temperatura VARCHAR(10),
    peso VARCHAR(10),
    talla VARCHAR(10),
    imc VARCHAR(10),
    hallazgos_examen TEXT,
    diagnostico TEXT,
    plan_tratamiento TEXT,
    tratamiento_farmacologico TEXT,
    observaciones_clinicas TEXT,
    recomendaciones TEXT
);

-- Columnas normativas para consultas existentes
ALTER TABLE consultas ADD COLUMN IF NOT EXISTS tipo_consulta VARCHAR(30);
ALTER TABLE consultas ADD COLUMN IF NOT EXISTS codigo_cie10 VARCHAR(20);
ALTER TABLE consultas ADD COLUMN IF NOT EXISTS codigo_cie10_secundario TEXT;
ALTER TABLE consultas ADD COLUMN IF NOT EXISTS dolor_eva VARCHAR(5);
ALTER TABLE consultas ADD COLUMN IF NOT EXISTS perimetro_abdominal VARCHAR(10);
ALTER TABLE consultas ADD COLUMN IF NOT EXISTS perimetro_cefalico VARCHAR(10);
ALTER TABLE consultas ADD COLUMN IF NOT EXISTS saturacion_o2 VARCHAR(10);
ALTER TABLE consultas ADD COLUMN IF NOT EXISTS presion_arterial VARCHAR(20);
ALTER TABLE consultas ADD COLUMN IF NOT EXISTS frecuencia_cardiaca VARCHAR(10);
ALTER TABLE consultas ADD COLUMN IF NOT EXISTS frecuencia_respiratoria VARCHAR(10);
ALTER TABLE consultas ADD COLUMN IF NOT EXISTS temperatura VARCHAR(10);
ALTER TABLE consultas ADD COLUMN IF NOT EXISTS peso VARCHAR(10);
ALTER TABLE consultas ADD COLUMN IF NOT EXISTS talla VARCHAR(10);
ALTER TABLE consultas ADD COLUMN IF NOT EXISTS imc VARCHAR(10);
ALTER TABLE consultas ADD COLUMN IF NOT EXISTS hallazgos_examen TEXT;
ALTER TABLE consultas ADD COLUMN IF NOT EXISTS diagnostico TEXT;
ALTER TABLE consultas ADD COLUMN IF NOT EXISTS plan_tratamiento TEXT;
ALTER TABLE consultas ADD COLUMN IF NOT EXISTS tratamiento_farmacologico TEXT;
ALTER TABLE consultas ADD COLUMN IF NOT EXISTS observaciones_clinicas TEXT;
ALTER TABLE consultas ADD COLUMN IF NOT EXISTS recomendaciones TEXT;

CREATE TABLE IF NOT EXISTS ordenes_clinicas (
    id BIGSERIAL PRIMARY KEY,
    paciente_id BIGINT NOT NULL REFERENCES pacientes(id),
    consulta_id BIGINT NOT NULL REFERENCES consultas(id) ON DELETE CASCADE,
    tipo VARCHAR(50) NOT NULL,
    detalle TEXT,
    estado VARCHAR(30) DEFAULT 'PENDIENTE',
    resultado TEXT,
    fecha_resultado TIMESTAMPTZ,
    resultado_registrado_por_id BIGINT REFERENCES personal(id),
    valor_estimado NUMERIC(14,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS facturas (
    id BIGSERIAL PRIMARY KEY,
    numero_factura VARCHAR(50),
    paciente_id BIGINT NOT NULL REFERENCES pacientes(id),
    orden_id BIGINT REFERENCES ordenes_clinicas(id),
    valor_total NUMERIC(14,2) NOT NULL,
    estado VARCHAR(30) DEFAULT 'PENDIENTE',
    descripcion TEXT,
    fecha_factura TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Campos normativos Decreto 4747/2007 + RIPS Res. 3374/2000
    codigo_cups VARCHAR(20),
    descripcion_cups VARCHAR(500),
    tipo_servicio VARCHAR(40),
    responsable_pago VARCHAR(30),
    cuota_moderadora NUMERIC(14,2),
    numero_autorizacion_eps VARCHAR(60),
    consecutive_counter BIGINT
);

ALTER TABLE facturas ADD COLUMN IF NOT EXISTS codigo_cups VARCHAR(20);
ALTER TABLE facturas ADD COLUMN IF NOT EXISTS descripcion_cups VARCHAR(500);
ALTER TABLE facturas ADD COLUMN IF NOT EXISTS tipo_servicio VARCHAR(40);
ALTER TABLE facturas ADD COLUMN IF NOT EXISTS responsable_pago VARCHAR(30);
ALTER TABLE facturas ADD COLUMN IF NOT EXISTS cuota_moderadora NUMERIC(14,2);
ALTER TABLE facturas ADD COLUMN IF NOT EXISTS numero_autorizacion_eps VARCHAR(60);
ALTER TABLE facturas ADD COLUMN IF NOT EXISTS consecutive_counter BIGINT;

-- Secuencia para numeración automática de facturas
CREATE SEQUENCE IF NOT EXISTS factura_seq START 1 INCREMENT 1;

CREATE TABLE IF NOT EXISTS laboratorio_solicitudes (
    id BIGSERIAL PRIMARY KEY,
    paciente_id BIGINT NOT NULL REFERENCES pacientes(id),
    solicitante_id BIGINT REFERENCES personal(id),
    tipo_prueba VARCHAR(150) NOT NULL,
    estado VARCHAR(50) DEFAULT 'PENDIENTE',
    fecha_solicitud DATE NOT NULL,
    resultado TEXT,
    observaciones TEXT,
    fecha_resultado TIMESTAMP,
    bacteriologo_id BIGINT REFERENCES personal(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- EPS comunes Colombia (idempotente)
INSERT INTO eps (codigo, nombre, activo, created_at) VALUES
    ('EPS001', 'Sura', true, CURRENT_TIMESTAMP),
    ('EPS002', 'Nueva EPS', true, CURRENT_TIMESTAMP),
    ('EPS003', 'Sanitas', true, CURRENT_TIMESTAMP),
    ('EPS004', 'Compensar', true, CURRENT_TIMESTAMP),
    ('EPS005', 'Famisanar', true, CURRENT_TIMESTAMP)
ON CONFLICT (codigo) DO NOTHING;

CREATE TABLE IF NOT EXISTS urgencias (
    id BIGSERIAL PRIMARY KEY,
    paciente_id BIGINT NOT NULL REFERENCES pacientes(id),
    nivel_triage VARCHAR(50),
    estado VARCHAR(50) DEFAULT 'EN_ESPERA',
    fecha_hora_ingreso TIMESTAMP NOT NULL,
    observaciones TEXT,
    atencion_id BIGINT REFERENCES atenciones(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Campos normativos Res. 5596/2015 (Triage hospitalario)
    tipo_llegada VARCHAR(30),
    motivo_consulta TEXT,
    profesional_triage_id BIGINT REFERENCES personal(id),
    sv_presion_arterial VARCHAR(20),
    sv_frecuencia_cardiaca VARCHAR(10),
    sv_frecuencia_respiratoria VARCHAR(10),
    sv_temperatura VARCHAR(10),
    sv_saturacion_o2 VARCHAR(10),
    sv_peso VARCHAR(10),
    sv_dolor_eva VARCHAR(5),
    glasgow_ocular INT,
    glasgow_verbal INT,
    glasgow_motor INT
);

ALTER TABLE urgencias ADD COLUMN IF NOT EXISTS tipo_llegada VARCHAR(30);
ALTER TABLE urgencias ADD COLUMN IF NOT EXISTS motivo_consulta TEXT;
ALTER TABLE urgencias ADD COLUMN IF NOT EXISTS profesional_triage_id BIGINT REFERENCES personal(id);
ALTER TABLE urgencias ADD COLUMN IF NOT EXISTS sv_presion_arterial VARCHAR(20);
ALTER TABLE urgencias ADD COLUMN IF NOT EXISTS sv_frecuencia_cardiaca VARCHAR(10);
ALTER TABLE urgencias ADD COLUMN IF NOT EXISTS sv_frecuencia_respiratoria VARCHAR(10);
ALTER TABLE urgencias ADD COLUMN IF NOT EXISTS sv_temperatura VARCHAR(10);
ALTER TABLE urgencias ADD COLUMN IF NOT EXISTS sv_saturacion_o2 VARCHAR(10);
ALTER TABLE urgencias ADD COLUMN IF NOT EXISTS sv_peso VARCHAR(10);
ALTER TABLE urgencias ADD COLUMN IF NOT EXISTS sv_dolor_eva VARCHAR(5);
ALTER TABLE urgencias ADD COLUMN IF NOT EXISTS glasgow_ocular INT;
ALTER TABLE urgencias ADD COLUMN IF NOT EXISTS glasgow_verbal INT;
ALTER TABLE urgencias ADD COLUMN IF NOT EXISTS glasgow_motor INT;

CREATE TABLE IF NOT EXISTS acceso_auditoria (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    evento VARCHAR(40) NOT NULL,
    ip VARCHAR(45),
    detalle TEXT,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    token VARCHAR(120) NOT NULL UNIQUE,
    expira_en TIMESTAMP NOT NULL,
    usado BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS hospitalizaciones (
    id BIGSERIAL PRIMARY KEY,
    paciente_id BIGINT NOT NULL REFERENCES pacientes(id),
    servicio VARCHAR(120),
    cama VARCHAR(50),
    estado VARCHAR(50) DEFAULT 'INGRESADO',
    fecha_ingreso TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_egreso TIMESTAMP,
    evolucion_diaria TEXT,
    ordenes_medicas TEXT,
    epicrisis TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS farmacia_medicamentos (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(200) NOT NULL,
    lote VARCHAR(80),
    fecha_vencimiento DATE,
    cantidad INT NOT NULL DEFAULT 0,
    precio NUMERIC(14,2),
    stock_minimo INT NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ── Agenda de Turnos ─────────────────────────────────────────────────────
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
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ,
    CONSTRAINT uk_programacion_anio_mes UNIQUE (anio, mes)
);

CREATE TABLE IF NOT EXISTS turnos (
    id                  BIGSERIAL PRIMARY KEY,
    personal_id         BIGINT NOT NULL REFERENCES personal(id),
    programacion_mes_id BIGINT NOT NULL REFERENCES programacion_mes(id),
    servicio            VARCHAR(30) NOT NULL,
    tipo_turno          VARCHAR(20) NOT NULL,
    fecha_inicio        TIMESTAMP NOT NULL,
    fecha_fin           TIMESTAMP NOT NULL,
    duracion_horas      INTEGER NOT NULL,
    estado              VARCHAR(15) NOT NULL DEFAULT 'BORRADOR',
    es_festivo          BOOLEAN NOT NULL DEFAULT FALSE,
    notas               TEXT,
    modificado_por_id   BIGINT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_turno_personal         ON turnos(personal_id);
CREATE INDEX IF NOT EXISTS idx_turno_programacion_mes ON turnos(programacion_mes_id);
CREATE INDEX IF NOT EXISTS idx_turno_fecha_inicio     ON turnos(fecha_inicio);

-- ── Sync ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sync_deduplication (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(64) NOT NULL UNIQUE,
    entity_type VARCHAR(100),
    server_id BIGINT,
    success BOOLEAN NOT NULL DEFAULT true,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notificaciones (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    contenido TEXT NOT NULL,
    tipo VARCHAR(30) DEFAULT 'GENERAL',
    remitente_id BIGINT NOT NULL,
    remitente_nombre VARCHAR(200),
    fecha_envio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cita_id BIGINT
);

CREATE TABLE IF NOT EXISTS notificacion_destinatarios (
    id BIGSERIAL PRIMARY KEY,
    notificacion_id BIGINT NOT NULL REFERENCES notificaciones(id) ON DELETE CASCADE,
    usuario_id BIGINT NOT NULL,
    usuario_email VARCHAR(255),
    usuario_nombre VARCHAR(200),
    leido BOOLEAN NOT NULL DEFAULT false,
    fecha_lectura TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_nd_usuario_id      ON notificacion_destinatarios(usuario_id);
CREATE INDEX IF NOT EXISTS idx_nd_notificacion_id ON notificacion_destinatarios(notificacion_id);

CREATE TABLE IF NOT EXISTS notificacion_adjuntos (
    id BIGSERIAL PRIMARY KEY,
    notificacion_id BIGINT NOT NULL REFERENCES notificaciones(id) ON DELETE CASCADE,
    nombre_archivo VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    tamano BIGINT,
    datos BYTEA
);

-- Recordatorios de cita y portal paciente: columnas en tablas existentes
ALTER TABLE notificaciones ADD COLUMN IF NOT EXISTS cita_id BIGINT;
ALTER TABLE citas ADD COLUMN IF NOT EXISTS recordatorio_24h_enviado_at TIMESTAMPTZ;
ALTER TABLE citas ADD COLUMN IF NOT EXISTS recordatorio_1h_enviado_at TIMESTAMPTZ;
ALTER TABLE pacientes ADD COLUMN IF NOT EXISTS usuario_id BIGINT;

-- Dispositivos para notificaciones push (portal/móvil)
CREATE TABLE IF NOT EXISTS dispositivos_push (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT NOT NULL,
    token       VARCHAR(512) NOT NULL,
    plataforma  VARCHAR(20) NOT NULL DEFAULT 'WEB',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS farmacia_dispensaciones (
    id BIGSERIAL PRIMARY KEY,
    medicamento_id BIGINT NOT NULL REFERENCES farmacia_medicamentos(id),
    paciente_id BIGINT NOT NULL REFERENCES pacientes(id),
    cantidad INT NOT NULL,
    fecha_dispensacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    entregado_por VARCHAR(150),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ─── Interoperabilidad RDA — Resolución 1888 de 2025 ─────────────────────────
-- Trazabilidad de Bundles FHIR R4 enviados al Ministerio de Salud (IHCE)
CREATE TABLE IF NOT EXISTS rda_envios (
    id              BIGSERIAL PRIMARY KEY,
    tipo_rda        VARCHAR(30)  NOT NULL,
    estado_envio    VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    atencion_id     BIGINT       REFERENCES atenciones(id),
    bundle_json     TEXT,
    id_ministerio   VARCHAR(100),
    fhir_version    VARCHAR(10)  DEFAULT '4.0.1',
    fecha_generacion TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_envio      TIMESTAMP,
    fecha_confirmacion TIMESTAMP,
    error_mensaje    TEXT,
    reintentos       INT         DEFAULT 0,
    tenant_schema    VARCHAR(100) NOT NULL DEFAULT current_schema()
);

CREATE INDEX IF NOT EXISTS idx_rda_atencion ON rda_envios (atencion_id);
CREATE INDEX IF NOT EXISTS idx_rda_estado   ON rda_envios (estado_envio);

-- ─── Índices de rendimiento — FK más consultadas ─────────────────────────────

-- pacientes
CREATE INDEX IF NOT EXISTS idx_pacientes_eps        ON pacientes (eps_id);
CREATE INDEX IF NOT EXISTS idx_pacientes_documento   ON pacientes (documento);

-- historias_clinicas  (paciente_id ya tiene UNIQUE → índice implícito)

-- atenciones
CREATE INDEX IF NOT EXISTS idx_atenciones_historia    ON atenciones (historia_id);
CREATE INDEX IF NOT EXISTS idx_atenciones_profesional ON atenciones (profesional_id);
CREATE INDEX IF NOT EXISTS idx_atenciones_fecha       ON atenciones (fecha_atencion);

-- hijos de atenciones
CREATE INDEX IF NOT EXISTS idx_diagnosticos_atencion  ON diagnosticos          (atencion_id);
CREATE INDEX IF NOT EXISTS idx_formulas_atencion       ON formulas_medicas      (atencion_id);
CREATE INDEX IF NOT EXISTS idx_procedimientos_atencion ON procedimientos        (atencion_id);
CREATE INDEX IF NOT EXISTS idx_laboratorios_atencion   ON laboratorios_atencion (atencion_id);
CREATE INDEX IF NOT EXISTS idx_imagenes_atencion       ON imagenes_diagnosticas (atencion_id);
CREATE INDEX IF NOT EXISTS idx_evoluciones_atencion    ON evoluciones           (atencion_id);
CREATE INDEX IF NOT EXISTS idx_notas_enf_atencion      ON notas_enfermeria      (atencion_id);
CREATE INDEX IF NOT EXISTS idx_consentim_atencion      ON consentimientos        (atencion_id);

-- citas
CREATE INDEX IF NOT EXISTS idx_citas_paciente   ON citas (paciente_id);
CREATE INDEX IF NOT EXISTS idx_citas_personal   ON citas (personal_id);
CREATE INDEX IF NOT EXISTS idx_citas_fecha_hora ON citas (fecha_hora);

-- consultas
CREATE INDEX IF NOT EXISTS idx_consultas_paciente ON consultas (paciente_id);
CREATE INDEX IF NOT EXISTS idx_consultas_personal ON consultas (personal_id);
CREATE INDEX IF NOT EXISTS idx_consultas_cita     ON consultas (cita_id);

-- órdenes y facturas
CREATE INDEX IF NOT EXISTS idx_ordenes_paciente  ON ordenes_clinicas (paciente_id);
CREATE INDEX IF NOT EXISTS idx_ordenes_consulta  ON ordenes_clinicas (consulta_id);
CREATE INDEX IF NOT EXISTS idx_facturas_paciente ON facturas          (paciente_id);
CREATE INDEX IF NOT EXISTS idx_facturas_orden    ON facturas          (orden_id);

-- urgencias y hospitalizaciones
CREATE INDEX IF NOT EXISTS idx_urgencias_paciente       ON urgencias           (paciente_id);
CREATE INDEX IF NOT EXISTS idx_urgencias_atencion        ON urgencias           (atencion_id);
CREATE INDEX IF NOT EXISTS idx_hospitalizaciones_paciente ON hospitalizaciones  (paciente_id);

-- laboratorio
CREATE INDEX IF NOT EXISTS idx_lab_solicitudes_paciente    ON laboratorio_solicitudes (paciente_id);
CREATE INDEX IF NOT EXISTS idx_lab_solicitudes_solicitante ON laboratorio_solicitudes (solicitante_id);

-- dolores
CREATE INDEX IF NOT EXISTS idx_dolores_paciente  ON dolores (paciente_id);
CREATE INDEX IF NOT EXISTS idx_dolores_historia  ON dolores (historia_clinica_id);

-- farmacia
CREATE INDEX IF NOT EXISTS idx_dispensaciones_medicamento ON farmacia_dispensaciones (medicamento_id);
CREATE INDEX IF NOT EXISTS idx_dispensaciones_paciente    ON farmacia_dispensaciones (paciente_id);
CREATE INDEX IF NOT EXISTS idx_dispensaciones_orden ON farmacia_dispensaciones (orden_clinica_id);
-- Migraciones farmacia / órdenes clínicas
ALTER TABLE ordenes_clinicas ADD COLUMN IF NOT EXISTS estado_dispensacion_farmacia VARCHAR(30) DEFAULT 'PENDIENTE';
ALTER TABLE ordenes_clinicas ADD COLUMN IF NOT EXISTS cantidad_prescrita INT;
ALTER TABLE ordenes_clinicas ADD COLUMN IF NOT EXISTS unidad_medida VARCHAR(30);
ALTER TABLE ordenes_clinicas ADD COLUMN IF NOT EXISTS frecuencia VARCHAR(120);
ALTER TABLE ordenes_clinicas ADD COLUMN IF NOT EXISTS duracion_dias INT;
ALTER TABLE farmacia_dispensaciones ADD COLUMN IF NOT EXISTS orden_clinica_id BIGINT REFERENCES ordenes_clinicas(id);

-- ============================================================
-- MÓDULO FACTURACIÓN ELECTRÓNICA DIAN (Res. 000042 / UBL 2.1)
-- ============================================================

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
    ambiente                   VARCHAR(20)   NOT NULL DEFAULT 'HABILITACION', -- HABILITACION / PRODUCCION
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
);

ALTER TABLE facturas
    ADD COLUMN IF NOT EXISTS dian_cufe        VARCHAR(128),
    ADD COLUMN IF NOT EXISTS dian_qr_url      VARCHAR(512),
    ADD COLUMN IF NOT EXISTS dian_estado      VARCHAR(30),
    ADD COLUMN IF NOT EXISTS dian_mensaje     TEXT,
    ADD COLUMN IF NOT EXISTS dian_xml_path    VARCHAR(500),
    ADD COLUMN IF NOT EXISTS dian_pdf_path    VARCHAR(500),
    ADD COLUMN IF NOT EXISTS dian_fecha_envio TIMESTAMPTZ;

-- ============================================================
-- MÓDULO ODONTOLOGÍA
-- ============================================================

CREATE TABLE IF NOT EXISTS consultas_odontologicas (
    id                          BIGSERIAL PRIMARY KEY,
    paciente_id                 BIGINT NOT NULL REFERENCES pacientes(id),
    profesional_id              BIGINT NOT NULL REFERENCES personal(id),
    cita_id                     BIGINT REFERENCES citas(id),
    -- SOAP Subjetivo
    motivo_consulta             TEXT,
    enfermedad_actual           TEXT,
    antecedentes_odontologicos  TEXT,
    antecedentes_sistemicos     TEXT,
    medicamentos_actuales       TEXT,
    alergias                    VARCHAR(500),
    habitos_orales              TEXT,
    higiene_oral                VARCHAR(30),     -- BUENA / REGULAR / MALA
    -- SOAP Objetivo
    examen_extra_oral           TEXT,
    examen_intra_oral           TEXT,
    cpod_cariados               INTEGER,
    cpod_perdidos               INTEGER,
    cpod_obturados              INTEGER,
    condicion_periodontal       VARCHAR(30),     -- LEVE / MODERADA / SEVERA / SANA
    riesgo_caries               VARCHAR(20),     -- BAJO / MEDIO / ALTO
    -- SOAP Análisis/Plan
    diagnostico                 TEXT,
    plan_tratamiento            TEXT,
    -- Firma digital
    firma_profesional_url       TEXT,
    firma_canvas_data           TEXT,
    estado                      VARCHAR(30) NOT NULL DEFAULT 'EN_ATENCION',
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS odontograma_estado (
    id              BIGSERIAL PRIMARY KEY,
    paciente_id     BIGINT NOT NULL REFERENCES pacientes(id),
    profesional_id  BIGINT NOT NULL REFERENCES personal(id),
    consulta_id     BIGINT REFERENCES consultas_odontologicas(id),
    pieza_fdi       INTEGER NOT NULL,
    superficie      VARCHAR(20) NOT NULL,   -- MESIAL|DISTAL|VESTIBULAR|LINGUAL|OCLUSAL|GENERAL
    estado          VARCHAR(40) NOT NULL,   -- SANO|CARIES|OBTURACION|ENDODONCIA|CORONA|AUSENTE|...
    observacion     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS procedimientos_catalogo (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(30),
    nombre      VARCHAR(250) NOT NULL,
    descripcion TEXT,
    categoria   VARCHAR(100),
    precio_base NUMERIC(12,2),
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    origen      VARCHAR(20) NOT NULL DEFAULT 'PERSONALIZADO',  -- CUPS | PERSONALIZADO
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS planes_tratamiento (
    id              BIGSERIAL PRIMARY KEY,
    paciente_id     BIGINT NOT NULL REFERENCES pacientes(id),
    profesional_id  BIGINT NOT NULL REFERENCES personal(id),
    consulta_id     BIGINT REFERENCES consultas_odontologicas(id),
    nombre          VARCHAR(200) NOT NULL DEFAULT 'Plan de Tratamiento',
    fase            INTEGER NOT NULL DEFAULT 1,
    descripcion     TEXT,
    valor_total     NUMERIC(14,2) NOT NULL DEFAULT 0,
    descuento       NUMERIC(5,2) NOT NULL DEFAULT 0,
    valor_final     NUMERIC(14,2) NOT NULL DEFAULT 0,
    valor_abonado   NUMERIC(14,2) NOT NULL DEFAULT 0,
    tipo_pago       VARCHAR(20) NOT NULL DEFAULT 'PARTICULAR',  -- EPS | PARTICULAR | MIXTO
    estado          VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    fecha_inicio    DATE,
    fecha_fin       DATE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS plan_tratamiento_items (
    id                  BIGSERIAL PRIMARY KEY,
    plan_id             BIGINT NOT NULL REFERENCES planes_tratamiento(id) ON DELETE CASCADE,
    procedimiento_id    BIGINT NOT NULL REFERENCES procedimientos_catalogo(id),
    pieza_fdi           INTEGER,
    cantidad            INTEGER NOT NULL DEFAULT 1,
    precio_unitario     NUMERIC(12,2) NOT NULL,
    descuento           NUMERIC(5,2) NOT NULL DEFAULT 0,
    valor_total         NUMERIC(12,2) NOT NULL,
    estado              VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    observaciones       TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS imagenes_clinicas (
    id                  BIGSERIAL PRIMARY KEY,
    paciente_id         BIGINT NOT NULL REFERENCES pacientes(id),
    profesional_id      BIGINT NOT NULL REFERENCES personal(id),
    consulta_id         BIGINT REFERENCES consultas_odontologicas(id),
    pieza_fdi           INTEGER,
    tipo                VARCHAR(50) NOT NULL DEFAULT 'FOTO_CLINICA',
    nombre_archivo      VARCHAR(300),
    url                 TEXT,
    thumbnail_base64    TEXT,
    descripcion         TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS evoluciones_odontologicas (
    id                          BIGSERIAL PRIMARY KEY,
    paciente_id                 BIGINT NOT NULL REFERENCES pacientes(id),
    profesional_id              BIGINT NOT NULL REFERENCES personal(id),
    consulta_id                 BIGINT NOT NULL REFERENCES consultas_odontologicas(id),
    plan_id                     BIGINT REFERENCES planes_tratamiento(id),
    nota_evolucion              TEXT NOT NULL,
    control_post_tratamiento    TEXT,
    proxima_cita_recomendada    TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Índices Odontología
CREATE INDEX IF NOT EXISTS idx_consultas_odont_paciente   ON consultas_odontologicas (paciente_id);
CREATE INDEX IF NOT EXISTS idx_odontograma_paciente       ON odontograma_estado      (paciente_id, pieza_fdi);
CREATE INDEX IF NOT EXISTS idx_planes_paciente            ON planes_tratamiento      (paciente_id);
CREATE INDEX IF NOT EXISTS idx_plan_items_plan            ON plan_tratamiento_items  (plan_id);
CREATE INDEX IF NOT EXISTS idx_imagenes_paciente          ON imagenes_clinicas       (paciente_id);
CREATE INDEX IF NOT EXISTS idx_evoluciones_odont_paciente ON evoluciones_odontologicas (paciente_id);

-- Catálogo de procedimientos base (CUPS Colombia)
INSERT INTO procedimientos_catalogo (codigo, nombre, categoria, precio_base, origen) VALUES
  ('890301','Consulta de primera vez por odontología general','Consulta',45000,'CUPS'),
  ('890302','Consulta de control o seguimiento por odontología general','Consulta',35000,'CUPS'),
  ('890310','Consulta de primera vez por odontología especializada','Consulta',80000,'CUPS'),
  ('890311','Consulta de control por odontología especializada','Consulta',60000,'CUPS'),
  ('898001','Restauración en resina compuesta - 1 cara','Restauración',120000,'CUPS'),
  ('898002','Restauración en resina compuesta - 2 caras','Restauración',150000,'CUPS'),
  ('898003','Restauración en resina compuesta - 3 caras','Restauración',180000,'CUPS'),
  ('898010','Restauración en amalgama - 1 cara','Restauración',90000,'CUPS'),
  ('898011','Restauración en amalgama - 2 caras','Restauración',110000,'CUPS'),
  ('898020','Sellante de fosas y fisuras','Preventiva',60000,'CUPS'),
  ('898030','Aplicación de flúor','Preventiva',40000,'CUPS'),
  ('898040','Detartraje supragingival','Periodoncia',150000,'CUPS'),
  ('898041','Detartraje subgingival','Periodoncia',200000,'CUPS'),
  ('898042','Curetaje cerrado','Periodoncia',250000,'CUPS'),
  ('898050','Exodoncia simple de diente permanente','Cirugía',120000,'CUPS'),
  ('898051','Exodoncia simple de diente temporal','Cirugía',80000,'CUPS'),
  ('898052','Exodoncia de diente retenido','Cirugía',350000,'CUPS'),
  ('898060','Endodoncia unirradicular','Endodoncia',450000,'CUPS'),
  ('898061','Endodoncia birradicular','Endodoncia',550000,'CUPS'),
  ('898062','Endodoncia multirradicular','Endodoncia',650000,'CUPS'),
  ('898070','Corona de porcelana sobre metal','Prótesis',1200000,'CUPS'),
  ('898071','Corona de porcelana pura (zirconio)','Prótesis',1800000,'CUPS'),
  ('898072','Corona de resina temporal','Prótesis',350000,'CUPS'),
  ('898080','Blanqueamiento dental con lámpara','Estética',450000,'CUPS'),
  ('898081','Blanqueamiento dental casero','Estética',250000,'CUPS'),
  ('898090','Ortodoncia - instalación','Ortodoncia',2500000,'CUPS'),
  ('898091','Control de ortodoncia mensual','Ortodoncia',120000,'CUPS'),
  ('898100','Implante dental (colocación)','Implantología',3500000,'CUPS'),
  ('898101','Corona sobre implante','Implantología',1500000,'CUPS')
ON CONFLICT DO NOTHING;

-- ── Receta electrónica (QR verificable, anti-falsificación)
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
);

CREATE TABLE IF NOT EXISTS receta_medicamentos (
    id          BIGSERIAL PRIMARY KEY,
    receta_id   BIGINT NOT NULL REFERENCES recetas_electronicas(id) ON DELETE CASCADE,
    medicamento VARCHAR(200) NOT NULL,
    dosis       VARCHAR(100),
    frecuencia  VARCHAR(100),
    duracion    VARCHAR(100)
);

-- ── Tabla Consentimiento Informado (Ley 23/1981, Res. 3380/1981)
CREATE TABLE IF NOT EXISTS consentimientos_informados (
    id                  BIGSERIAL PRIMARY KEY,
    paciente_id         BIGINT NOT NULL REFERENCES pacientes(id),
    profesional_id      BIGINT NOT NULL REFERENCES personal(id),
    tipo                VARCHAR(30) NOT NULL,       -- GENERAL|QUIRURGICO|DIAGNOSTICO|ODONTOLOGICO|ANESTESIA
    estado              VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',  -- PENDIENTE|FIRMADO|RECHAZADO|REVOCADO
    procedimiento       VARCHAR(300),               -- Descripción del procedimiento
    fecha_solicitud     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_firma         TIMESTAMPTZ,
    observaciones       TEXT,
    firma_canvas_data   TEXT,                       -- Base64 del trazo de firma
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Campos normativos Personal (Res. 1449/2016 RETHUS, habilitación)
ALTER TABLE personal ADD COLUMN IF NOT EXISTS tarjeta_profesional VARCHAR(30);
ALTER TABLE personal ADD COLUMN IF NOT EXISTS especialidad_formal VARCHAR(150);
ALTER TABLE personal ADD COLUMN IF NOT EXISTS numero_rethus VARCHAR(30);

-- ── Multi-rol profesional (sincronizado con usuario_roles)
CREATE TABLE IF NOT EXISTS personal_roles (
    personal_id BIGINT NOT NULL REFERENCES personal(id) ON DELETE CASCADE,
    rol         VARCHAR(50) NOT NULL,
    PRIMARY KEY (personal_id, rol)
);

-- ── Nuevos campos normativos Personal ──────────────────────────────────────
-- Tipo de documento (Res. 3374/2000 RIPS — campos CT/US)
ALTER TABLE personal ADD COLUMN IF NOT EXISTS tipo_documento VARCHAR(10);
-- Datos demográficos (SISPRO, RIPS)
ALTER TABLE personal ADD COLUMN IF NOT EXISTS fecha_nacimiento DATE;
ALTER TABLE personal ADD COLUMN IF NOT EXISTS sexo VARCHAR(10);
-- Lugar de práctica (Res. 2003/2014 habilitación)
ALTER TABLE personal ADD COLUMN IF NOT EXISTS municipio VARCHAR(10);
ALTER TABLE personal ADD COLUMN IF NOT EXISTS departamento VARCHAR(10);
-- Vínculo laboral (Circular 047/2007 Min. Protección Social)
ALTER TABLE personal ADD COLUMN IF NOT EXISTS tipo_vinculacion VARCHAR(30);
ALTER TABLE personal ADD COLUMN IF NOT EXISTS fecha_ingreso DATE;
ALTER TABLE personal ADD COLUMN IF NOT EXISTS fecha_retiro DATE;

-- ── Campos normativos Odontología (Res. 1995/1999, IHO-S, CPOD/ceod, CIE-10, consentimiento)
ALTER TABLE consultas_odontologicas ADD COLUMN IF NOT EXISTS tipo_consulta VARCHAR(40);
ALTER TABLE consultas_odontologicas ADD COLUMN IF NOT EXISTS codigo_cie10 VARCHAR(10);
ALTER TABLE consultas_odontologicas ADD COLUMN IF NOT EXISTS descripcion_cie10 VARCHAR(200);
ALTER TABLE consultas_odontologicas ADD COLUMN IF NOT EXISTS consentimiento_firmado BOOLEAN;
ALTER TABLE consultas_odontologicas ADD COLUMN IF NOT EXISTS fecha_consentimiento DATE;
ALTER TABLE consultas_odontologicas ADD COLUMN IF NOT EXISTS consentimiento_observaciones TEXT;
ALTER TABLE consultas_odontologicas ADD COLUMN IF NOT EXISTS ceod_cariados INTEGER;
ALTER TABLE consultas_odontologicas ADD COLUMN IF NOT EXISTS ceod_extraidos INTEGER;
ALTER TABLE consultas_odontologicas ADD COLUMN IF NOT EXISTS ceod_obturados INTEGER;
ALTER TABLE consultas_odontologicas ADD COLUMN IF NOT EXISTS ihos_placa NUMERIC(4,2);
ALTER TABLE consultas_odontologicas ADD COLUMN IF NOT EXISTS ihos_calculo NUMERIC(4,2);

-- ============================================================
-- MÓDULO EQUIPOS BÁSICOS DE SALUD (EBS)
-- ============================================================

CREATE TABLE IF NOT EXISTS ebs_territories (
    id                  BIGSERIAL PRIMARY KEY,
    code                VARCHAR(50) NOT NULL UNIQUE,
    name                VARCHAR(200) NOT NULL,
    type                VARCHAR(50),
    parent_territory_id BIGINT REFERENCES ebs_territories(id),
    geometry            TEXT,
    assigned_team_id    BIGINT,
    igac_departamento_codigo VARCHAR(2),
    igac_municipio_codigo    VARCHAR(5),
    igac_vereda_codigo       VARCHAR(20),
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE ebs_territories ADD COLUMN IF NOT EXISTS igac_departamento_codigo VARCHAR(2);
ALTER TABLE ebs_territories ADD COLUMN IF NOT EXISTS igac_municipio_codigo VARCHAR(5);
ALTER TABLE ebs_territories ADD COLUMN IF NOT EXISTS igac_vereda_codigo VARCHAR(20);

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
);

CREATE TABLE IF NOT EXISTS ebs_family_groups (
    id                     BIGSERIAL PRIMARY KEY,
    household_id           BIGINT NOT NULL REFERENCES ebs_households(id),
    fhir_group_id          VARCHAR(64),
    main_contact_patient_id BIGINT REFERENCES pacientes(id),
    socioeconomic_level    VARCHAR(30),
    risk_notes             TEXT,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS ebs_brigades (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(200) NOT NULL,
    territory_id BIGINT NOT NULL REFERENCES ebs_territories(id),
    date_start   DATE NOT NULL,
    date_end     DATE NOT NULL,
    status       VARCHAR(30) NOT NULL DEFAULT 'PROGRAMADA',
    notes        TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS ebs_territory_team (
    territory_id BIGINT NOT NULL REFERENCES ebs_territories(id) ON DELETE CASCADE,
    personal_id   BIGINT NOT NULL REFERENCES personal(id) ON DELETE CASCADE,
    PRIMARY KEY (territory_id, personal_id)
);

CREATE TABLE IF NOT EXISTS ebs_brigade_team (
    brigade_id   BIGINT NOT NULL REFERENCES ebs_brigades(id) ON DELETE CASCADE,
    personal_id  BIGINT NOT NULL REFERENCES personal(id) ON DELETE CASCADE,
    PRIMARY KEY (brigade_id, personal_id)
);

CREATE TABLE IF NOT EXISTS ebs_alerts (
    id                   BIGSERIAL PRIMARY KEY,
    type                 VARCHAR(50) NOT NULL,
    vereda_codigo        VARCHAR(20),
    municipio_codigo     VARCHAR(5),
    departamento_codigo  VARCHAR(2),
    title                VARCHAR(300) NOT NULL,
    description          TEXT,
    alert_date           DATE NOT NULL,
    status               VARCHAR(30) NOT NULL DEFAULT 'ACTIVA',
    external_id          VARCHAR(64),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS ebs_home_visits (
    id              BIGSERIAL PRIMARY KEY,
    household_id    BIGINT NOT NULL REFERENCES ebs_households(id),
    family_group_id BIGINT REFERENCES ebs_family_groups(id),
    professional_id BIGINT REFERENCES personal(id),
    brigade_id      BIGINT REFERENCES ebs_brigades(id),
    visit_date      TIMESTAMPTZ NOT NULL,
    visit_type      VARCHAR(50),
    tipo_intervencion VARCHAR(80),
    vereda_codigo   VARCHAR(20),
    diagnostico_cie10 VARCHAR(20),
    plan_cuidado    TEXT,
    motivo          TEXT,
    notes           TEXT,
    fhir_encounter_id VARCHAR(64),
    status          VARCHAR(30) NOT NULL DEFAULT 'EN_PROCESO',
    offline_uuid    VARCHAR(64),
    sync_status     VARCHAR(20) NOT NULL DEFAULT 'SYNCED',
    sync_errors     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

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
);

CREATE INDEX IF NOT EXISTS idx_ebs_households_territory
    ON ebs_households(territory_id);

CREATE INDEX IF NOT EXISTS idx_ebs_households_state
    ON ebs_households(state);

CREATE INDEX IF NOT EXISTS idx_ebs_households_risk
    ON ebs_households(risk_level);

CREATE INDEX IF NOT EXISTS idx_ebs_home_visits_household
    ON ebs_home_visits(household_id, visit_date);

CREATE INDEX IF NOT EXISTS idx_ebs_risk_patient
    ON ebs_risk_assessments(patient_id);

CREATE INDEX IF NOT EXISTS idx_ebs_risk_level
    ON ebs_risk_assessments(risk_level);

-- Migraciones EBS: columnas nuevas en visitas y tablas brigadas/equipo/alertas
ALTER TABLE ebs_home_visits ADD COLUMN IF NOT EXISTS brigade_id BIGINT REFERENCES ebs_brigades(id);
ALTER TABLE ebs_home_visits ADD COLUMN IF NOT EXISTS tipo_intervencion VARCHAR(80);
ALTER TABLE ebs_home_visits ADD COLUMN IF NOT EXISTS vereda_codigo VARCHAR(20);
ALTER TABLE ebs_home_visits ADD COLUMN IF NOT EXISTS diagnostico_cie10 VARCHAR(20);
ALTER TABLE ebs_home_visits ADD COLUMN IF NOT EXISTS plan_cuidado TEXT;
