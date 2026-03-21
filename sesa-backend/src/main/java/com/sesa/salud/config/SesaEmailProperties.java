/**
 * Configuración de envío transaccional (Resend) y branding de correos.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "sesa.email")
public class SesaEmailProperties {

    /**
     * false = no se llama a Resend (kill switch). Por defecto en YAML suele ser true salvo perfil prod.
     */
    private boolean enabled = true;

    /** API key re_… (variable de entorno RESEND_API_KEY en producción). */
    private String resendApiKey = "";

    /** Remitente verificado en Resend, ej. {@code SESA <noreply@tudominio.com>}. */
    private String from = "SESA <onboarding@resend.dev>";

    /** Reply-To opcional. */
    private String replyTo = "";

    /**
     * URL absoluta del logo para &lt;img src&gt; (debe ser público en Internet).
     * Si queda vacío, se usa {@code sesa.frontend-url + /logo.png}.
     */
    private String logoUrl = "";

    /** Nombre corto de marca en el asunto y pie. */
    private String brandName = "SESA";

    /** Texto legal breve al pie (opcional). */
    private String footerLegal = "Sistema Electrónico de Salud · Colombia";

    /** URL base de la API Resend (normalmente no cambiar). */
    private String resendApiBase = "https://api.resend.com";

    public boolean isResendConfigured() {
        return enabled && resendApiKey != null && !resendApiKey.isBlank();
    }
}
