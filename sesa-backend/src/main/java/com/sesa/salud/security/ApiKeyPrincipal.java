/**
 * S12: Principal para autenticación por API Key (integradores).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.security;

import java.io.Serializable;
import java.util.Set;

public final class ApiKeyPrincipal implements Serializable {
    private final String nombreIntegrador;
    private final String schema;
    private final Set<String> permisos;

    public ApiKeyPrincipal(String nombreIntegrador, String schema, Set<String> permisos) {
        this.nombreIntegrador = nombreIntegrador;
        this.schema = schema;
        this.permisos = permisos != null ? Set.copyOf(permisos) : Set.of();
    }

    public String getNombreIntegrador() { return nombreIntegrador; }
    public String getSchema() { return schema; }
    public Set<String> getPermisos() { return permisos; }
    public boolean hasPermiso(String p) { return permisos.contains(p); }
}
