-- =============================================================
-- SESA Salud — Datos ficticios de prueba
-- Ejecutar DESPUÉS de tenant_schema.sql y migraciones
-- SET search_path = <nombre_schema_tenant>;
-- Para cada tenant donde se quiera poblar
-- =============================================================

-- (Las EPS ya se insertan en tenant_schema.sql, pero por seguridad:)
INSERT INTO eps (codigo, nombre, activo) VALUES
  ('EPS001', 'Sura', true),
  ('EPS002', 'Nueva EPS', true),
  ('EPS003', 'Sanitas', true),
  ('EPS004', 'Compensar', true),
  ('EPS005', 'Famisanar', true)
ON CONFLICT (codigo) DO NOTHING;

-- ========================
-- Pacientes (20)
-- ========================
INSERT INTO pacientes (tipo_documento, documento, nombres, apellidos, fecha_nacimiento, sexo, grupo_sanguineo, telefono, email, direccion, eps_id, activo) VALUES
  ('CC', '1032456789', 'Carlos Andrés', 'García Muñoz', '1988-03-15', 'M', 'O+', '3101234567', 'carlos.garcia@mail.com', 'Cra 15 # 82-40, Bogotá', 1, true),
  ('CC', '1019876543', 'María Fernanda', 'López Rodríguez', '1992-07-22', 'F', 'A+', '3157896543', 'maria.lopez@mail.com', 'Cll 100 # 19-12, Bogotá', 2, true),
  ('CC', '80234561',   'Jorge Eduardo', 'Martínez Herrera', '1975-11-10', 'M', 'B+', '3204561234', 'jorge.martinez@mail.com', 'Av. Suba # 118-95, Bogotá', 3, true),
  ('CC', '52345678',   'Ana Lucía', 'Torres Vargas', '1990-01-28', 'F', 'O-', '3189012345', 'ana.torres@mail.com', 'Cll 53 # 14-20, Bogotá', 1, true),
  ('CC', '1098765432', 'Sebastián', 'Ramírez Peña', '1995-09-03', 'M', 'AB+', '3001122334', 'seba.ramirez@mail.com', 'Cra 7 # 45-60, Bogotá', 4, true),
  ('TI', '1234567890', 'Valentina', 'Castillo Moreno', '2009-05-17', 'F', 'A-', '3145678901', 'valentina.c@mail.com', 'Cll 72 # 10-30, Bogotá', 2, true),
  ('CC', '79345612',   'Ricardo Andrés', 'Sánchez Díaz', '1983-08-20', 'M', 'O+', '3112345678', 'ricardo.sanchez@mail.com', 'Cra 50 # 120-15, Bogotá', 5, true),
  ('CE', 'E12345678',  'Luisa Mariana', 'Mejía Ospina', '1997-12-05', 'F', 'B-', '3176543210', 'luisa.mejia@mail.com', 'Cll 85 # 20-45, Medellín', 3, true),
  ('CC', '10457896',   'Andrés Felipe', 'Gutiérrez Ruiz', '1980-04-14', 'M', 'A+', '3209876543', 'andres.gutierrez@mail.com', 'Cra 30 # 45-78, Cali', 1, true),
  ('CC', '52987654',   'Diana Marcela', 'Rojas Castro', '1987-06-30', 'F', 'O+', '3123456789', 'diana.rojas@mail.com', 'Av. 68 # 25-40, Bogotá', 4, true),
  ('CC', '1045678901', 'Santiago', 'Vargas Londoño', '2000-02-11', 'M', 'AB-', '3198765432', 'santiago.vargas@mail.com', 'Cll 26 # 69D-91, Bogotá', 2, true),
  ('CC', '39456789',   'Laura Camila', 'Hernández Rueda', '1993-10-08', 'F', 'B+', '3156789012', 'laura.hernandez@mail.com', 'Cra 11 # 93-55, Bogotá', 5, true),
  ('CC', '80567890',   'Pedro Alejandro', 'Cruz Montaña', '1978-12-25', 'M', 'O-', '3201234567', 'pedro.cruz@mail.com', 'Cll 170 # 9-30, Bogotá', 3, true),
  ('CC', '1023456712', 'Camila Andrea', 'Restrepo Gil', '1999-03-19', 'F', 'A+', '3147890123', 'camila.restrepo@mail.com', 'Cra 68 # 13-51, Bogotá', 1, true),
  ('CC', '79678901',   'Mauricio', 'Patiño Velasco', '1972-07-04', 'M', 'B-', '3110987654', 'mauricio.patino@mail.com', 'Cll 45 # 28-14, Medellín', 2, true),
  ('TI', '1012345679', 'Sofía', 'Morales Arango', '2011-11-22', 'F', 'O+', '3162345678', 'sofia.morales@mail.com', 'Cra 15 # 106-90, Bogotá', 4, true),
  ('CC', '1056789012', 'Daniel Esteban', 'Ríos Bermúdez', '1996-08-16', 'M', 'A-', '3193456789', 'daniel.rios@mail.com', 'Cll 80 # 50-22, Barranquilla', 5, true),
  ('CC', '52123456',   'Natalia', 'Pineda Gómez', '1985-05-09', 'F', 'AB+', '3134567890', 'natalia.pineda@mail.com', 'Av. Caracas # 34-60, Bogotá', 3, true),
  ('CC', '80890123',   'Héctor Fabio', 'Ospina Cardona', '1970-09-27', 'M', 'O+', '3205678901', 'hector.ospina@mail.com', 'Cra 25 # 55-30, Pereira', 1, true),
  ('CC', '1034567890', 'Isabella', 'Franco Quintero', '2003-01-05', 'F', 'B+', '3176789012', 'isabella.franco@mail.com', 'Cll 93B # 12-18, Bogotá', 2, true)
ON CONFLICT (documento) DO NOTHING;

-- ========================
-- Personal (roles RBAC: MEDICO, ODONTOLOGO, BACTERIOLOGO, ENFERMERO, JEFE_ENFERMERIA, etc.)
-- ========================
INSERT INTO personal (nombres, apellidos, cargo, servicio, turno, identificacion, primer_nombre, primer_apellido, celular, email, rol, institucion_prestadora, activo) VALUES
  ('Juan Carlos',   'Medina López',     'Médico General',     'Consulta Externa', 'Mañana', 'MP-2345', 'Juan Carlos', 'Medina', '3109998877', 'jc.medina@sesa.com',  'MEDICO', 'IPS SESA Salud', true),
  ('Patricia',      'Rojas Sánchez',    'Médica Especialista','Medicina Interna',  'Mañana', 'MP-4567', 'Patricia',     'Rojas',  '3118887766', 'p.rojas@sesa.com',    'MEDICO', 'IPS SESA Salud', true),
  ('Roberto',       'Díaz Bermúdez',    'Médico General',     'Urgencias',         'Noche',  'MP-6789', 'Roberto',      'Díaz',   '3127776655', 'r.diaz@sesa.com',     'MEDICO', 'IPS SESA Salud', true),
  ('María Elena',   'Ortiz Ramírez',    'Odontóloga',         'Odontología',       'Mañana', 'OD-1234', 'María Elena',  'Ortiz',  '3163332211', 'me.ortiz@sesa.com',   'ODONTOLOGO', 'IPS SESA Salud', true),
  ('Andrea',        'Gómez Morales',    'Enfermera Jefe',     'Hospitalización',   'Tarde',  'ENF-123', 'Andrea',       'Gómez',  '3136665544', 'a.gomez@sesa.com',    'JEFE_ENFERMERIA', 'IPS SESA Salud', true),
  ('Luis Fernando', 'Castaño Rivera',   'Bacteriólogo',       'Laboratorio',       'Mañana', 'LAB-456', 'Luis Fernando','Castaño','3145554433', 'l.castano@sesa.com',  'BACTERIOLOGO', 'IPS SESA Salud', true),
  ('Claudia Milena','Vargas Quintero',  'Regente de Farmacia','Farmacia',          'Mañana', 'FAR-789', 'Claudia',      'Vargas', '3154443322', 'c.vargas@sesa.com',   'REGENTE_FARMACIA', 'IPS SESA Salud', true),
  ('Sandra',        'Martínez Pérez',   'Enfermera',          'Urgencias',         'Mañana', 'ENF-456', 'Sandra',       'Martínez','3175554433', 's.martinez@sesa.com', 'ENFERMERO', 'IPS SESA Salud', true),
  ('Paola',         'Rodríguez López',  'Recepcionista',      'Atención al Público','Mañana', 'REC-001', 'Paola',        'Rodríguez','3186665544', 'p.rodriguez@sesa.com','RECEPCIONISTA', 'IPS SESA Salud', true),
  ('Carlos',        'Restrepo Gómez',   'Psicólogo',          'Psicología',        'Mañana', 'PSI-001', 'Carlos',       'Restrepo','3197776655', 'c.restrepo@sesa.com', 'PSICOLOGO', 'IPS SESA Salud', true),
  ('Laura',         'Hernández Torres', 'Auxiliar de Enfermería','Hospitalización','Tarde',  'AUX-001', 'Laura',        'Hernández','3208887766', 'l.hernandez@sesa.com','AUXILIAR_ENFERMERIA', 'IPS SESA Salud', true),
  ('Fernando',      'Álvarez Pérez',    'Administrador',      'Gerencia',          'Completo','ADM-001','Fernando',     'Álvarez','3219998877', 'f.alvarez@sesa.com',  'ADMIN', 'IPS SESA Salud', true)
ON CONFLICT DO NOTHING;

-- ========================
-- Historias Clínicas (10 pacientes)
-- ========================
INSERT INTO historias_clinicas (paciente_id, fecha_apertura, estado, grupo_sanguineo, alergias_generales, antecedentes_personales, antecedentes_familiares, habitos_tabaco, habitos_alcohol) VALUES
  (1, '2024-01-10 08:30:00', 'ACTIVA', 'O+', 'Penicilina', 'Hipertensión arterial controlada', 'Padre con diabetes tipo 2', false, false),
  (2, '2024-01-15 09:00:00', 'ACTIVA', 'A+', 'Ninguna conocida', 'Sin antecedentes relevantes', 'Madre con hipertensión', false, false),
  (3, '2024-02-01 10:15:00', 'ACTIVA', 'B+', 'Sulfonamidas', 'Diabetes tipo 2, Artrosis', 'Antecedentes de cáncer de colon (madre)', true, true),
  (4, '2024-02-20 07:45:00', 'ACTIVA', 'O-', 'Ninguna conocida', 'Migraña crónica', 'Sin antecedentes relevantes', false, false),
  (5, '2024-03-05 11:00:00', 'ACTIVA', 'AB+', 'Ibuprofeno', 'Asma leve', 'Padre con asma, tío con EPOC', false, false),
  (7, '2024-03-18 08:00:00', 'ACTIVA', 'O+', 'Ninguna conocida', 'Colesterol alto, Lumbalgia crónica', 'Madre con artritis reumatoide', false, true),
  (9, '2024-04-02 09:30:00', 'ACTIVA', 'A+', 'Mariscos', 'Gastritis, Reflujo gastroesofágico', 'Padre con infarto a los 60 años', true, false),
  (10, '2024-04-15 14:00:00', 'ACTIVA', 'O+', 'Ninguna conocida', 'Hipotiroidismo', 'Antecedentes de tiroides en familia', false, false),
  (12, '2024-05-10 08:30:00', 'ACTIVA', 'B+', 'Dipirona', 'Rinitis alérgica crónica', 'Sin antecedentes relevantes', false, false),
  (14, '2024-06-01 10:00:00', 'ACTIVA', 'A+', 'Ninguna conocida', 'Sana, sin antecedentes', 'Abuela materna con diabetes', false, false)
ON CONFLICT DO NOTHING;

-- ========================
-- Atenciones médicas (12)
-- ========================
INSERT INTO atenciones (historia_id, profesional_id, fecha_atencion, motivo_consulta, enfermedad_actual, presion_arterial, frecuencia_cardiaca, temperatura, peso, talla, imc, diagnostico, codigo_cie10, plan_tratamiento) VALUES
  (1, 1, '2024-06-10 08:30:00', 'Control de hipertensión',     'Paciente refiere cefalea leve ocasional, PA estable con tratamiento actual', '130/85', '72', '36.5', '78', '1.72', '26.4', 'Hipertensión esencial controlada', 'I10', 'Continuar losartán 50mg/día. Control en 3 meses.'),
  (2, 1, '2024-06-12 09:00:00', 'Dolor de garganta persistente','Odinofagia de 4 días, sin fiebre, amígdalas eritematosas', '110/70', '68', '36.8', '62', '1.65', '22.8', 'Faringitis aguda', 'J02.9', 'Amoxicilina 500mg c/8h x 7 días, ibuprofeno SOS.'),
  (3, 2, '2024-06-15 10:00:00', 'Control de diabetes',         'Glicemia en ayunas 145 mg/dL, HbA1c 7.8%, polidipsia leve', '140/90', '76', '36.4', '92', '1.68', '32.6', 'Diabetes mellitus tipo 2 mal controlada', 'E11.9', 'Ajustar metformina a 850mg c/12h. Dieta hipoglúcida. HbA1c en 3 meses.'),
  (4, 1, '2024-07-01 08:00:00', 'Cefalea intensa recurrente',  'Episodios de migraña 3-4 veces/mes, fotofobia, náuseas asociadas', '115/75', '64', '36.6', '58', '1.60', '22.7', 'Migraña sin aura', 'G43.0', 'Sumatriptán 50mg al inicio del episodio. Propranolol 40mg/día profiláctico.'),
  (5, 2, '2024-07-10 11:00:00', 'Crisis asmática leve',        'Disnea al esfuerzo, sibilancias espiratorias bilaterales, SpO2 94%', '120/80', '88', '36.7', '73', '1.75', '23.8', 'Asma persistente leve exacerbada', 'J45.1', 'Salbutamol inhalado SOS + fluticasona inhalada 250mcg c/12h.'),
  (1, 2, '2024-08-15 09:00:00', 'Control trimestral HTA',      'PA dentro de rango meta, sin síntomas cardiovasculares', '125/82', '70', '36.5', '77', '1.72', '26.0', 'Hipertensión esencial controlada', 'I10', 'Continuar esquema actual. Laboratorios de control.'),
  (7, 1, '2024-08-20 08:30:00', 'Lumbalgia crónica exacerbada','Dolor lumbar irradiado a miembro inferior derecho, VAS 7/10', '135/88', '74', '36.5', '85', '1.70', '29.4', 'Lumbalgia mecánica con radiculopatía', 'M54.5', 'Naproxeno 500mg c/12h x 10 días. Terapia física 10 sesiones. RMN lumbar.'),
  (9, 3, '2024-09-01 10:00:00', 'Epigastralgia y reflujo',     'Ardor epigástrico posprandial, regurgitación ácida, sin signos de alarma', '118/76', '66', '36.5', '80', '1.74', '26.4', 'Enfermedad por reflujo gastroesofágico', 'K21.0', 'Omeprazol 20mg antes del desayuno x 8 semanas. Medidas antirreflujo.'),
  (10, 1, '2024-09-15 14:30:00','Control de hipotiroidismo',   'TSH 5.8 (levemente elevada), T4 libre normal baja, astenia leve', '112/72', '62', '36.3', '66', '1.63', '24.8', 'Hipotiroidismo primario', 'E03.9', 'Ajustar levotiroxina de 50 a 75 mcg/día. TSH control en 6 semanas.'),
  (2, 2, '2024-10-05 09:30:00', 'Infección urinaria',          'Disuria, polaquiuria, orina turbia x 3 días, sin fiebre', '108/68', '70', '36.7', '63', '1.65', '23.1', 'Infección urinaria baja no complicada', 'N39.0', 'Nitrofurantoína 100mg c/6h x 5 días. Urocultivo de control.'),
  (12, 1, '2024-10-20 08:00:00','Rinitis alérgica exacerbada', 'Rinorrea hialina, estornudos en salva, prurito nasal y ocular', '110/70', '66', '36.5', '55', '1.58', '22.0', 'Rinitis alérgica persistente', 'J30.4', 'Loratadina 10mg/día. Fluticasona nasal 2 puffs c/12h.'),
  (14, 3, '2024-11-01 10:30:00','Consulta de rutina',          'Paciente joven, asintomática, acude por chequeo general preventivo', '105/65', '68', '36.5', '57', '1.62', '21.7', 'Paciente sana - examen periódico', 'Z00.0', 'Laboratorios de rutina: hemograma, glicemia, perfil lipídico, parcial de orina.')
ON CONFLICT DO NOTHING;

-- ========================
-- Diagnósticos asociados (15)
-- ========================
INSERT INTO diagnosticos (atencion_id, codigo_cie10, descripcion, tipo) VALUES
  (1, 'I10',   'Hipertensión esencial (primaria)', 'PRINCIPAL'),
  (2, 'J02.9', 'Faringitis aguda, no especificada', 'PRINCIPAL'),
  (3, 'E11.9', 'Diabetes mellitus tipo 2 sin complicaciones', 'PRINCIPAL'),
  (3, 'E78.5', 'Hiperlipidemia no especificada', 'SECUNDARIO'),
  (4, 'G43.0', 'Migraña sin aura', 'PRINCIPAL'),
  (5, 'J45.1', 'Asma no alérgica', 'PRINCIPAL'),
  (7, 'M54.5', 'Lumbago no especificado', 'PRINCIPAL'),
  (7, 'M54.1', 'Radiculopatía lumbar', 'SECUNDARIO'),
  (8, 'K21.0', 'Enfermedad por reflujo gastroesofágico con esofagitis', 'PRINCIPAL'),
  (9, 'E03.9', 'Hipotiroidismo no especificado', 'PRINCIPAL'),
  (10, 'N39.0', 'Infección de vías urinarias, sitio no especificado', 'PRINCIPAL'),
  (11, 'J30.4', 'Rinitis alérgica, no especificada', 'PRINCIPAL'),
  (12, 'Z00.0', 'Examen médico general', 'PRINCIPAL')
ON CONFLICT DO NOTHING;

-- ========================
-- Fórmulas médicas (10)
-- ========================
INSERT INTO formulas_medicas (atencion_id, medicamento, dosis, frecuencia, duracion) VALUES
  (1, 'Losartán 50mg',       '1 tableta', 'Cada 24 horas', 'Uso crónico'),
  (2, 'Amoxicilina 500mg',   '1 cápsula', 'Cada 8 horas',  '7 días'),
  (2, 'Ibuprofeno 400mg',    '1 tableta', 'Si dolor',       'SOS'),
  (3, 'Metformina 850mg',    '1 tableta', 'Cada 12 horas',  'Uso crónico'),
  (4, 'Sumatriptán 50mg',    '1 tableta', 'Al inicio de crisis', 'SOS'),
  (4, 'Propranolol 40mg',    '1 tableta', 'Cada 24 horas',  'Uso crónico'),
  (5, 'Salbutamol inhalado', '2 puffs',   'Si disnea',      'SOS'),
  (5, 'Fluticasona inhalada','2 puffs',   'Cada 12 horas',  'Uso crónico'),
  (8, 'Omeprazol 20mg',      '1 cápsula', 'Antes del desayuno', '8 semanas'),
  (10, 'Nitrofurantoína 100mg','1 cápsula','Cada 6 horas',  '5 días')
ON CONFLICT DO NOTHING;

-- ========================
-- Citas (15 — pasadas y futuras)
-- ========================
INSERT INTO citas (paciente_id, personal_id, servicio, fecha_hora, estado, notas) VALUES
  (1,  1, 'Consulta Externa', '2025-02-20 08:00:00', 'COMPLETADA', 'Control de hipertensión trimestral'),
  (2,  1, 'Consulta Externa', '2025-02-20 09:00:00', 'COMPLETADA', 'Seguimiento faringitis'),
  (3,  2, 'Medicina Interna',  '2025-02-21 10:00:00', 'COMPLETADA', 'Control diabetes con resultados HbA1c'),
  (4,  1, 'Consulta Externa', '2025-02-22 08:30:00', 'COMPLETADA', 'Evaluación profilaxis migraña'),
  (5,  2, 'Consulta Externa', '2025-02-25 11:00:00', 'CANCELADA',  'Paciente canceló por motivos personales'),
  (7,  1, 'Consulta Externa', '2025-02-28 08:00:00', 'COMPLETADA', 'Seguimiento lumbalgia + resultados RMN'),
  (9,  3, 'Urgencias',        '2025-03-01 22:00:00', 'COMPLETADA', 'Epigastralgia aguda nocturna'),
  (10, 1, 'Consulta Externa', '2025-03-05 14:00:00', 'COMPLETADA', 'Control TSH post-ajuste levotiroxina'),
  -- Citas futuras (para probar agenda)
  (1,  1, 'Consulta Externa', '2026-02-20 08:00:00', 'AGENDADA',   'Control HTA — laboratorios previos'),
  (3,  2, 'Medicina Interna',  '2026-02-20 10:00:00', 'AGENDADA',   'Control diabetes trimestral'),
  (12, 1, 'Consulta Externa', '2026-02-21 09:00:00', 'AGENDADA',   'Control rinitis alérgica'),
  (14, 2, 'Consulta Externa', '2026-02-22 11:00:00', 'AGENDADA',   'Resultado de exámenes preventivos'),
  (5,  2, 'Consulta Externa', '2026-02-25 11:00:00', 'AGENDADA',   'Control de asma — espirometría'),
  (8,  1, 'Consulta Externa', '2026-02-27 08:30:00', 'AGENDADA',   'Consulta dolor abdominal recurrente'),
  (20, 3, 'Urgencias',        '2026-02-28 07:00:00', 'AGENDADA',   'Chequeo preventivo urgencias')
ON CONFLICT DO NOTHING;

-- ========================
-- Laboratorio solicitudes (8)
-- ========================
INSERT INTO laboratorio_solicitudes (paciente_id, solicitante_id, tipo_prueba, estado, fecha_solicitud) VALUES
  (1,  1, 'Hemograma completo',           'COMPLETADO', '2025-02-18'),
  (1,  1, 'Perfil lipídico',              'COMPLETADO', '2025-02-18'),
  (3,  2, 'Hemoglobina glicosilada HbA1c','COMPLETADO', '2025-02-19'),
  (3,  2, 'Creatinina sérica',            'COMPLETADO', '2025-02-19'),
  (10, 1, 'TSH y T4 libre',               'COMPLETADO', '2025-03-03'),
  (14, 3, 'Hemograma + Glicemia + Perfil lipídico', 'PENDIENTE', '2026-02-15'),
  (12, 1, 'IgE total',                    'PENDIENTE', '2026-02-18'),
  (5,  2, 'Espirometría',                 'PENDIENTE', '2026-02-20')
ON CONFLICT DO NOTHING;

-- ========================
-- Farmacia — medicamentos (12)
-- ========================
INSERT INTO farmacia_medicamentos (nombre, lote, fecha_vencimiento, cantidad, precio, stock_minimo, activo) VALUES
  ('Losartán 50mg tabletas x30',           'LOT-2025-001', '2026-08-15', 150, 12500.00, 20, true),
  ('Amoxicilina 500mg cápsulas x21',       'LOT-2025-002', '2026-06-30', 80,  18900.00, 15, true),
  ('Ibuprofeno 400mg tabletas x30',        'LOT-2025-003', '2027-01-10', 200, 8500.00,  25, true),
  ('Metformina 850mg tabletas x30',        'LOT-2025-004', '2026-11-20', 120, 14200.00, 20, true),
  ('Sumatriptán 50mg tabletas x6',         'LOT-2025-005', '2026-09-01', 45,  35000.00, 10, true),
  ('Propranolol 40mg tabletas x30',        'LOT-2025-006', '2026-12-15', 90,  9800.00,  15, true),
  ('Salbutamol inhalador 100mcg 200 dosis','LOT-2025-007', '2026-07-25', 60,  22000.00, 10, true),
  ('Fluticasona nasal spray 120 dosis',    'LOT-2025-008', '2026-10-30', 55,  45000.00, 10, true),
  ('Omeprazol 20mg cápsulas x28',          'LOT-2025-009', '2027-03-15', 180, 11000.00, 25, true),
  ('Nitrofurantoína 100mg cápsulas x20',   'LOT-2025-010', '2026-05-20', 70,  16500.00, 15, true),
  ('Loratadina 10mg tabletas x30',         'LOT-2025-011', '2027-02-28', 160, 7500.00,  20, true),
  ('Levotiroxina 75mcg tabletas x30',      'LOT-2025-012', '2026-11-10', 100, 19500.00, 15, true)
ON CONFLICT DO NOTHING;

-- ========================
-- Dispensaciones de farmacia (8)
-- ========================
INSERT INTO farmacia_dispensaciones (medicamento_id, paciente_id, cantidad, fecha_dispensacion, entregado_por) VALUES
  (1,  1, 1, '2025-02-20 08:45:00', 'Claudia Vargas'),
  (2,  2, 1, '2025-02-20 09:30:00', 'Claudia Vargas'),
  (3,  2, 1, '2025-02-20 09:30:00', 'Claudia Vargas'),
  (4,  3, 1, '2025-02-21 10:45:00', 'Claudia Vargas'),
  (5,  4, 1, '2025-02-22 09:00:00', 'Claudia Vargas'),
  (9,  9, 2, '2025-03-01 22:30:00', 'Claudia Vargas'),
  (12, 10, 1, '2025-03-05 14:30:00', 'Claudia Vargas'),
  (11, 12, 1, '2025-10-20 08:30:00', 'Claudia Vargas')
ON CONFLICT DO NOTHING;

-- ========================
-- Urgencias (3)
-- ========================
INSERT INTO urgencias (paciente_id, nivel_triage, estado, fecha_hora_ingreso, observaciones) VALUES
  (9,  'TRIAGE_III', 'ATENDIDO',  '2025-03-01 21:45:00', 'Paciente ingresa por epigastralgia intensa, sin signos de abdomen agudo'),
  (13, 'TRIAGE_II',  'ATENDIDO',  '2025-01-15 06:30:00', 'Dolor torácico atípico, descartado síndrome coronario agudo'),
  (7,  'TRIAGE_IV',  'EN_ESPERA', '2026-02-14 15:00:00', 'Lumbalgia aguda con limitación funcional moderada')
ON CONFLICT DO NOTHING;

-- ========================
-- Hospitalizaciones (2)
-- ========================
INSERT INTO hospitalizaciones (paciente_id, servicio, cama, estado, fecha_ingreso, fecha_egreso, evolucion_diaria, ordenes_medicas, epicrisis) VALUES
  (13, 'Medicina Interna', 'Cama 204-B', 'EGRESADO', '2025-01-15 07:00:00', '2025-01-17 11:00:00',
    'Día 1: Monitoreo continuo, troponinas seriadas negativas. Día 2: Estable, dolor resuelto, tolerando vía oral.',
    'Monitoreo cardiaco continuo. Troponinas c/6h. ECG seriado. Dieta blanda. ASA 100mg/día.',
    'Paciente masculino 47 años con dolor torácico atípico. SCA descartado por troponinas negativas seriadas y ECG sin cambios isquémicos. Egresa estable.'),
  (3, 'Medicina Interna', 'Cama 108-A', 'INGRESADO', '2026-02-10 14:00:00', NULL,
    'Día 1: Ingresa por descompensación diabética, glicemia 380 mg/dL. Hidratación IV + insulina cristalina. Día 4: Glicemia estabilizada en 180 mg/dL.',
    'Insulina cristalina según esquema. Hidratación con SSN 0.9%. Glicemia capilar c/4h. Dieta diabética 1800 kcal.',
    NULL)
ON CONFLICT DO NOTHING;

-- ========================
-- Consultas (para el módulo flujo clínico) (5)
-- ========================
INSERT INTO consultas (paciente_id, personal_id, cita_id, motivo_consulta, enfermedad_actual, fecha_consulta) VALUES
  (1,  1, 1, 'Control de hipertensión trimestral', 'PA estable, sin síntomas', '2025-02-20 08:15:00'),
  (2,  1, 2, 'Seguimiento faringitis', 'Síntomas resueltos tras antibiótico', '2025-02-20 09:15:00'),
  (3,  2, 3, 'Control diabetes con HbA1c', 'HbA1c mejorada a 7.2%, sin hipoglicemias', '2025-02-21 10:15:00'),
  (4,  1, 4, 'Evaluación profilaxis migraña', 'Reducción de episodios de 4 a 1/mes con propranolol', '2025-02-22 08:45:00'),
  (7,  1, 6, 'Seguimiento lumbalgia', 'RMN muestra protrusión discal L4-L5, mejoría parcial con fisioterapia', '2025-02-28 08:30:00')
ON CONFLICT DO NOTHING;

-- ========================
-- Órdenes clínicas (4)
-- ========================
INSERT INTO ordenes_clinicas (paciente_id, consulta_id, tipo, detalle, estado, valor_estimado) VALUES
  (1, 1, 'LABORATORIO', 'Hemograma, perfil lipídico, creatinina, potasio',      'COMPLETADA', 85000.00),
  (3, 3, 'LABORATORIO', 'HbA1c, creatinina, microalbuminuria',                   'COMPLETADA', 120000.00),
  (7, 5, 'IMAGEN',      'RMN columna lumbar sin contraste',                      'COMPLETADA', 450000.00),
  (4, 4, 'LABORATORIO', 'Hemograma, perfil tiroideo',                             'PENDIENTE',  95000.00)
ON CONFLICT DO NOTHING;

-- ========================
-- Facturas (3)
-- ========================
INSERT INTO facturas (paciente_id, orden_id, valor_total, estado, descripcion) VALUES
  (1, 1, 85000.00,  'PAGADA',    'Laboratorios de control HTA — Feb 2025'),
  (3, 2, 120000.00, 'PAGADA',    'Laboratorios control diabetes — Feb 2025'),
  (7, 3, 450000.00, 'PENDIENTE', 'RMN columna lumbar — Feb 2025')
ON CONFLICT DO NOTHING;
