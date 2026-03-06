-- Huila (41): departamento, 37 municipios, veredas y corregimientos
-- Ejecutado por IgacHuilaSeeder contra schema public.
-- Autor: Ing. J Sebastian Vargas S

-- Departamento Huila
INSERT INTO public.igac_departamentos (codigo_dane, nombre) VALUES ('41', 'Huila') ON CONFLICT (codigo_dane) DO NOTHING;

-- 37 Municipios del Huila (códigos DANE)
INSERT INTO public.igac_municipios (codigo_dane, departamento_codigo, nombre) VALUES
('41001', '41', 'Neiva'),
('41006', '41', 'Acevedo'),
('41013', '41', 'Agrado'),
('41016', '41', 'Aipe'),
('41020', '41', 'Algeciras'),
('41026', '41', 'Altamira'),
('41078', '41', 'Baraya'),
('41132', '41', 'Campoalegre'),
('41206', '41', 'Colombia'),
('41244', '41', 'Elías'),
('41298', '41', 'Garzón'),
('41306', '41', 'Gigante'),
('41319', '41', 'Guadalupe'),
('41349', '41', 'Hobo'),
('41357', '41', 'Íquira'),
('41359', '41', 'Isnos'),
('41378', '41', 'La Argentina'),
('41396', '41', 'La Plata'),
('41483', '41', 'Nátaga'),
('41503', '41', 'Oporapa'),
('41518', '41', 'Paicol'),
('41524', '41', 'Palermo'),
('41530', '41', 'Palestina'),
('41548', '41', 'Pital'),
('41551', '41', 'Pitalito'),
('41615', '41', 'Rivera'),
('41660', '41', 'Saladoblanco'),
('41668', '41', 'San Agustín'),
('41676', '41', 'Santa María'),
('41770', '41', 'Suaza'),
('41791', '41', 'Tarqui'),
('41799', '41', 'Tello'),
('41801', '41', 'Teruel'),
('41797', '41', 'Tesalia'),
('41807', '41', 'Timaná'),
('41872', '41', 'Villavieja'),
('41885', '41', 'Yaguará')
ON CONFLICT (codigo_dane) DO NOTHING;
