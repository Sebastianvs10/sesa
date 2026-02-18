/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.tenant;

/**
 * Almacena el esquema del tenant actual para la petición (multi-tenancy por esquema).
 */
public final class TenantContextHolder {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    public static final String PUBLIC = "public";

    public static void setTenantSchema(String schemaName) {
        if (schemaName == null || schemaName.isBlank()) {
            CURRENT_TENANT.set(PUBLIC);
        } else {
            CURRENT_TENANT.set(schemaName);
        }
    }

    public static String getTenantSchema() {
        String schema = CURRENT_TENANT.get();
        return schema != null ? schema : PUBLIC;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
