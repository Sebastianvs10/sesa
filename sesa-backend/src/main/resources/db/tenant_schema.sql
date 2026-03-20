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

-- S6: Referencia (motivo, nivel, datos para PDF)
ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS referencia_motivo TEXT;
ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS referencia_nivel VARCHAR(50);
ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS referencia_diagnostico TEXT;
ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS referencia_tratamiento TEXT;
ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS referencia_recomendaciones TEXT;
ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS referencia_proxima_cita TEXT;

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

-- S5: Reconciliación de medicamentos y alergias por atención
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
);

CREATE TABLE IF NOT EXISTS notas_enfermeria (
    id BIGSERIAL PRIMARY KEY,
    atencion_id BIGINT NOT NULL REFERENCES atenciones(id) ON DELETE CASCADE,
    nota TEXT NOT NULL,
    fecha_nota TIMESTAMP NOT NULL,
    profesional_id BIGINT REFERENCES personal(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Plantillas SOAP por motivo de consulta (Res. 1995/1999 — contenido mínimo HC)
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
);

-- Seed plantillas por tipo de consulta (Res. 1995/412 — ver docs/PLANTILLAS-HC-COLOMBIA.md)
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
WHERE NOT EXISTS (SELECT 1 FROM plantillas_soap LIMIT 1);

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
-- S3: Confirmación/cancelación de cita por enlace
ALTER TABLE citas ADD COLUMN IF NOT EXISTS token_confirmacion VARCHAR(64) UNIQUE;
ALTER TABLE citas ADD COLUMN IF NOT EXISTS confirmado_at TIMESTAMPTZ;
ALTER TABLE citas ADD COLUMN IF NOT EXISTS cancelado_desde_enlace_at TIMESTAMPTZ;

-- S10: Cuestionario pre-consulta (ePRO)
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
);
CREATE INDEX IF NOT EXISTS idx_cuestionario_preconsulta_cita ON cuestionario_preconsulta (cita_id);
CREATE INDEX IF NOT EXISTS idx_cuestionario_preconsulta_paciente ON cuestionario_preconsulta (paciente_id);

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
    examen_fisico_estructurado TEXT,
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
ALTER TABLE consultas ADD COLUMN IF NOT EXISTS examen_fisico_estructurado TEXT;
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
);
CREATE INDEX IF NOT EXISTS idx_orden_clinica_items_orden ON orden_clinica_items (orden_id);

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

-- Detalle multiclínea de factura (cuenta médica con varios ítems/CUPS)
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
);
CREATE INDEX IF NOT EXISTS idx_factura_items_factura ON factura_items (factura_id);

-- Radicación de facturas ante EPS (seguimiento y plazo 22 d hábiles)
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
);
CREATE INDEX IF NOT EXISTS idx_radicaciones_factura ON radicaciones (factura_id);
CREATE INDEX IF NOT EXISTS idx_radicaciones_fecha ON radicaciones (fecha_radicacion);
CREATE INDEX IF NOT EXISTS idx_radicaciones_estado ON radicaciones (estado);

-- Catálogo CUPS nacional (Colombia) - procedimientos, consultas, lab, imagenología
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
);
CREATE INDEX IF NOT EXISTS idx_cups_catalogo_codigo ON cups_catalogo (codigo);
CREATE INDEX IF NOT EXISTS idx_cups_catalogo_tipo ON cups_catalogo (tipo_servicio);
CREATE INDEX IF NOT EXISTS idx_cups_catalogo_activo ON cups_catalogo (activo) WHERE activo = TRUE;

-- Seed CUPS Colombia (consultas, laboratorio, imagenología, procedimientos) - idempotente
INSERT INTO cups_catalogo (codigo, descripcion, capitulo, tipo_servicio, precio_sugerido) VALUES
  ('890201', 'Consulta médica general', 'Cap 15 - Consulta', 'CONSULTA', 55000),
  ('890202', 'Consulta médica especializada', 'Cap 15 - Consulta', 'CONSULTA', 85000),
  ('890203', 'Consulta de urgencias', 'Cap 15 - Consulta', 'CONSULTA', 95000),
  ('890204', 'Consulta prioritaria', 'Cap 15 - Consulta', 'CONSULTA', 65000),
  ('890301', 'Consulta de primera vez por odontología general', 'Cap 15 - Consulta', 'CONSULTA', 45000),
  ('890302', 'Consulta de control o seguimiento por odontología general', 'Cap 15 - Consulta', 'CONSULTA', 35000),
  ('890310', 'Consulta de primera vez por odontología especializada', 'Cap 15 - Consulta', 'CONSULTA', 80000),
  ('890311', 'Consulta de control por odontología especializada', 'Cap 15 - Consulta', 'CONSULTA', 60000),
  ('890401', 'Valoración por psicología', 'Cap 15 - Consulta', 'CONSULTA', 70000),
  ('890402', 'Valoración por terapia ocupacional', 'Cap 15 - Consulta', 'CONSULTA', 65000),
  ('890403', 'Valoración por fonoaudiología', 'Cap 15 - Consulta', 'CONSULTA', 65000),
  ('890404', 'Valoración por nutrición', 'Cap 15 - Consulta', 'CONSULTA', 55000),
  ('890501', 'Valoración por enfermería', 'Cap 15 - Consulta', 'CONSULTA', 35000),
  ('890601', 'Consulta domiciliaria', 'Cap 15 - Consulta', 'CONSULTA', 120000),
  ('903801', 'Hemograma completo', 'Cap 16 - Laboratorio', 'LABORATORIO', 25000),
  ('903802', 'Glicemia', 'Cap 16 - Laboratorio', 'LABORATORIO', 8000),
  ('903804', 'Perfil lipídico', 'Cap 16 - Laboratorio', 'LABORATORIO', 35000),
  ('903806', 'Creatinina', 'Cap 16 - Laboratorio', 'LABORATORIO', 12000),
  ('903808', 'TSH', 'Cap 16 - Laboratorio', 'LABORATORIO', 28000),
  ('903810', 'Parcial de orina', 'Cap 16 - Laboratorio', 'LABORATORIO', 10000),
  ('903812', 'Coprológico', 'Cap 16 - Laboratorio', 'LABORATORIO', 15000),
  ('903814', 'Coagulograma', 'Cap 16 - Laboratorio', 'LABORATORIO', 45000),
  ('903816', 'Bilirrubinas', 'Cap 16 - Laboratorio', 'LABORATORIO', 12000),
  ('903818', 'Transaminasas (TGO, TGP)', 'Cap 16 - Laboratorio', 'LABORATORIO', 18000),
  ('903820', 'Urea', 'Cap 16 - Laboratorio', 'LABORATORIO', 10000),
  ('903822', 'Ácido úrico', 'Cap 16 - Laboratorio', 'LABORATORIO', 10000),
  ('903824', 'Hemoglobina glicada (HbA1c)', 'Cap 16 - Laboratorio', 'LABORATORIO', 28000),
  ('903826', 'PCR (Proteína C reactiva)', 'Cap 16 - Laboratorio', 'LABORATORIO', 22000),
  ('903828', 'Ferritina', 'Cap 16 - Laboratorio', 'LABORATORIO', 35000),
  ('903830', 'Vitamina B12', 'Cap 16 - Laboratorio', 'LABORATORIO', 38000),
  ('903832', 'Ácido fólico', 'Cap 16 - Laboratorio', 'LABORATORIO', 25000),
  ('903834', 'Troponina', 'Cap 16 - Laboratorio', 'LABORATORIO', 45000),
  ('903836', 'Gasometría arterial', 'Cap 16 - Laboratorio', 'LABORATORIO', 35000),
  ('903838', 'Grupo sanguíneo y factor Rh', 'Cap 16 - Laboratorio', 'LABORATORIO', 18000),
  ('903840', 'Tiempo de protrombina (PT)', 'Cap 16 - Laboratorio', 'LABORATORIO', 15000),
  ('903842', 'Fibrinógeno', 'Cap 16 - Laboratorio', 'LABORATORIO', 22000),
  ('903844', 'Dímero D', 'Cap 16 - Laboratorio', 'LABORATORIO', 55000),
  ('903846', 'PSA (antígeno prostático específico)', 'Cap 16 - Laboratorio', 'LABORATORIO', 42000),
  ('903848', 'Beta HCG cuantitativa', 'Cap 16 - Laboratorio', 'LABORATORIO', 25000),
  ('903850', 'Prueba de embarazo en orina', 'Cap 16 - Laboratorio', 'LABORATORIO', 8000),
  ('903852', 'Coprocultivo', 'Cap 16 - Laboratorio', 'LABORATORIO', 35000),
  ('903854', 'Urocultivo', 'Cap 16 - Laboratorio', 'LABORATORIO', 28000),
  ('903856', 'Hemocultivo', 'Cap 16 - Laboratorio', 'LABORATORIO', 45000),
  ('903860', 'Panel tiroideo', 'Cap 16 - Laboratorio', 'LABORATORIO', 55000),
  ('903862', 'Perfil hepático', 'Cap 16 - Laboratorio', 'LABORATORIO', 45000),
  ('903864', 'Perfil renal', 'Cap 16 - Laboratorio', 'LABORATORIO', 35000),
  ('903868', 'Ionograma', 'Cap 16 - Laboratorio', 'LABORATORIO', 22000),
  ('903870', 'LDH', 'Cap 16 - Laboratorio', 'LABORATORIO', 18000),
  ('903872', 'CK total', 'Cap 16 - Laboratorio', 'LABORATORIO', 18000),
  ('903874', 'Amilasa', 'Cap 16 - Laboratorio', 'LABORATORIO', 15000),
  ('903876', 'Lipasa', 'Cap 16 - Laboratorio', 'LABORATORIO', 22000),
  ('903878', 'PCR ultrasensible', 'Cap 16 - Laboratorio', 'LABORATORIO', 35000),
  ('903880', 'Vitamina D', 'Cap 16 - Laboratorio', 'LABORATORIO', 55000),
  ('903882', 'TSH libre', 'Cap 16 - Laboratorio', 'LABORATORIO', 32000),
  ('903884', 'T3 y T4', 'Cap 16 - Laboratorio', 'LABORATORIO', 38000),
  ('903886', 'Insulina', 'Cap 16 - Laboratorio', 'LABORATORIO', 35000),
  ('903890', 'Cortisol', 'Cap 16 - Laboratorio', 'LABORATORIO', 32000),
  ('903892', 'Microalbuminuria', 'Cap 16 - Laboratorio', 'LABORATORIO', 28000),
  ('903896', 'Testosterona', 'Cap 16 - Laboratorio', 'LABORATORIO', 42000),
  ('903898', 'Estradiol', 'Cap 16 - Laboratorio', 'LABORATORIO', 42000),
  ('903900', 'FSH', 'Cap 16 - Laboratorio', 'LABORATORIO', 35000),
  ('903902', 'LH', 'Cap 16 - Laboratorio', 'LABORATORIO', 35000),
  ('903904', 'Prolactina', 'Cap 16 - Laboratorio', 'LABORATORIO', 38000),
  ('903916', 'Anticuerpos anti-VIH', 'Cap 16 - Laboratorio', 'LABORATORIO', 45000),
  ('903918', 'VDRL / RPR', 'Cap 16 - Laboratorio', 'LABORATORIO', 15000),
  ('903920', 'Hepatitis B superficie', 'Cap 16 - Laboratorio', 'LABORATORIO', 35000),
  ('903922', 'Hepatitis C', 'Cap 16 - Laboratorio', 'LABORATORIO', 45000),
  ('903924', 'PCR COVID-19', 'Cap 16 - Laboratorio', 'LABORATORIO', 85000),
  ('903926', 'Prueba antígeno COVID-19', 'Cap 16 - Laboratorio', 'LABORATORIO', 35000),
  ('881601', 'Radiografía de tórax PA', 'Cap 17 - Imagen', 'IMAGENOLOGIA', 45000),
  ('881602', 'Radiografía de tórax PA y lateral', 'Cap 17 - Imagen', 'IMAGENOLOGIA', 55000),
  ('881604', 'Radiografía de columna', 'Cap 17 - Imagen', 'IMAGENOLOGIA', 55000),
  ('881606', 'Radiografía de abdomen', 'Cap 17 - Imagen', 'IMAGENOLOGIA', 45000),
  ('881608', 'Radiografía de cráneo', 'Cap 17 - Imagen', 'IMAGENOLOGIA', 50000),
  ('881610', 'Radiografía de extremidades', 'Cap 17 - Imagen', 'IMAGENOLOGIA', 40000),
  ('881612', 'Radiografía de pelvis', 'Cap 17 - Imagen', 'IMAGENOLOGIA', 45000),
  ('881614', 'Radiografía de cadera', 'Cap 17 - Imagen', 'IMAGENOLOGIA', 45000),
  ('881616', 'Radiografía de rodilla', 'Cap 17 - Imagen', 'IMAGENOLOGIA', 40000),
  ('881618', 'Radiografía de tobillo', 'Cap 17 - Imagen', 'IMAGENOLOGIA', 40000),
  ('881620', 'Radiografía de mano', 'Cap 17 - Imagen', 'IMAGENOLOGIA', 35000),
  ('881626', 'Radiografía lumbar', 'Cap 17 - Imagen', 'IMAGENOLOGIA', 50000),
  ('881628', 'Radiografía cervical', 'Cap 17 - Imagen', 'IMAGENOLOGIA', 50000),
  ('881801', 'Ecografía abdominal', 'Cap 17 - Imagen', 'IMAGENOLOGIA', 85000),
  ('881802', 'Ecografía pélvica', 'Cap 17 - Imagen', 'IMAGENOLOGIA', 75000),
  ('881804', 'Ecografía obstétrica', 'Cap 17 - Imagen', 'IMAGENOLOGIA', 95000),
  ('881806', 'Ecografía de tiroides', 'Cap 17 - Imagen', 'IMAGENOLOGIA', 65000),
  ('881808', 'Ecografía de partes blandas', 'Cap 17 - Imagen', 'IMAGENOLOGIA', 55000),
  ('881810', 'Ecografía doppler vascular', 'Cap 17 - Imagen', 'IMAGENOLOGIA', 120000),
  ('881812', 'Ecografía renal', 'Cap 17 - Imagen', 'IMAGENOLOGIA', 75000),
  ('881814', 'Ecografía de mama', 'Cap 17 - Imagen', 'IMAGENOLOGIA', 85000),
  ('882001', 'Tomografía axial computarizada (TAC) cráneo', 'Cap 17 - Imagen', 'IMAGENOLOGIA', 180000),
  ('882002', 'TAC tórax', 'Cap 17 - Imagen', 'IMAGENOLOGIA', 220000),
  ('882004', 'TAC abdomen', 'Cap 17 - Imagen', 'IMAGENOLOGIA', 220000),
  ('882006', 'TAC columna', 'Cap 17 - Imagen', 'IMAGENOLOGIA', 200000),
  ('882101', 'Resonancia magnética cráneo', 'Cap 17 - Imagen', 'IMAGENOLOGIA', 350000),
  ('882102', 'Resonancia magnética columna', 'Cap 17 - Imagen', 'IMAGENOLOGIA', 380000),
  ('882104', 'Resonancia magnética articulaciones', 'Cap 17 - Imagen', 'IMAGENOLOGIA', 320000),
  ('860001', 'Electrocardiograma', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 35000),
  ('860002', 'Electrocardiograma con informe', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 45000),
  ('860101', 'Espirometría', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 65000),
  ('860102', 'Espirometría con broncodilatador', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 85000),
  ('860201', 'Holter 24 horas', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 120000),
  ('860202', 'MAPA 24 horas', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 95000),
  ('860301', 'Oximetría de pulso', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 15000),
  ('860401', 'Polisomnografía', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 450000),
  ('860501', 'Prueba de esfuerzo', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 95000),
  ('870101', 'Curaciones', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 25000),
  ('870102', 'Sutura simple', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 55000),
  ('870104', 'Retiro de puntos', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 20000),
  ('870201', 'Lavado ótico', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 35000),
  ('870202', 'Retiro de tapón de cerumen', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 40000),
  ('870301', 'Nebulización', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 25000),
  ('870302', 'Oxigenoterapia', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 35000),
  ('870401', 'Inyección intramuscular', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 15000),
  ('870402', 'Inyección intravenosa', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 18000),
  ('870403', 'Inyección subcutánea', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 15000),
  ('870501', 'Cateterismo vesical', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 45000),
  ('870502', 'Sondaje vesical', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 40000),
  ('870601', 'Lavado gástrico', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 65000),
  ('870701', 'Punción lumbar', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 95000),
  ('870702', 'Paracentesis', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 85000),
  ('870703', 'Toracentesis', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 95000),
  ('870704', 'Artrocentesis', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 75000),
  ('870801', 'Biopsia de piel', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 85000),
  ('870802', 'Biopsia por aspiración con aguja fina', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 95000),
  ('870903', 'Audiometría', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 55000),
  ('870904', 'Oftalmoscopia', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 35000),
  ('870905', 'Otoscopia', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 25000),
  ('870906', 'Rinoscopia', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 25000),
  ('870907', 'Laringoscopia indirecta', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 45000),
  ('870908', 'Fundoscopia', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 45000),
  ('870909', 'Tonometría', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 35000),
  ('870910', 'Campimetría', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 55000),
  ('870911', 'Prueba de Schirmer', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 25000),
  ('870912', 'Refracción', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 45000),
  ('870913', 'Biomicroscopía', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 45000),
  ('870914', 'Gonioscopia', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 55000),
  ('870915', 'Retinografía', 'Cap 18 - Procedimientos', 'PROCEDIMIENTO', 85000),
  ('898001', 'Restauración en resina compuesta - 1 cara', 'Cap 15 - Odontología', 'PROCEDIMIENTO', 120000),
  ('898002', 'Restauración en resina compuesta - 2 caras', 'Cap 15 - Odontología', 'PROCEDIMIENTO', 150000),
  ('898003', 'Restauración en resina compuesta - 3 caras', 'Cap 15 - Odontología', 'PROCEDIMIENTO', 180000),
  ('898010', 'Restauración en amalgama - 1 cara', 'Cap 15 - Odontología', 'PROCEDIMIENTO', 90000),
  ('898011', 'Restauración en amalgama - 2 caras', 'Cap 15 - Odontología', 'PROCEDIMIENTO', 110000),
  ('898020', 'Sellante de fosas y fisuras', 'Cap 15 - Odontología', 'PROCEDIMIENTO', 60000),
  ('898030', 'Aplicación de flúor', 'Cap 15 - Odontología', 'PROCEDIMIENTO', 40000),
  ('898040', 'Detartraje supragingival', 'Cap 15 - Odontología', 'PROCEDIMIENTO', 150000),
  ('898041', 'Detartraje subgingival', 'Cap 15 - Odontología', 'PROCEDIMIENTO', 200000),
  ('898042', 'Curetaje cerrado', 'Cap 15 - Odontología', 'PROCEDIMIENTO', 250000),
  ('898050', 'Exodoncia simple de diente permanente', 'Cap 15 - Odontología', 'PROCEDIMIENTO', 120000),
  ('898051', 'Exodoncia simple de diente temporal', 'Cap 15 - Odontología', 'PROCEDIMIENTO', 80000),
  ('898052', 'Exodoncia de diente retenido', 'Cap 15 - Odontología', 'PROCEDIMIENTO', 350000),
  ('898060', 'Endodoncia unirradicular', 'Cap 15 - Odontología', 'PROCEDIMIENTO', 450000),
  ('898061', 'Endodoncia birradicular', 'Cap 15 - Odontología', 'PROCEDIMIENTO', 550000),
  ('898062', 'Endodoncia multirradicular', 'Cap 15 - Odontología', 'PROCEDIMIENTO', 650000),
  ('898070', 'Corona de porcelana sobre metal', 'Cap 15 - Odontología', 'PROCEDIMIENTO', 1200000),
  ('898071', 'Corona de porcelana pura (zirconio)', 'Cap 15 - Odontología', 'PROCEDIMIENTO', 1800000),
  ('898072', 'Corona de resina temporal', 'Cap 15 - Odontología', 'PROCEDIMIENTO', 350000),
  ('898080', 'Blanqueamiento dental con lámpara', 'Cap 15 - Odontología', 'PROCEDIMIENTO', 450000),
  ('898081', 'Blanqueamiento dental casero', 'Cap 15 - Odontología', 'PROCEDIMIENTO', 250000),
  ('898090', 'Ortodoncia - instalación', 'Cap 15 - Odontología', 'PROCEDIMIENTO', 2500000),
  ('898091', 'Control de ortodoncia mensual', 'Cap 15 - Odontología', 'PROCEDIMIENTO', 120000),
  ('898100', 'Implante dental (colocación)', 'Cap 15 - Odontología', 'PROCEDIMIENTO', 3500000),
  ('898101', 'Corona sobre implante', 'Cap 15 - Odontología', 'PROCEDIMIENTO', 1500000)
ON CONFLICT (codigo) DO NOTHING;

-- S9: Glosas (rechazos de factura) y adjuntos
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
);
CREATE INDEX IF NOT EXISTS idx_glosas_factura ON glosas (factura_id);
CREATE INDEX IF NOT EXISTS idx_glosas_estado ON glosas (estado);
CREATE INDEX IF NOT EXISTS idx_glosas_fecha_registro ON glosas (fecha_registro);

CREATE TABLE IF NOT EXISTS glosa_adjuntos (
    id BIGSERIAL PRIMARY KEY,
    glosa_id BIGINT NOT NULL REFERENCES glosas(id) ON DELETE CASCADE,
    nombre_archivo VARCHAR(255) NOT NULL,
    tipo VARCHAR(50),
    url_o_blob TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_glosa_adjuntos_glosa ON glosa_adjuntos (glosa_id);

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
ALTER TABLE urgencias ADD COLUMN IF NOT EXISTS fecha_hora_inicio_atencion TIMESTAMP;

-- S6: Datos de alta para PDF al paciente
ALTER TABLE urgencias ADD COLUMN IF NOT EXISTS alta_diagnostico TEXT;
ALTER TABLE urgencias ADD COLUMN IF NOT EXISTS alta_tratamiento TEXT;
ALTER TABLE urgencias ADD COLUMN IF NOT EXISTS alta_recomendaciones TEXT;
ALTER TABLE urgencias ADD COLUMN IF NOT EXISTS alta_proxima_cita TEXT;

-- Signos vitales seriados por urgencia (sugerencia 5)
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
    codigo_barras VARCHAR(64),
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
    fecha_lectura TIMESTAMP,
    archivado BOOLEAN NOT NULL DEFAULT false,
    fecha_archivado TIMESTAMP,
    eliminado BOOLEAN NOT NULL DEFAULT false,
    fecha_eliminado TIMESTAMP
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
ALTER TABLE notificacion_destinatarios ADD COLUMN IF NOT EXISTS archivado BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE notificacion_destinatarios ADD COLUMN IF NOT EXISTS fecha_archivado TIMESTAMPTZ;
ALTER TABLE notificacion_destinatarios ADD COLUMN IF NOT EXISTS eliminado BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE notificacion_destinatarios ADD COLUMN IF NOT EXISTS fecha_eliminado TIMESTAMPTZ;
ALTER TABLE citas ADD COLUMN IF NOT EXISTS recordatorio_24h_enviado_at TIMESTAMPTZ;
ALTER TABLE citas ADD COLUMN IF NOT EXISTS recordatorio_1h_enviado_at TIMESTAMPTZ;
ALTER TABLE citas ADD COLUMN IF NOT EXISTS token_confirmacion VARCHAR(64) UNIQUE;
ALTER TABLE citas ADD COLUMN IF NOT EXISTS confirmado_at TIMESTAMPTZ;
ALTER TABLE citas ADD COLUMN IF NOT EXISTS cancelado_desde_enlace_at TIMESTAMPTZ;
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
    urgencia_registro_id BIGINT  REFERENCES urgencias(id),
    hospitalizacion_id   BIGINT  REFERENCES hospitalizaciones(id),
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
CREATE INDEX IF NOT EXISTS idx_rda_urgencia ON rda_envios (urgencia_registro_id);
CREATE INDEX IF NOT EXISTS idx_rda_hospitalizacion ON rda_envios (hospitalizacion_id);
CREATE INDEX IF NOT EXISTS idx_rda_estado   ON rda_envios (estado_envio);

-- S11: RDA urgencias/hospitalización — columnas opcionales en schemas existentes
ALTER TABLE rda_envios ADD COLUMN IF NOT EXISTS urgencia_registro_id BIGINT REFERENCES urgencias(id);
ALTER TABLE rda_envios ADD COLUMN IF NOT EXISTS hospitalizacion_id BIGINT REFERENCES hospitalizaciones(id);

-- ─── S12: API Keys para integradores (laboratorio, PACS, signos vitales) ─────
CREATE TABLE IF NOT EXISTS api_keys (
    id                BIGSERIAL PRIMARY KEY,
    nombre_integrador VARCHAR(150) NOT NULL,
    api_key_hash      VARCHAR(255) NOT NULL,
    api_key_index     VARCHAR(64)  NOT NULL,
    permisos          VARCHAR(200) NOT NULL DEFAULT 'LABORATORIO',
    activo            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_api_keys_index ON api_keys (api_key_index);

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
-- Migraciones farmacia / órdenes clínicas (columna orden_clinica_id antes del índice)
ALTER TABLE ordenes_clinicas ADD COLUMN IF NOT EXISTS estado_dispensacion_farmacia VARCHAR(30) DEFAULT 'PENDIENTE';
ALTER TABLE ordenes_clinicas ADD COLUMN IF NOT EXISTS cantidad_prescrita INT;
ALTER TABLE ordenes_clinicas ADD COLUMN IF NOT EXISTS unidad_medida VARCHAR(30);
ALTER TABLE ordenes_clinicas ADD COLUMN IF NOT EXISTS frecuencia VARCHAR(120);
ALTER TABLE ordenes_clinicas ADD COLUMN IF NOT EXISTS duracion_dias INT;
ALTER TABLE farmacia_dispensaciones ADD COLUMN IF NOT EXISTS orden_clinica_id BIGINT REFERENCES ordenes_clinicas(id);
CREATE INDEX IF NOT EXISTS idx_dispensaciones_orden ON farmacia_dispensaciones (orden_clinica_id);

-- S2: Resultados críticos — flag en orden y trazabilidad de lectura
ALTER TABLE ordenes_clinicas ADD COLUMN IF NOT EXISTS resultado_critico BOOLEAN DEFAULT FALSE;
CREATE TABLE IF NOT EXISTS resultado_critico_lectura (
    id BIGSERIAL PRIMARY KEY,
    orden_clinica_id BIGINT NOT NULL REFERENCES ordenes_clinicas(id) ON DELETE CASCADE,
    personal_id BIGINT NOT NULL REFERENCES personal(id) ON DELETE CASCADE,
    leido_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_resultado_critico_lectura_orden_personal UNIQUE (orden_clinica_id, personal_id)
);
CREATE INDEX IF NOT EXISTS idx_resultado_critico_lectura_orden ON resultado_critico_lectura (orden_clinica_id);

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

-- S15: Guías de práctica clínica (GPC) por CIE-10
CREATE TABLE IF NOT EXISTS guia_gpc (
    id BIGSERIAL PRIMARY KEY,
    codigo_cie10 VARCHAR(20) NOT NULL,
    titulo VARCHAR(300) NOT NULL,
    criterios_control TEXT,
    medicamentos_primera_linea TEXT,
    estudios_seguimiento TEXT,
    fuente VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_guia_gpc_codigo ON guia_gpc(codigo_cie10);

CREATE TABLE IF NOT EXISTS gpc_sugerencia_mostrada (
    id BIGSERIAL PRIMARY KEY,
    atencion_id BIGINT NOT NULL REFERENCES atenciones(id) ON DELETE CASCADE,
    codigo_cie10 VARCHAR(20) NOT NULL,
    guia_id BIGINT NOT NULL REFERENCES guia_gpc(id) ON DELETE CASCADE,
    mostrado_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    profesional_id BIGINT REFERENCES personal(id)
);
CREATE INDEX IF NOT EXISTS idx_gpc_sugerencia_atencion ON gpc_sugerencia_mostrada(atencion_id);
CREATE INDEX IF NOT EXISTS idx_gpc_sugerencia_guia ON gpc_sugerencia_mostrada(guia_id);

-- S15: Dato inicial de ejemplo (una guía por CIE-10 E11)
INSERT INTO guia_gpc (codigo_cie10, titulo, criterios_control, medicamentos_primera_linea, estudios_seguimiento, fuente)
SELECT 'E11', 'Diabetes mellitus tipo 2', 'Control de glicemia, HbA1c, presión arterial, peso y pie diabético.', 'Metformina como primera línea; considerar iDPP-4 o iSGLT2 según perfil.', 'HbA1c cada 3-6 meses; creatinina y perfil lipídico anual; fondo de ojo según criterio.', 'Guía de práctica clínica - MinSalud'
WHERE NOT EXISTS (SELECT 1 FROM guia_gpc LIMIT 1);
