-- Migración: Agregar columna celular a tabla personal
-- Ejecutar en cada schema de tenant

-- Para schema miempresa4 (ejemplo):
ALTER TABLE miempresa4.personal ADD COLUMN IF NOT EXISTS celular VARCHAR(30);

-- Si tienes otros schemas, ejecuta lo mismo para cada uno:
-- ALTER TABLE [schema_name].personal ADD COLUMN IF NOT EXISTS celular VARCHAR(30);
