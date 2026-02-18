-- Migración V7: Tabla dolores (registro de dolores del paciente)
-- Autor: Ing. J Sebastian Vargas S
-- Ejecutar con search_path = schema_tenant para cada esquema existente que no tenga la tabla.

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
