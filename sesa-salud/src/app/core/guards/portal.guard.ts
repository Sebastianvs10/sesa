/**
 * Autor: Ing. J Sebastian Vargas S
 */

import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Guard para el Portal del Paciente.
 * Permite acceso si el usuario está autenticado con rol PACIENTE,
 * o si es personal médico/administrativo (acceso de lectura al portal).
 */
export const portalGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isAuthenticated()) {
    router.navigate(['/login']);
    return false;
  }

  const roles = auth.currentRoles();
  const portalRoles = ['PACIENTE', 'MEDICO', 'ODONTOLOGO', 'ADMIN', 'SUPERADMINISTRADOR', 'ENFERMERA', 'BACTERIOLOGO'];
  const hasAccess = roles.some(r => portalRoles.includes(r));

  if (!hasAccess) {
    router.navigate(['/dashboard']);
    return false;
  }

  return true;
};

/**
 * Guard exclusivo para pacientes — restringe rutas solo al rol PACIENTE.
 */
export const soloPackienteGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isAuthenticated()) {
    router.navigate(['/login']);
    return false;
  }

  if (auth.currentRoles().includes('PACIENTE')) {
    return true;
  }

  // Personal médico redirige al sistema principal
  router.navigate(['/dashboard']);
  return false;
};
