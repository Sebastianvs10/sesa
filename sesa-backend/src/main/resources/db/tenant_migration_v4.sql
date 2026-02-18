-- Migración V4: Seguridad (auditoría de acceso y recuperación de contraseña)
-- Autor: Ing. J Sebastian Vargas S

CREATE TABLE IF NOT EXISTS acceso_auditoria (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    evento VARCHAR(40) NOT NULL,
    ip VARCHAR(45),
    detalle TEXT,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    token VARCHAR(120) NOT NULL UNIQUE,
    expira_en TIMESTAMP NOT NULL,
    usado BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
