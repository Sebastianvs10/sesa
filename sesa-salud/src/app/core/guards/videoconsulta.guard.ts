/**
 * Guard: acceso a videoconsulta.
 * Si la URL tiene ?room=XXX&token=YYY, permite entrar sin login (enlace compartido).
 * Si no hay room, exige estar autenticado (crear sala desde consulta).
 * Con room pero sin token no se permite acceso anónimo (enlace incompleto).
 * Autor: Ing. J Sebastian Vargas S
 */

import { inject } from '@angular/core';
import { Router, CanActivateFn, ActivatedRouteSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const videoconsultaGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const router = inject(Router);
  const auth = inject(AuthService);
  const room = route.queryParamMap.get('room');
  const token = route.queryParamMap.get('token');
  const hasRoomAndToken = !!room?.trim() && !!token?.trim();

  if (hasRoomAndToken) {
    return true;
  }
  if (auth.isAuthenticated()) {
    return true;
  }
  router.navigate(['/login']);
  return false;
};
