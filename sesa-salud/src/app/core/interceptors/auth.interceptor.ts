import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, EMPTY, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * POST públicos de auth donde un 401 es respuesta de negocio (credenciales incorrectas),
 * no "sesión expirada". Deben propagarse al suscriptor (p. ej. pantalla de login).
 */
function isPublicAuthFailureResponse(req: { url: string; method: string }): boolean {
  if (req.method.toUpperCase() !== 'POST') {
    return false;
  }
  const u = req.url;
  return u.includes('/auth/login') || u.includes('/auth/password/');
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.token();
  if (token) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    });
  }
  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401) {
        if (isPublicAuthFailureResponse(req)) {
          return throwError(() => err);
        }
        auth.logout();
        router.navigate(['/login']);
        // No re-lanzar el error: evita que el suscriptor muestre "Error: No autenticado"
        // tras un cierre de sesión intencional o sesión expirada.
        return EMPTY;
      }
      return throwError(() => err);
    })
  );
};
