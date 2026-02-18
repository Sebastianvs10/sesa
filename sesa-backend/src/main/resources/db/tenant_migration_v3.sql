-- Migración V3: Flujo clínico completo y usuarios/roles
-- Ejecutar en cada schema de tenant existente
-- Autor: Ing. J Sebastian Vargas S

ALTER TABLE consultas ADD COLUMN IF NOT EXISTS cita_id BIGINT REFERENCES citas(id);

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
