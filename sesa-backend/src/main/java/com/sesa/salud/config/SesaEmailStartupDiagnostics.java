/**
 * Diagnóstico al arranque: coherencia entre sesa.email.enabled y RESEND_API_KEY.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class SesaEmailStartupDiagnostics implements ApplicationRunner {

    private final SesaEmailProperties emailProperties;

    @Override
    public void run(ApplicationArguments args) {
        boolean hasKey =
                emailProperties.getResendApiKey() != null && !emailProperties.getResendApiKey().isBlank();

        if (emailProperties.isEnabled() && !hasKey) {
            log.warn(
                    "[sesa.email] Envío habilitado pero RESEND_API_KEY (o sesa.email.resend-api-key) está vacía: no se enviarán correos.");
        } else if (!emailProperties.isEnabled() && hasKey) {
            log.warn(
                    "[sesa.email] Hay API key de Resend configurada pero sesa.email.enabled=false: no se enviarán correos. En producción defina SESA_EMAIL_ENABLED=true.");
        } else if (emailProperties.isResendConfigured()) {
            log.info("[sesa.email] Resend operativo (envío activo y clave presente).");
        } else {
            log.info("[sesa.email] Correo transaccional inactivo (deshabilitado o sin clave).");
        }
    }
}
