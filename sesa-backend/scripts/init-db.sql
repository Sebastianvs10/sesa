-- Crear usuario y base de datos para SESA (ejecutar como superuser, p. ej. postgres)
CREATE USER sesa WITH PASSWORD 'sesa_secret';
CREATE DATABASE sesa_db OWNER sesa;
GRANT ALL PRIVILEGES ON DATABASE sesa_db TO sesa;
\c sesa_db
GRANT ALL ON SCHEMA public TO sesa;
