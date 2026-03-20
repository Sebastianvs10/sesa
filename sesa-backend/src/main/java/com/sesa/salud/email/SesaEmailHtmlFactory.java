/**
 * Plantillas HTML para correos transaccionales (diseño futurista, logo SESA, tablas email-safe).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.email;

import com.sesa.salud.config.SesaEmailProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SesaEmailHtmlFactory {

    private static final String WRAP_TEMPLATE = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>{{TITLE_ESC}}</title>
            </head>
            <body style="margin:0;padding:0;background:#0b0d12;font-family:'Segoe UI',Inter,system-ui,-apple-system,sans-serif;">
              <div style="display:none;max-height:0;overflow:hidden;opacity:0;color:transparent;">{{PREHEADER_ESC}}</div>
              <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background:linear-gradient(165deg,#0b0d12 0%,#121826 45%,#0f172a 100%);padding:32px 16px;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="100%" style="max-width:560px;border-collapse:collapse;">
                      <tr>
                        <td style="padding:0 0 24px 0;text-align:center;">
                          <img src="{{LOGO_URL_ESC}}" alt="{{BRAND_ESC}}" width="132" height="auto" style="display:inline-block;max-width:132px;height:auto;filter:drop-shadow(0 0 24px rgba(13,148,136,0.35));">
                        </td>
                      </tr>
                      <tr>
                        <td style="background:linear-gradient(135deg,rgba(13,148,136,0.22) 0%,rgba(99,102,241,0.18) 100%);border-radius:16px;padding:1px;">
                          <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background:rgba(15,23,42,0.92);border-radius:15px;border:1px solid rgba(148,163,184,0.12);">
                            <tr>
                              <td style="padding:28px 28px 8px 28px;">
                                <div style="font-size:11px;letter-spacing:0.14em;text-transform:uppercase;color:#5eead4;font-weight:600;margin-bottom:10px;">{{BRAND_ESC}} · Secure channel</div>
                                <h1 style="margin:0 0 16px 0;font-size:22px;line-height:1.25;font-weight:700;letter-spacing:-0.02em;color:#f8fafc;">{{HEADLINE_ESC}}</h1>
                              </td>
                            </tr>
                            <tr>
                              <td style="padding:0 28px 28px 28px;color:#cbd5e1;font-size:15px;line-height:1.6;">
                                {{INNER_BODY}}
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:24px 8px 0 8px;text-align:center;font-size:11px;line-height:1.5;color:#64748b;">
                          {{LEGAL_ESC}}<br/>
                          <span style="opacity:0.75;">Este mensaje es automático; no respondas si no tienes canal de soporte asociado.</span>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """;

    private final SesaEmailProperties props;

    @Value("${sesa.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public String resolvedLogoUrl() {
        if (props.getLogoUrl() != null && !props.getLogoUrl().isBlank()) {
            return props.getLogoUrl().trim();
        }
        String base = frontendUrl == null ? "" : frontendUrl.replaceAll("/$", "");
        return base + "/logo.png";
    }

    public String wrap(String title, String preheader, String headline, String innerHtml) {
        String brand = esc(props.getBrandName());
        String legal = esc(props.getFooterLegal());
        return WRAP_TEMPLATE
                .replace("{{TITLE_ESC}}", esc(title))
                .replace("{{PREHEADER_ESC}}", esc(preheader != null ? preheader : title))
                .replace("{{LOGO_URL_ESC}}", esc(resolvedLogoUrl()))
                .replace("{{BRAND_ESC}}", brand)
                .replace("{{HEADLINE_ESC}}", esc(headline != null ? headline : title))
                .replace("{{INNER_BODY}}", innerHtml)
                .replace("{{LEGAL_ESC}}", legal);
    }

    public String passwordResetBody(String greeting, String code, int ttlMinutes) {
        String g = esc(greeting);
        String c = esc(code);
        String brand = esc(props.getBrandName());
        return """
                <p style="margin:0 0 16px 0;">%s</p>
                <p style="margin:0 0 20px 0;color:#94a3b8;">Usa este código en la pantalla de recuperación de contraseña de <strong style="color:#e2e8f0;">%s</strong>:</p>
                <table role="presentation" cellspacing="0" cellpadding="0" style="width:100%;margin:0 0 20px 0;">
                  <tr>
                    <td style="background:linear-gradient(135deg,rgba(13,148,136,0.15) 0%,rgba(99,102,241,0.12) 100%);border:1px solid rgba(94,234,212,0.35);border-radius:12px;padding:18px 20px;text-align:center;">
                      <span style="font-family:ui-monospace,'Cascadia Code',Consolas,monospace;font-size:22px;font-weight:700;letter-spacing:0.18em;color:#5eead4;">%s</span>
                    </td>
                  </tr>
                </table>
                <p style="margin:0;color:#94a3b8;font-size:13px;">Caduca en <strong style="color:#e2e8f0;">%d minutos</strong>. Si no solicitaste el cambio, ignora este correo y revisa la seguridad de tu cuenta.</p>
                """
                .formatted(g, brand, c, ttlMinutes);
    }

    public String passwordChangedBody(String greeting) {
        return """
                <p style="margin:0 0 16px 0;">%s</p>
                <p style="margin:0 0 12px 0;">La contraseña de tu cuenta en <strong style="color:#e2e8f0;">%s</strong> se actualizó correctamente.</p>
                <p style="margin:0;color:#94a3b8;font-size:13px;">Si no fuiste tú, contacta de inmediato al administrador de tu organización.</p>
                """
                .formatted(esc(greeting), esc(props.getBrandName()));
    }

    public String welcomeUserBody(String greeting, String extraLine) {
        String extra = extraLine != null && !extraLine.isBlank()
                ? "<p style=\"margin:16px 0 0 0;color:#94a3b8;\">" + esc(extraLine) + "</p>"
                : "";
        return """
                <p style="margin:0 0 16px 0;">%s</p>
                <p style="margin:0;">Tu cuenta ya está activa en <strong style="color:#e2e8f0;">%s</strong>. Accede con las credenciales que te proporcionó tu administrador.</p>
                %s
                <table role="presentation" cellspacing="0" cellpadding="0" style="margin:24px 0 0 0;">
                  <tr>
                    <td style="border-radius:10px;background:linear-gradient(90deg,#0d9488,#6366f1);padding:2px;">
                      <a href="%s" style="display:inline-block;padding:12px 22px;border-radius:8px;background:#0f172a;color:#f8fafc;font-weight:600;font-size:14px;text-decoration:none;">Ir al inicio de sesión</a>
                    </td>
                  </tr>
                </table>
                """
                .formatted(esc(greeting), esc(props.getBrandName()), extra, esc(loginUrl()));
    }

    public String tenantAdminWelcomeBody(String greeting, String empresaNombre) {
        return """
                <p style="margin:0 0 16px 0;">%s</p>
                <p style="margin:0 0 12px 0;">Se creó el espacio de trabajo para <strong style="color:#e2e8f0;">%s</strong>. Ya puedes operar historias clínicas, facturación electrónica y más desde un solo lugar.</p>
                <ul style="margin:12px 0 16px 18px;padding:0;color:#94a3b8;font-size:14px;">
                  <li>Multi-sede y roles en tiempo real</li>
                  <li>RIPS y trazabilidad normativa</li>
                  <li>Experiencia clínica unificada</li>
                </ul>
                <table role="presentation" cellspacing="0" cellpadding="0">
                  <tr>
                    <td style="border-radius:10px;background:linear-gradient(90deg,#0d9488,#6366f1);padding:2px;">
                      <a href="%s" style="display:inline-block;padding:12px 22px;border-radius:8px;background:#0f172a;color:#f8fafc;font-weight:600;font-size:14px;text-decoration:none;">Entrar a %s</a>
                    </td>
                  </tr>
                </table>
                """
                .formatted(esc(greeting), esc(empresaNombre), esc(loginUrl()), esc(props.getBrandName()));
    }

    public String securityNoticeBody(String greeting, String detail) {
        return """
                <p style="margin:0 0 16px 0;">%s</p>
                <p style="margin:0 0 12px 0;color:#fca5a5;">%s</p>
                <p style="margin:0;color:#94a3b8;font-size:13px;">Si reconoces esta actividad, no hagas nada. En caso contrario, cambia tu contraseña y avisa a soporte.</p>
                """
                .formatted(esc(greeting), esc(detail));
    }

    private String loginUrl() {
        String base = frontendUrl == null ? "http://localhost:4200" : frontendUrl.trim();
        return base.replaceAll("/$", "") + "/login";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
