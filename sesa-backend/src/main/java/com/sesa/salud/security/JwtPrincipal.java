/**
 * Principal JWT con datos del usuario autenticado.
 * Soporta múltiples roles simultáneos.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.security;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class JwtPrincipal implements Serializable {

    private final String username;
    private final Long userId;
    private final Set<String> roles;
    private final String schema;

    public JwtPrincipal(String username, Long userId, Set<String> roles, String schema) {
        this.username = username;
        this.userId   = userId;
        this.roles    = roles != null ? Collections.unmodifiableSet(new LinkedHashSet<>(roles)) : Set.of();
        this.schema   = schema;
    }

    /** Backward-compat: acepta un único rol. */
    public JwtPrincipal(String username, Long userId, String role, String schema) {
        this(username, userId, role != null ? Set.of(role) : Set.of(), schema);
    }

    public String username() { return username; }
    public Long   userId()   { return userId; }
    public String schema()   { return schema; }

    /** Todos los roles asignados al usuario. */
    public Set<String> roles() { return roles; }

    /**
     * Rol primario (SUPERADMINISTRADOR > ADMIN > primero).
     * Mantiene la firma original para retrocompatibilidad con código que llame a role().
     */
    public String role() {
        if (roles.isEmpty()) return null;
        if (roles.contains("SUPERADMINISTRADOR")) return "SUPERADMINISTRADOR";
        if (roles.contains("ADMIN")) return "ADMIN";
        return roles.iterator().next();
    }

    /** Indica si el usuario tiene el rol indicado. */
    public boolean hasRole(String rol) {
        return roles.contains(rol);
    }
}
