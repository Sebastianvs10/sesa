/**
 * Servicio de cierre de sesión automático por inactividad.
 * Autor: Ing. J Sebastian Vargas S
 */

import { Injectable, inject, NgZone, OnDestroy } from '@angular/core';
import { AuthService } from './auth.service';

/** Tiempo sin actividad antes del cierre de sesión automático (15 minutos). */
const IDLE_TIMEOUT_MS = 15 * 60 * 1000;

/** Eventos del DOM que se consideran "actividad del usuario". */
const ACTIVITY_EVENTS = [
  'mousemove', 'mousedown', 'keydown',
  'touchstart', 'scroll', 'click',
] as const;

/**
 * Detecta inactividad del usuario y cierra la sesión automáticamente
 * tras 15 minutos sin interacción.
 *
 * Uso en AppComponent:
 *   - Llamar a `start()` cuando el usuario se autentica.
 *   - Llamar a `stop()` en ngOnDestroy o al hacer logout manual.
 */
@Injectable({ providedIn: 'root' })
export class IdleTimeoutService implements OnDestroy {

  private readonly auth = inject(AuthService);
  private readonly zone = inject(NgZone);

  private idleTimer?: ReturnType<typeof setTimeout>;
  private running = false;

  /** Referencia estable al handler (necesaria para removeEventListener). */
  private readonly onActivity = (): void => this.resetTimer();

  /** Activa la vigilancia de inactividad. Idempotente: llamadas múltiples no duplican listeners. */
  start(): void {
    if (this.running) return;
    this.running = true;
    // Los event listeners van fuera de la zona Angular para no disparar change detection en cada movimiento
    this.zone.runOutsideAngular(() => {
      ACTIVITY_EVENTS.forEach(evt =>
        document.addEventListener(evt, this.onActivity, { passive: true })
      );
    });
    this.resetTimer();
  }

  /** Desactiva la vigilancia. Se llama al hacer logout o al destruir el componente raíz. */
  stop(): void {
    if (!this.running) return;
    this.running = false;
    ACTIVITY_EVENTS.forEach(evt =>
      document.removeEventListener(evt, this.onActivity)
    );
    this.clearTimer();
  }

  ngOnDestroy(): void {
    this.stop();
  }

  private resetTimer(): void {
    this.clearTimer();
    this.idleTimer = setTimeout(() => {
      // Volver a la zona Angular para que el logout actualice las señales y la UI
      this.zone.run(() => {
        if (this.auth.isAuthenticated()) {
          this.stop();
          this.auth.logout();
        }
      });
    }, IDLE_TIMEOUT_MS);
  }

  private clearTimer(): void {
    if (this.idleTimer !== undefined) {
      clearTimeout(this.idleTimer);
      this.idleTimer = undefined;
    }
  }
}
