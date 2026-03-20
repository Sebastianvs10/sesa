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

    private static final String DDL = """
            CREATE TABLE IF NOT EXISTS public.password_reset_tokens_public (
                id BIGSERIAL PRIMARY KEY,
                email VARCHAR(255) NOT NULL,
                tenant_schema VARCHAR(63) NOT NULL,
                token VARCHAR(128) NOT NULL UNIQUE,
                expira_en TIMESTAMPTZ NOT NULL,
                usado BOOLEAN NOT NULL DEFAULT false,
                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
            )
            """;

    @Override
    public void run(String... args) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(DDL);
            stmt.execute(
                    "CREATE INDEX IF NOT EXISTS idx_pwd_reset_pub_email ON public.password_reset_tokens_public(email)");
            log.info("public.password_reset_tokens_public verificada");
        } catch (Exception e) {
            log.error("No se pudo crear public.password_reset_tokens_public: {}", e.getMessage());
        }
    }
}
