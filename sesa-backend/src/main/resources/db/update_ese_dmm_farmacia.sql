-- Actualización del esquema ese_dmm: columnas de farmacia/orígenes y datos de medicamentos.
-- Ejecutar con: psql -v schema=ese_dmm -f update_ese_dmm_farmacia.sql
-- O desde psql: SET search_path = 'ese_dmm'; \i update_ese_dmm_farmacia.sql
-- Autor: Ing. J Sebastian Vargas S

SET search_path = 'ese_dmm';

-- 1) Migraciones de columnas (farmacia / órdenes clínicas / prescripción medicamento)
ALTER TABLE ordenes_clinicas ADD COLUMN IF NOT EXISTS estado_dispensacion_farmacia VARCHAR(30) DEFAULT 'PENDIENTE';
ALTER TABLE ordenes_clinicas ADD COLUMN IF NOT EXISTS cantidad_prescrita INT;
ALTER TABLE ordenes_clinicas ADD COLUMN IF NOT EXISTS unidad_medida VARCHAR(30);
ALTER TABLE ordenes_clinicas ADD COLUMN IF NOT EXISTS frecuencia VARCHAR(120);
ALTER TABLE ordenes_clinicas ADD COLUMN IF NOT EXISTS duracion_dias INT;
ALTER TABLE farmacia_dispensaciones ADD COLUMN IF NOT EXISTS orden_clinica_id BIGINT REFERENCES ordenes_clinicas(id);
CREATE INDEX IF NOT EXISTS idx_dispensaciones_orden ON farmacia_dispensaciones (orden_clinica_id);

-- 2) Datos de medicamentos (ejecutar una sola vez; si ya hay datos, comentar este bloque)
INSERT INTO farmacia_medicamentos (nombre, lote, fecha_vencimiento, cantidad, precio, stock_minimo, activo) VALUES
  ('Losartán 50mg tabletas x30',           'LOT-2025-001', '2026-08-15', 150, 12500.00, 20, true),
  ('Amoxicilina 500mg cápsulas x21',       'LOT-2025-002', '2026-06-30',  80, 18900.00, 15, true),
  ('Ibuprofeno 400mg tabletas x30',        'LOT-2025-003', '2027-01-10', 200,  8500.00, 25, true),
  ('Metformina 850mg tabletas x30',        'LOT-2025-004', '2026-11-20', 120, 14200.00, 20, true),
  ('Sumatriptán 50mg tabletas x6',         'LOT-2025-005', '2026-09-01',  45, 35000.00, 10, true),
  ('Propranolol 40mg tabletas x30',        'LOT-2025-006', '2026-12-15',  90,  9800.00, 15, true),
  ('Salbutamol inhalador 100mcg 200 dosis','LOT-2025-007', '2026-07-25',  60, 22000.00, 10, true),
  ('Fluticasona nasal spray 120 dosis',    'LOT-2025-008', '2026-10-30',  55, 45000.00, 10, true),
  ('Omeprazol 20mg cápsulas x28',          'LOT-2025-009', '2027-03-15', 180, 11000.00, 25, true),
  ('Nitrofurantoína 100mg cápsulas x20',   'LOT-2025-010', '2026-05-20',  70, 16500.00, 15, true),
  ('Loratadina 10mg tabletas x30',         'LOT-2025-011', '2027-02-28', 160,  7500.00, 20, true),
  ('Levotiroxina 75mcg tabletas x30',      'LOT-2025-012', '2026-11-10', 100, 19500.00, 15, true),
  ('Paracetamol 500mg tabletas x30',      'LOT-2025-013', '2026-12-01', 250,  5200.00, 30, true),
  ('Enalapril 10mg tabletas x30',          'LOT-2025-014', '2026-10-15', 110, 11800.00, 15, true),
  ('Diclofenaco 50mg tabletas x30',        'LOT-2025-015', '2027-01-20', 140,  6800.00, 20, true);
