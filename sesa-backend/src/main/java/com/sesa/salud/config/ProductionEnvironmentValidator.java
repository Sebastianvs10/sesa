/**
 * Fallo al arranque en perfil {@code prod} si el JWT o la base de datos usan valores inseguros.
 * Las variables obligatorias se resuelven vía {@code application-prod.yml} y el entorno (Render, etc.).
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Orden alto para fallar antes de exponer la aplicación.
 */
@Component
@Profile("prod")
@Order(0)
public class ProductionEnvironmentValidator implements ApplicationRunner {

    private static final int JWT_MIN_LENGTH = 32;
    private static final String PLACEHOLDER_JWT = "cambiar_jwt";
    private static final String DEV_JWT_MARKER = "dev-secret-key-only-for-development";
    private static final String PLACEHOLDER_DB_PASSWORD = "cambiar_password_produccion";

    @Value("${sesa.jwt.secret}")
    private String jwtSecret;

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Override
    public void run(ApplicationArguments args) {
        validateJwt();
        validateDatasource();
    }

    private void validateJwt() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("Producción: sesa.jwt.secret / SESA_JWT_SECRET no puede estar vacío.");
        }
        if (jwtSecret.length() < JWT_MIN_LENGTH) {
            throw new IllegalStateException(
                    "Producción: JWT demasiado corto para HS256 (mín. " + JWT_MIN_LENGTH + " caracteres).");
        }
        if (jwtSecret.contains(PLACEHOLDER_JWT) || jwtSecret.contains(DEV_JWT_MARKER)) {
            throw new IllegalStateException("Producción: JWT no puede ser un valor de ejemplo o de desarrollo.");
        }
    }

    private void validateDatasource() {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalStateException("Producción: spring.datasource.url / SPRING_DATASOURCE_URL requerido.");
        }
        boolean localDb = jdbcUrl.contains("localhost") || jdbcUrl.contains("127.0.0.1");
        if (!localDb) {
            if (dbPassword == null || dbPassword.isBlank()) {
                throw new IllegalStateException("Producción: contraseña de base de datos requerida (PostgreSQL remoto).");
            }
            if (PLACEHOLDER_DB_PASSWORD.equals(dbPassword)) {
                throw new IllegalStateException(
                        "Producción: sustituya el placeholder de contraseña de base de datos por un secreto real.");
            }
        }
    }
}
