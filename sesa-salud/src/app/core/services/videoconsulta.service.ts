/**
 * Servicio de Telemedicina — Videoconsulta WebRTC.
 * Gestiona salas, señalización (REST polling) y conexión peer-to-peer.
 * Autor: Ing. J Sebastian Vargas S
 */

import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, interval, Subject, firstValueFrom, throwError } from 'rxjs';
import { switchMap, takeWhile, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface SalaVideoconsultaDto {
  salaId: string;
  role: 'creador' | 'participante';
  token: string;
  citaId?: number;
  profesionalId?: number;
  pacienteId?: number;
}

export interface SignalingEventDto {
  type: 'offer' | 'answer' | 'ice';
  payload: string; // SDP o ICE candidate JSON
}

@Injectable({ providedIn: 'root' })
export class VideoconsultaService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/videoconsulta`;

  private readonly _estado = signal<'idle' | 'creando' | 'uniendo' | 'conectando' | 'conectado' | 'error'>('idle');
  private readonly _error = signal<string | null>(null);
  private readonly _salaId = signal<string | null>(null);
  private readonly _role = signal<'creador' | 'participante' | null>(null);
  /** Token de invitación: se guarda al crear o unirse; se envía en unirse y en todas las llamadas de signaling. */
  private readonly _token = signal<string | null>(null);

  readonly estado = this._estado.asReadonly();
  readonly error = this._error.asReadonly();
  readonly salaId = this._salaId.asReadonly();
  readonly role = this._role.asReadonly();
  readonly token = this._token.asReadonly();
  readonly isConectado = computed(() => this._estado() === 'conectado');

  /** Crea una sala de videoconsulta (médico). Opcional: citaId para vincular. */
  crearSala(citaId?: number, profesionalId?: number): Observable<SalaVideoconsultaDto> {
    this._estado.set('creando');
    this._error.set(null);
    return this.http.post<SalaVideoconsultaDto>(`${this.apiUrl}/salas`, { citaId, profesionalId }).pipe(
      tap((s) => {
        this._salaId.set(s.salaId);
        this._role.set(s.role);
        this._token.set(s.token ?? null);
        this._estado.set('idle');
      })
    );
  }

  /** Unirse a una sala existente con el token del enlace (paciente o invitado). */
  unirseSala(salaId: string, token: string): Observable<SalaVideoconsultaDto> {
    this._estado.set('uniendo');
    this._error.set(null);
    this._salaId.set(salaId);
    return this.http.get<SalaVideoconsultaDto>(`${this.apiUrl}/salas/${salaId}`, {
      params: { token },
    }).pipe(
      tap((s) => {
        this._role.set(s.role ?? 'participante');
        this._token.set(s.token ?? token);
        this._estado.set('idle');
      })
    );
  }

  /** Envía un evento de señalización (offer, answer o ice). Token: el del servicio o el pasado explícitamente. */
  enviarSignaling(salaId: string, event: SignalingEventDto, token?: string | null): Observable<void> {
    const t = token ?? this._token();
    if (!t) {
      return throwError(() => new Error('Falta token de sala'));
    }
    if (!this._token()) {
      this._token.set(t);
    }
    return this.http.post<void>(`${this.apiUrl}/salas/${salaId}/signaling`, event, {
      params: { token: t },
    });
  }

  /** Obtiene eventos de señalización desde un índice (polling). Token: el del servicio o el pasado explícitamente. */
  obtenerSignaling(salaId: string, afterIndex: number, token?: string | null): Observable<{ events: Array<SignalingEventDto & { index: number }> }> {
    const t = token ?? this._token();
    if (!t) {
      return throwError(() => new Error('Falta token de sala'));
    }
    if (!this._token()) {
      this._token.set(t);
    }
    return this.http.get<{ events: Array<SignalingEventDto & { index: number }> }>(
      `${this.apiUrl}/salas/${salaId}/signaling`,
      { params: { after: afterIndex.toString(), token: t } }
    );
  }

  /** Limpia estado local (al colgar). */
  limpiarSala(): void {
    this._estado.set('idle');
    this._error.set(null);
    this._salaId.set(null);
    this._role.set(null);
    this._token.set(null);
  }

  /** Construye la URL para compartir (incluye room y token). */
  getUrlUnirse(salaId: string, token: string): string {
    const base = typeof window !== 'undefined' ? window.location.origin : '';
    return `${base}/videoconsulta?room=${encodeURIComponent(salaId)}&token=${encodeURIComponent(token)}`;
  }

  /** Creador: habilita asistente y devuelve token para el enlace. */
  habilitarAsistente(salaId: string, token: string): Observable<{ tokenAsistente: string }> {
    return this.http.post<{ tokenAsistente: string }>(`${this.apiUrl}/salas/${salaId}/habilitar-asistente`, null, {
      params: { token },
    });
  }

  /** Paciente: registra consentimiento para que un asistente tome notas. */
  registrarConsentimientoAsistente(salaId: string, token: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/salas/${salaId}/consentimiento-asistente`, null, {
      params: { token },
    });
  }

  /** Asistente: guarda las notas (contenido completo). */
  guardarNotas(salaId: string, token: string, texto: string): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/salas/${salaId}/notas`, { texto }, {
      params: { token },
    });
  }

  /** Creador: obtiene el resumen (notas) de la reunión. */
  obtenerNotas(salaId: string, token: string): Observable<{ notas: string }> {
    return this.http.get<{ notas: string }>(`${this.apiUrl}/salas/${salaId}/notas`, {
      params: { token },
    });
  }

  /** Valida si el token es de asistente y hay consentimiento. */
  validarAsistente(salaId: string, token: string): Observable<{ valido: boolean }> {
    return this.http.get<{ valido: boolean }>(`${this.apiUrl}/salas/${salaId}/validar-asistente`, {
      params: { token },
    });
  }

  /** Paciente: true si el profesional solicitó asistente y aún no ha autorizado. */
  solicitudAsistentePendiente(salaId: string, token: string): Observable<{ pendiente: boolean }> {
    return this.http.get<{ pendiente: boolean }>(`${this.apiUrl}/salas/${salaId}/solicitud-asistente-pendiente`, {
      params: { token },
    });
  }

  /** URL para el asistente (solo toma de notas). */
  getUrlAsistente(salaId: string, tokenAsistente: string): string {
    const base = typeof window !== 'undefined' ? window.location.origin : '';
    return `${base}/videoconsulta/asistente?room=${encodeURIComponent(salaId)}&token=${encodeURIComponent(tokenAsistente)}`;
  }
}
