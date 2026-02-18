/**
 * Principal JWT con datos del usuario autenticado
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.security;

import java.io.Serializable;

public final class JwtPrincipal implements Serializable {

    private final String username;
    private final Long userId;
    private final String role;
    private final String schema;

    public JwtPrincipal(String username, Long userId, String role, String schema) {
        this.username = username;
        this.userId = userId;
        this.role = role;
        this.schema = schema;
    }

    public String username() {
        return username;
    }

    public Long userId() {
        return userId;
    }

    public String role() {
        return role;
    }

    public String schema() {
        return schema;
    }
}
