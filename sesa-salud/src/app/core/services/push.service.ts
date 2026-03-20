/**
 * Registro de token para notificaciones push (recordatorios de cita, alertas).
 * Autor: Ing. J Sebastian Vargas S
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PushService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/push`;

  /**
   * Registra el token FCM/Web Push del usuario actual.
   * Llamar tras login o cuando el usuario acepte notificaciones (PWA/móvil).
   * @param token Token FCM o Web Push
   * @param plataforma 'WEB' | 'ANDROID' | 'IOS'
   */
  registerToken(token: string, plataforma: 'WEB' | 'ANDROID' | 'IOS' = 'WEB'): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/register`, { token, plataforma });
  }

  /** Elimina el token del dispositivo (al desactivar notificaciones o logout). */
  unregisterToken(token: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/register`, { params: { token } });
  }
}
