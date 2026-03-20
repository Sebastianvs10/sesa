/**
 * Envío de correos transaccionales (bienvenida, seguridad, recuperación de contraseña).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

public interface TransactionalEmailService {

    void sendPasswordResetCode(String toEmail, String recipientName, String code, int ttlMinutes);

    void sendPasswordChangedNotice(String toEmail, String recipientName);

    void sendWelcomeNewUser(String toEmail, String recipientName, String organizationHint);

    void sendTenantAdminWelcome(String toEmail, String recipientName, String empresaNombre);

    void sendSecurityAlert(String toEmail, String recipientName, String detail);
}
