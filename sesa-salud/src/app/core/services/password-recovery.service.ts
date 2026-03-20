/**
 * Recuperación de contraseña: solicitud de código y confirmación (API /auth/password/*).
 * Autor: Ing. J Sebastian Vargas S
 */
import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

/** Error normalizado desde {@link PasswordRecoveryService} (uso en subscribe error). */
export interface PasswordRecoveryError {
  status: number;
  message: string;
}

export interface PasswordResetRequestPayload {
  email: string;
}

export interface PasswordResetConfirmPayload {
  token: string;
  newPassword: string;
}

/** Respuesta alineada con backend PasswordResetRequestResponse */
export interface PasswordResetRequestResponse {
  message: string;
  /** Solo si el backend tiene expose-token (desarrollo). */
  devToken?: string | null;
}

@Injectable({ providedIn: 'root' })
export class PasswordRecoveryService {
  private readonly base = `${environment.apiUrl}/auth/password`;

  constructor(private readonly http: HttpClient) {}

  /**
   * Paso 1: solicita generación de código (mismo mensaje público haya o no cuenta).
   */
  requestCode(email: string): Observable<PasswordResetRequestResponse> {
    const body: PasswordResetRequestPayload = { email: email.trim().toLowerCase() };
    return this.http
      .post<PasswordResetRequestResponse>(`${this.base}/request-reset`, body)
      .pipe(catchError((e) => throwError(() => this.normalizeError(e))));
  }

  /**
   * Paso 2: establece nueva contraseña con el código recibido (correo en producción; devToken en dev).
   */
  confirmNewPassword(token: string, newPassword: string): Observable<PasswordResetRequestResponse> {
    const body: PasswordResetConfirmPayload = {
      token: token.trim(),
      newPassword,
    };
    return this.http
      .post<PasswordResetRequestResponse>(`${this.base}/reset`, body)
      .pipe(catchError((e) => throwError(() => this.normalizeError(e))));
  }

  private normalizeError(err: unknown): PasswordRecoveryError {
    if (err instanceof HttpErrorResponse) {
      const msg =
        err.error?.message ??
        err.error?.error ??
        (err.status === 429
          ? 'Demasiadas solicitudes. Espera un minuto e inténtalo de nuevo.'
          : err.status === 0
            ? 'No hay conexión con el servidor.'
            : 'No se pudo completar la operación.');
      return { status: err.status, message: msg };
    }
    return { status: 0, message: 'Error inesperado.' };
  }
}
