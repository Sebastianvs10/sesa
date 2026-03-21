/**
 * Desacopla el envío de correos del hilo de la petición y del commit transaccional.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.event.email;

import com.sesa.salud.config.EmailAsyncConfig;
import com.sesa.salud.service.TransactionalEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionalEmailEventListener {

    private final TransactionalEmailService transactionalEmailService;

    @Async(EmailAsyncConfig.EMAIL_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPasswordResetRequested(PasswordResetRequestedEvent e) {
        try {
            transactionalEmailService.sendPasswordResetCode(
                    e.email(), e.recipientDisplayName(), e.token(), e.ttlMinutes());
        } catch (Exception ex) {
            log.warn("No se pudo enviar correo de recuperación: {}", ex.getMessage());
        }
    }

    @Async(EmailAsyncConfig.EMAIL_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPasswordChanged(PasswordChangedEvent e) {
        try {
            transactionalEmailService.sendPasswordChangedNotice(e.email(), e.recipientDisplayName());
        } catch (Exception ex) {
            log.warn("No se pudo enviar correo de contraseña actualizada: {}", ex.getMessage());
        }
    }

    @Async(EmailAsyncConfig.EMAIL_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTenantAdminWelcome(TenantAdminWelcomeEmailEvent e) {
        try {
            transactionalEmailService.sendTenantAdminWelcome(
                    e.adminEmail(), e.adminDisplayName(), e.razonSocial());
        } catch (Exception ex) {
            log.warn("No se pudo enviar bienvenida de tenant: {}", ex.getMessage());
        }
    }

    @Async(EmailAsyncConfig.EMAIL_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNewUserWelcome(NewUserWelcomeEmailEvent e) {
        try {
            transactionalEmailService.sendWelcomeNewUser(e.email(), e.displayName(), e.organizationHint());
        } catch (Exception ex) {
            log.warn("No se pudo enviar bienvenida de usuario: {}", ex.getMessage());
        }
    }
}
