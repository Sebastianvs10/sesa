/**
 * Guard: solo permite acceso si el usuario tiene rol SUPERADMINISTRADOR.
 * Autor: Ing. J Sebastian Vargas S
 */

import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const superAdminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const role = auth.currentUser()?.role;
  if (role === 'SUPERADMINISTRADOR') {
    return true;
  }
  router.navigate(['/empresas']);
  return false;
};
