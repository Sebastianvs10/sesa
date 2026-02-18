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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS personal (
    id BIGSERIAL PRIMARY KEY,
    nombres VARCHAR(150) NOT NULL,
    apellidos VARCHAR(150),
    cargo VARCHAR(100) NOT NULL,
    servicio VARCHAR(100),
    turno VARCHAR(50),
    identificacion VARCHAR(50),
    primer_nombre VARCHAR(80),
    segundo_nombre VARCHAR(80),
    primer_apellido VARCHAR(80),
    segundo_apellido VARCHAR(80),
    celular VARCHAR(30),
    email VARCHAR(255),
    rol VARCHAR(50),
    institucion_prestadora VARCHAR(255),
    foto_url VARCHAR(500),
    foto_data BYTEA,
    foto_content_type VARCHAR(100),
    firma_url VARCHAR(500),
    firma_data BYTEA,
    firma_content_type VARCHAR(100),
    usuario_id BIGINT REFERENCES usuarios(id),
    activo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ordenes_clinicas (
    id BIGSERIAL PRIMARY KEY,
    paciente_id BIGINT NOT NULL REFERENCES pacientes(id),
    consulta_id BIGINT NOT NULL REFERENCES consultas(id) ON DELETE CASCADE,
    tipo VARCHAR(50) NOT NULL,
    detalle TEXT,
    estado VARCHAR(30) DEFAULT 'PENDIENTE',
    valor_estimado NUMERIC(14,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS facturas (
    id BIGSERIAL PRIMARY KEY,
    paciente_id BIGINT NOT NULL REFERENCES pacientes(id),
    orden_id BIGINT REFERENCES ordenes_clinicas(id),
    valor_total NUMERIC(14,2) NOT NULL,
    estado VARCHAR(30) DEFAULT 'PENDIENTE',
    descripcion TEXT,
    fecha_factura TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS laboratorio_solicitudes (
    id BIGSERIAL PRIMARY KEY,
    paciente_id BIGINT NOT NULL REFERENCES pacientes(id),
    solicitante_id BIGINT REFERENCES personal(id),
    tipo_prueba VARCHAR(150) NOT NULL,
    estado VARCHAR(50) DEFAULT 'PENDIENTE',
    fecha_solicitud DATE NOT NULL,
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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
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

CREATE TABLE IF NOT EXISTS notificacion_adjuntos (
    id BIGSERIAL PRIMARY KEY,
    notificacion_id BIGINT NOT NULL REFERENCES notificaciones(id) ON DELETE CASCADE,
    nombre_archivo VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    tamano BIGINT,
    datos BYTEA
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
