/**
 * Servicio de detección de conexión a internet en tiempo real.
 * Usa Navigator.onLine + eventos online/offline + heartbeat HTTP periódico.
 * Autor: Ing. J Sebastian Vargas S
 */

import { Injectable, OnDestroy, signal, computed } from '@angular/core';
import { Subject, fromEvent, merge, interval, Subscription, firstValueFrom, of } from 'rxjs';
import { map, distinctUntilChanged, startWith, switchMap, catchError, filter, timeout } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export type ConnectionStatus = 'online' | 'offline' | 'unstable';

@Injectable({ providedIn: 'root' })
export class ConnectionService implements OnDestroy {

  /** Signal reactivo: true = hay conexión real al servidor */
  readonly online = signal<boolean>(navigator.onLine);

  /** Signal del estado detallado */
  readonly status = signal<ConnectionStatus>(navigator.onLine ? 'online' : 'offline');

  /** Timestamp de la última reconexión detectada */
  readonly lastReconnect = signal<Date | null>(null);

  /** Computed para uso en templates */
  readonly isOnline = computed(() => this.online());
  readonly isOffline = computed(() => !this.online());

  /** Observable que emite true solo al cambiar de offline → online */
  private reconnected$ = new Subject<void>();
  readonly onReconnect$ = this.reconnected$.asObservable();

  private subs: Subscription[] = [];
  /**
   * Heartbeat apunta a /actuator/health dentro del context-path del backend.
   * Como el backend usa server.servlet.context-path=/api, el endpoint real es
   * <apiUrl>/actuator/health (p. ej. http://localhost:8081/api/actuator/health).
   */
  private heartbeatUrl = environment.apiUrl.replace(/\/?$/, '') + '/actuator/health';
  /** Intervalo entre comprobaciones (evita marcar inestable por un fallo puntual) */
  private heartbeatIntervalMs = 60_000;
  /** Cuando está "unstable", reintentar cada tantos ms para recuperar antes */
  private unstableRetryIntervalMs = 15_000;
  /** Timeout por request: si el servidor tarda más, se considera fallo */
  private heartbeatTimeoutMs = 12_000;
  private consecutiveFails = 0;
  /** Solo mostrar "inestable" tras esta cantidad de fallos consecutivos (evita falsos positivos) */
  private readonly MAX_FAILS_UNSTABLE = 4;

  constructor(private http: HttpClient) {
    this.setupBrowserEvents();
    this.setupHeartbeat();
    this.setupUnstableRecovery();
  }

  /** Escucha eventos nativos online/offline del navegador */
  private setupBrowserEvents(): void {
    const online$ = fromEvent(window, 'online').pipe(map(() => true));
    const offline$ = fromEvent(window, 'offline').pipe(map(() => false));

    const sub = merge(online$, offline$)
      .pipe(
        startWith(navigator.onLine),
        distinctUntilChanged()
      )
      .subscribe((isOnline) => {
        const wasOffline = !this.online();
        this.online.set(isOnline);
        this.status.set(isOnline ? 'online' : 'offline');

        if (isOnline && wasOffline) {
          this.consecutiveFails = 0;
          this.lastReconnect.set(new Date());
          this.reconnected$.next();
        }
      });
    this.subs.push(sub);
  }

  /** Heartbeat periódico al backend para detectar estado real */
  private setupHeartbeat(): void {
    const sub = interval(this.heartbeatIntervalMs)
      .pipe(
        filter(() => navigator.onLine),
        switchMap(() =>
          this.http.get(this.heartbeatUrl, { responseType: 'text' }).pipe(
            timeout(this.heartbeatTimeoutMs),
            map(() => true),
            catchError(() => of(false))
          )
        )
      )
      .subscribe((reachable) => {
        const wasOffline = !this.online();

        if (reachable) {
          this.consecutiveFails = 0;
          this.online.set(true);
          this.status.set('online');
          if (wasOffline) {
            this.lastReconnect.set(new Date());
            this.reconnected$.next();
          }
        } else {
          this.consecutiveFails++;
          // Solo marcar "unstable" tras varios fallos consecutivos (evita falsos positivos
          // por un fallo puntual, CORS, backend reiniciando, etc.)
          if (navigator.onLine && this.consecutiveFails >= this.MAX_FAILS_UNSTABLE) {
            this.status.set('unstable');
          }
        }
      });
    this.subs.push(sub);
  }

  /** Cuando el estado es "unstable", reintenta cada poco para volver a "online" en cuanto el servidor responda */
  private setupUnstableRecovery(): void {
    const sub = interval(this.unstableRetryIntervalMs)
      .pipe(
        filter(() => navigator.onLine && this.status() === 'unstable')
      )
      .subscribe(() => this.checkNow());
    this.subs.push(sub);
  }

  /** Fuerza un chequeo inmediato de conectividad (p. ej. botón "Reintentar") */
  async checkNow(): Promise<boolean> {
    try {
      await firstValueFrom(
        this.http.get(this.heartbeatUrl, { responseType: 'text' }).pipe(
          timeout(this.heartbeatTimeoutMs)
        )
      );
      const wasOffline = !this.online();
      this.online.set(true);
      this.status.set('online');
      this.consecutiveFails = 0;
      if (wasOffline) {
        this.lastReconnect.set(new Date());
        this.reconnected$.next();
      }
      return true;
    } catch {
      // Solo marcar offline si el navegador confirma que no hay red
      if (!navigator.onLine) {
        this.online.set(false);
        this.status.set('offline');
      } else {
        this.consecutiveFails++;
        if (this.consecutiveFails >= this.MAX_FAILS_UNSTABLE) {
          this.status.set('unstable');
        }
      }
      return false;
    }
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
    this.reconnected$.complete();
  }
}
