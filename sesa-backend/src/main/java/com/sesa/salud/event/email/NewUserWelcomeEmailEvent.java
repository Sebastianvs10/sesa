/**
 * Publicado al crear un usuario en un tenant.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.event.email;

public record NewUserWelcomeEmailEvent(
        String email,
        String displayName,
        String organizationHint
) {}
