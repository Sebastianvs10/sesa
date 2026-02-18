-- Migración V5: Hospitalización y Farmacia
-- Autor: Ing. J Sebastian Vargas S

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

CREATE TABLE IF NOT EXISTS farmacia_dispensaciones (
    id BIGSERIAL PRIMARY KEY,
    medicamento_id BIGINT NOT NULL REFERENCES farmacia_medicamentos(id),
    paciente_id BIGINT NOT NULL REFERENCES pacientes(id),
    cantidad INT NOT NULL,
    fecha_dispensacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    entregado_por VARCHAR(150),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
