/**
 * Crea la tabla global de tokens de recuperación en {@code public} (sin depender del search_path del tenant).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Component
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class PasswordResetPublicSchemaInitializer implements CommandLineRunner {

    private final DataSource dataSource;

    /** Nombre alineado con {@code @UniqueConstraint} en la entidad (evita WARN de Hibernate ddl-auto). */
    private static final String DDL = """
            CREATE TABLE IF NOT EXISTS public.password_reset_tokens_public (
                id BIGSERIAL PRIMARY KEY,
                email VARCHAR(255) NOT NULL,
                tenant_schema VARCHAR(63) NOT NULL,
                token VARCHAR(128) NOT NULL,
                expira_en TIMESTAMPTZ NOT NULL,
                usado BOOLEAN NOT NULL DEFAULT false,
                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                CONSTRAINT uk_pwd_reset_public_token UNIQUE (token)
            )
            """;

    /**
     * Instalaciones antiguas: UNIQUE en token con nombre por defecto de PostgreSQL; renombrar para que
     * coincida con Hibernate y no intente eliminar una restricción inexistente al arrancar.
     */
    private static final String DDL_RENAME_TOKEN_UNIQUE = """
            DO $$
            BEGIN
              IF EXISTS (
                SELECT 1 FROM pg_constraint c
                JOIN pg_class t ON t.oid = c.conrelid
                JOIN pg_namespace n ON n.oid = t.relnamespace
                WHERE n.nspname = 'public'
                  AND t.relname = 'password_reset_tokens_public'
                  AND c.conname = 'password_reset_tokens_public_token_key'
              ) THEN
                ALTER TABLE public.password_reset_tokens_public
                  RENAME CONSTRAINT password_reset_tokens_public_token_key TO uk_pwd_reset_public_token;
              END IF;
            END $$
            """;

    @Override
    public void run(String... args) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(DDL);
            stmt.execute(DDL_RENAME_TOKEN_UNIQUE);
            stmt.execute(
                    "CREATE INDEX IF NOT EXISTS idx_pwd_reset_pub_email ON public.password_reset_tokens_public(email)");
            log.info("public.password_reset_tokens_public verificada");
        } catch (Exception e) {
            log.error("No se pudo crear public.password_reset_tokens_public: {}", e.getMessage());
        }
    }
}
