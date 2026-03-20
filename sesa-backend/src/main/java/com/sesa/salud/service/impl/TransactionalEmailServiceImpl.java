/**
 * Implementación: plantillas SESA + API Resend.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service.impl;

import com.sesa.salud.config.SesaEmailProperties;
import com.sesa.salud.email.SesaEmailHtmlFactory;
import com.sesa.salud.email.resend.ResendEmailClient;
import com.sesa.salud.service.TransactionalEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionalEmailServiceImpl implements TransactionalEmailService {

    private final SesaEmailProperties emailProperties;
    private final SesaEmailHtmlFactory htmlFactory;
    private final ResendEmailClient resendEmailClient;

    @Override
    public void sendPasswordResetCode(String toEmail, String recipientName, String code, int ttlMinutes) {
        if (!shouldSend()) return;
        String greet = greeting(recipientName);
        String inner = htmlFactory.passwordResetBody(greet, code, ttlMinutes);
        String html = htmlFactory.wrap(
                "Código de recuperación · " + emailProperties.getBrandName(),
                "Tu código de verificación para restablecer acceso",
                "Restablece tu acceso",
                inner);
        String subject = "[" + emailProperties.getBrandName() + "] Código de recuperación";
        resendEmailClient.sendHtml(toEmail, subject, html);
    }

    @Override
    public void sendPasswordChangedNotice(String toEmail, String recipientName) {
        if (!shouldSend()) return;
        String inner = htmlFactory.passwordChangedBody(greeting(recipientName));
        String html = htmlFactory.wrap(
                "Contraseña actualizada",
                "Confirmación de cambio de contraseña",
                "Tu contraseña se actualizó",
                inner);
        String subject = "[" + emailProperties.getBrandName() + "] Contraseña actualizada";
        resendEmailClient.sendHtml(toEmail, subject, html);
    }

    @Override
    public void sendWelcomeNewUser(String toEmail, String recipientName, String organizationHint) {
        if (!shouldSend()) return;
        String inner = htmlFactory.welcomeUserBody(greeting(recipientName), organizationHint);
        String html = htmlFactory.wrap(
                "Bienvenido a " + emailProperties.getBrandName(),
                "Tu cuenta ya está lista",
                "Bienvenido al ecosistema clínico",
                inner);
        String subject = "[" + emailProperties.getBrandName() + "] Tu cuenta está activa";
        resendEmailClient.sendHtml(toEmail, subject, html);
    }

    @Override
    public void sendTenantAdminWelcome(String toEmail, String recipientName, String empresaNombre) {
        if (!shouldSend()) return;
        String inner = htmlFactory.tenantAdminWelcomeBody(greeting(recipientName), empresaNombre);
        String html = htmlFactory.wrap(
                "Tu organización en " + emailProperties.getBrandName(),
                empresaNombre + " — espacio creado",
                "Tu espacio de trabajo está listo",
                inner);
        String subject = "[" + emailProperties.getBrandName() + "] " + empresaNombre + " — bienvenida administrador";
        resendEmailClient.sendHtml(toEmail, subject, html);
    }

    @Override
    public void sendSecurityAlert(String toEmail, String recipientName, String detail) {
        if (!shouldSend()) return;
        String inner = htmlFactory.securityNoticeBody(greeting(recipientName), detail);
        String html = htmlFactory.wrap(
                "Alerta de seguridad",
                "Actividad reciente en tu cuenta",
                "Revisa esta actividad",
                inner);
        String subject = "[" + emailProperties.getBrandName() + "] Alerta de seguridad";
        resendEmailClient.sendHtml(toEmail, subject, html);
    }

    private boolean shouldSend() {
        if (!emailProperties.isEnabled()) {
            log.trace("Correos transaccionales deshabilitados (sesa.email.enabled=false)");
            return false;
        }
        if (!emailProperties.isResendConfigured()) {
            log.debug("Correo omitido: habilita sesa.email.enabled y define resend-api-key (RESEND_API_KEY)");
            return false;
        }
        return true;
    }

    private static String greeting(String recipientName) {
        if (recipientName == null || recipientName.isBlank()) {
            return "Hola,";
        }
        return "Hola, " + recipientName.trim() + ",";
    }
}
