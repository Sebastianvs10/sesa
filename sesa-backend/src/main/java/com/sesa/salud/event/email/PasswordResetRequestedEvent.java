/**
 * Publicado tras persistir el token de recuperación (envío de correo tras commit).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.event.email;

public record PasswordResetRequestedEvent(
        String email,
        String token,
        int ttlMinutes,
        String recipientDisplayName
) {}
