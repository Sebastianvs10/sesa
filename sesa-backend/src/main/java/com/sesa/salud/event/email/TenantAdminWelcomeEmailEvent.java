/**
 * Publicado tras crear empresa/tenant y usuario administrador.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.event.email;

public record TenantAdminWelcomeEmailEvent(
        String adminEmail,
        String adminDisplayName,
        String razonSocial
) {}
