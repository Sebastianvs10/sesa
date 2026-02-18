/**
 * Servicio de permisos RBAC.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service;

import com.sesa.salud.security.RoleConstants;

import java.util.Set;

public interface PermissionService {

    boolean hasPermission(String rol, RoleConstants.Modulo modulo, RoleConstants.Accion accion);

    boolean hasAnyPermission(Set<String> roles, RoleConstants.Modulo modulo, RoleConstants.Accion accion);

    boolean canAccessModule(Set<String> roles, RoleConstants.Modulo modulo);

    Set<RoleConstants.Modulo> getAccessibleModules(Set<String> roles);

    /** Actualiza los módulos permitidos para un rol (SUPERADMINISTRADOR). */
    void updateModulosForRole(String rol, Set<RoleConstants.Modulo> modulos);
}
