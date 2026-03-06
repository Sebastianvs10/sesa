/**
 * Guard: control de acceso por rol/módulo (RBAC).
 * Autor: Ing. J Sebastian Vargas S
 */

import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { map } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';
import { PermissionsService, Modulo } from '../services/permissions.service';

/**
 * Guard que verifica si el usuario tiene acceso al módulo indicado.
 * Si los permisos aún no han cargado (p. ej. al recargar), espera a que carguen para no redirigir al dashboard.
 * Uso: canActivate: [authGuard, roleGuard('PACIENTES')]
 */
export function roleGuard(modulo: Modulo | string): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const permissions = inject(PermissionsService);
    const router = inject(Router);

    if (!auth.isAuthenticated()) {
      router.navigate(['/login']);
      return false;
    }
    if (auth.isSuperAdmin()) return true;
    if (permissions.loaded() && permissions.canAccess(modulo)) return true;
    if (permissions.loaded() && !permissions.canAccess(modulo)) {
      router.navigate(['/dashboard']);
      return false;
    }

    // Permisos aún no cargados (recarga): esperar carga y luego decidir
    return permissions.whenLoaded().pipe(
      map(() => {
        if (permissions.canAccess(modulo)) return true;
        router.navigate(['/dashboard']);
        return false;
      })
    );
  };
}

/**
 * Guard que verifica si el usuario puede gestionar roles (SUPERADMINISTRADOR).
 */
export function rolesManagementGuard(): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    if (auth.isSuperAdmin()) return true;
    router.navigate(['/dashboard']);
    return false;
  };
}
