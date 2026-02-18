/**
 * Guard: solo permite acceso si el usuario tiene rol MEDICO
 * Autor: Ing. J Sebastian Vargas S
 */

import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const medicoGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const role = auth.currentUser()?.role;
  if (['MEDICO', 'ODONTOLOGO', 'ADMIN', 'SUPERADMINISTRADOR'].includes(role ?? '')) {
    return true;
  }
  router.navigate(['/dashboard']);
  return false;
};
