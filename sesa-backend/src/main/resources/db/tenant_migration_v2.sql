-- Migración V2: Añade columnas HC y Atención alineadas con el frontend
-- Ejecutar en cada schema de tenant existente
-- Autor: Ing. J Sebastian Vargas S

-- Historias clínicas: antecedentes adicionales y hábitos
ALTER TABLE historias_clinicas ADD COLUMN IF NOT EXISTS antecedentes_quirurgicos TEXT;
ALTER TABLE historias_clinicas ADD COLUMN IF NOT EXISTS antecedentes_farmacologicos TEXT;
ALTER TABLE historias_clinicas ADD COLUMN IF NOT EXISTS antecedentes_traumaticos TEXT;
ALTER TABLE historias_clinicas ADD COLUMN IF NOT EXISTS antecedentes_ginecoobstetricos TEXT;
ALTER TABLE historias_clinicas ADD COLUMN IF NOT EXISTS habitos_tabaco BOOLEAN DEFAULT false;
ALTER TABLE historias_clinicas ADD COLUMN IF NOT EXISTS habitos_alcohol BOOLEAN DEFAULT false;
ALTER TABLE historias_clinicas ADD COLUMN IF NOT EXISTS habitos_sustancias BOOLEAN DEFAULT false;
ALTER TABLE historias_clinicas ADD COLUMN IF NOT EXISTS habitos_detalles TEXT;

-- Atenciones: campos clínicos extendidos
ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS version_enfermedad TEXT;
ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS sintomas_asociados TEXT;
ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS factores_mejoran TEXT;
ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS factores_empeoran TEXT;
ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS revision_sistemas TEXT;
ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS presion_arterial VARCHAR(20);
ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS frecuencia_cardiaca VARCHAR(10);
ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS frecuencia_respiratoria VARCHAR(10);
ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS temperatura VARCHAR(10);
ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS peso VARCHAR(10);
ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS talla VARCHAR(10);
ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS imc VARCHAR(10);
ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS evaluacion_general TEXT;
ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS hallazgos TEXT;
ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS diagnostico TEXT;
ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS codigo_cie10 VARCHAR(20);
ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS tratamiento_farmacologico TEXT;
ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS ordenes_medicas TEXT;
ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS examenes_solicitados TEXT;
ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS incapacidad TEXT;
ALTER TABLE atenciones ADD COLUMN IF NOT EXISTS recomendaciones TEXT;
