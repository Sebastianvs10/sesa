/**
 * Guard: control de acceso por rol/módulo (RBAC).
 * Autor: Ing. J Sebastian Vargas S
 */

import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { PermissionsService, Modulo } from '../services/permissions.service';

/**
 * Guard que verifica si el usuario tiene acceso al módulo indicado.
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
    if (permissions.canAccess(modulo)) return true;

    router.navigate(['/dashboard']);
    return false;
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
