/**
 * Publicado tras restablecer contraseña con éxito.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.event.email;

public record PasswordChangedEvent(String email, String recipientDisplayName) {}
