-- Ejecutar en el schema ese_dmm los cambios de urgencias (fecha_hora_inicio_atencion + signos_vitales_urgencia)
-- Uso: psql -h localhost -p 5432 -U postgres -d sesa_db -f run_ese_dmm_urgencias.sql

SET search_path TO ese_dmm;

-- Migración urgencias: columna para reporte de cumplimiento Res. 5596/2015
ALTER TABLE urgencias ADD COLUMN IF NOT EXISTS fecha_hora_inicio_atencion TIMESTAMP;

-- Tabla signos vitales seriados por urgencia
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

RESET search_path;
