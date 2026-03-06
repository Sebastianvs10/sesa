/**
 * Sala de Videoconsulta — WebRTC integrado con señalización REST.
 * Ruta: /videoconsulta?room=XXX (unirse) o sin room (crear desde consulta).
 * Autor: Ing. J Sebastian Vargas S
 */

import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  OnInit,
  OnDestroy,
  computed,
  HostListener,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { interval, Subscription, firstValueFrom } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { VideoconsultaService, SignalingEventDto } from '../../core/services/videoconsulta.service';
import { WebrtcPeerService } from '../../core/services/webrtc-peer.service';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';

@Component({
  selector: 'sesa-videoconsulta-sala',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  template: `
    @if (pendingJoin()) {
      <div class="vc-sala vc-sala--prejoin">
        <div class="vc-sala__prejoin-card">
          <div class="vc-sala__prejoin-icon">
            <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/>
            </svg>
          </div>
          <h2 class="vc-sala__prejoin-title">Videoconsulta</h2>
          <p class="vc-sala__prejoin-text">¿Desea unirse a esta videoconsulta? Se activará su cámara y micrófono.</p>
          <div class="vc-sala__prejoin-toggles">
            <label class="vc-sala__prejoin-toggle">
              <input
                #videoToggle
                type="checkbox"
                [checked]="videoOn()"
                (change)="setVideoOn(videoToggle.checked)"
              />
              Activar cámara
            </label>
            <label class="vc-sala__prejoin-toggle">
              <input
                #audioToggle
                type="checkbox"
                [checked]="audioOn()"
                (change)="setAudioOn(audioToggle.checked)"
              />
              Activar micrófono
            </label>
          </div>
          <div class="vc-sala__prejoin-actions">
            <button type="button" class="vc-sala__prejoin-btn vc-sala__prejoin-btn--primary" (click)="confirmarUnirse()">
              Unirse
            </button>
            <button type="button" class="vc-sala__prejoin-btn vc-sala__prejoin-btn--secondary" (click)="cancelarUnirse()">
              Cancelar
            </button>
          </div>
        </div>
      </div>
    } @else {
    <div class="vc-sala">
      <header class="vc-sala__header">
        <div class="vc-sala__brand">
          <span class="vc-sala__logo">SESA</span>
          <span class="vc-sala__title">Videoconsulta</span>
        </div>
        <div class="vc-sala__status" [class.vc-sala__status--ok]="webrtc.state().connectionState === 'connected'">
          <span class="vc-sala__dot"></span>
          {{ estadoTexto() }}
        </div>
        <button class="vc-sala__btn-end" (click)="colgar()" title="Finalizar llamada">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/>
          </svg>
          Colgar
        </button>
      </header>

      <main class="vc-sala__main">
        <!-- Remoto (paciente o médico) -->
        <div class="vc-sala__remote">
          @if (webrtc.state().remoteStream) {
            <video
              class="vc-sala__video vc-sala__video--remote"
              autoplay
              playsinline
              [srcObject]="webrtc.state().remoteStream"
            ></video>
          } @else {
            <div class="vc-sala__placeholder">
              <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
              </svg>
              <p>Esperando al otro participante…</p>
            </div>
          }
        </div>

        <!-- Local (tu cámara) -->
        <div class="vc-sala__local">
          @if (webrtc.state().localStream) {
            <video
              class="vc-sala__video vc-sala__video--local"
              autoplay
              playsinline
              muted
              [srcObject]="webrtc.state().localStream"
            ></video>
            <div class="vc-sala__local-actions">
              <button
                class="vc-sala__action"
                [class.vc-sala__action--off]="!videoOn()"
                (click)="toggleVideo()"
                title="{{ videoOn() ? 'Apagar cámara' : 'Encender cámara' }}"
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M23 7l-7 5 7 5V7z"/><rect x="1" y="5" width="15" height="14" rx="2" ry="2"/>
                </svg>
              </button>
              <button
                class="vc-sala__action"
                [class.vc-sala__action--off]="!audioOn()"
                (click)="toggleAudio()"
                title="{{ audioOn() ? 'Silenciar' : 'Activar micrófono' }}"
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
                  <line x1="12" y1="19" x2="12" y2="23"/><line x1="8" y1="23" x2="16" y2="23"/>
                </svg>
              </button>
            </div>
          }
        </div>
      </main>

      <!-- Panel crear sala: mostrar enlace para compartir -->
      @if (esCreador() && linkCompartir() && webrtc.state().connectionState !== 'connected') {
        <div class="vc-sala__share">
          <p class="vc-sala__share-label">Comparte este enlace con el paciente para que se una:</p>
          <div class="vc-sala__share-row">
            <input type="text" class="vc-sala__share-input" [value]="linkCompartir()" readonly />
            <button class="vc-sala__btn-copy" (click)="copiarEnlace()">Copiar</button>
          </div>
          @if (!linkAsistente()) {
            <button type="button" class="vc-sala__btn-asistente" (click)="habilitarAsistente()">Incluir asistente (toma de notas)</button>
          } @else {
            <p class="vc-sala__share-label">Enlace para el asistente (solo notas; el paciente debe autorizar primero):</p>
            <div class="vc-sala__share-row">
              <input type="text" class="vc-sala__share-input" [value]="linkAsistente()" readonly />
              <button class="vc-sala__btn-copy" (click)="copiarEnlaceAsistente()">Copiar</button>
            </div>
          }
        </div>
      }

      <!-- Paciente: consentimiento para asistente -->
      @if (esPaciente() && consentimientoAsistentePendiente()) {
        <div class="vc-sala__consent-overlay">
          <div class="vc-sala__consent-card">
            <p class="vc-sala__consent-text">El profesional solicita que un asistente tome notas de esta videoconsulta. ¿Autoriza?</p>
            <div class="vc-sala__consent-actions">
              <button type="button" class="vc-sala__prejoin-btn vc-sala__prejoin-btn--primary" (click)="aceptarConsentimientoAsistente()">Sí, autorizo</button>
              <button type="button" class="vc-sala__prejoin-btn vc-sala__prejoin-btn--secondary" (click)="rechazarConsentimientoAsistente()">No</button>
            </div>
          </div>
        </div>
      }

      <!-- Modal resumen para el profesional al colgar -->
      @if (showResumenModal()) {
        <div class="vc-sala__resumen-overlay" (click)="cerrarResumen()">
          <div class="vc-sala__resumen-card" (click)="$event.stopPropagation()">
            <h3 class="vc-sala__resumen-title">Resumen de la reunión (notas del asistente)</h3>
            <div class="vc-sala__resumen-texto">{{ resumenTexto() }}</div>
            <button type="button" class="vc-sala__prejoin-btn vc-sala__prejoin-btn--primary" (click)="cerrarResumen()">Cerrar</button>
          </div>
        </div>
      }

      <!-- Error -->
      @if (webrtc.state().error) {
        <div class="vc-sala__error">{{ webrtc.state().error }}</div>
      }
    </div>
    }
  `,
  styleUrl: './videoconsulta-sala.page.scss',
})
export class VideoconsultaSalaPageComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toast = inject(SesaToastService);
  readonly videoconsulta = inject(VideoconsultaService);
  readonly webrtc = inject(WebrtcPeerService);

  /** Sala actual (se guarda al crear para usar en habilitar asistente). */
  private salaIdActual: string | null = null;

  readonly videoOn = signal(true);
  readonly audioOn = signal(true);
  readonly linkCompartir = signal<string | null>(null);
  readonly linkAsistente = signal<string | null>(null);
  readonly consentimientoAsistentePendiente = signal(false);
  readonly showResumenModal = signal(false);
  readonly resumenTexto = signal('');
  /** True cuando el paciente abrió el enlace y aún no ha confirmado si desea unirse. */
  readonly pendingJoin = signal(false);
  private pendingRoom: string | null = null;
  private pendingToken: string | null = null;
  private pollingSub: Subscription | null = null;
  private consentimientoPollSub: Subscription | null = null;
  /** Token de sala: se guarda al crear (respuesta API) o al unirse (query param) y se pasa a todas las llamadas de signaling. */
  private tokenSala: string | null = null;

  readonly estadoTexto = computed(() => {
    const s = this.webrtc.state().connectionState;
    const map: Record<string, string> = {
      new: 'Preparando…',
      connecting: 'Conectando…',
      connected: 'Conectado',
      disconnected: 'Desconectado',
      failed: 'Error de conexión',
      closed: 'Llamada finalizada',
    };
    return map[s] ?? s;
  });

  esCreador(): boolean {
    return this.videoconsulta.role() === 'creador';
  }

  esPaciente(): boolean {
    return this.videoconsulta.role() === 'participante';
  }

  setVideoOn(value: boolean): void {
    this.videoOn.set(value);
  }

  setAudioOn(value: boolean): void {
    this.audioOn.set(value);
  }

  ngOnInit(): void {
    const room = this.route.snapshot.queryParamMap.get('room');
    const token = this.route.snapshot.queryParamMap.get('token');
    if (room && token) {
      this.pendingRoom = room;
      this.pendingToken = token;
      this.pendingJoin.set(true);
    } else if (!room) {
      const citaId = this.route.snapshot.queryParamMap.get('citaId');
      this.crearSala(citaId ? +citaId : undefined);
    } else {
      this.webrtc.state.update(s => ({ ...s, error: 'Enlace inválido: falta el token de la videoconsulta.' }));
    }
  }

  /** El paciente confirmó que desea unirse: unir a la sala y activar cámara/micrófono. */
  async confirmarUnirse(): Promise<void> {
    const room = this.pendingRoom;
    const token = this.pendingToken;
    this.pendingJoin.set(false);
    this.pendingRoom = null;
    this.pendingToken = null;
    if (room && token) await this.unirseSala(room, token);
  }

  /** El paciente canceló: salir sin unirse. */
  cancelarUnirse(): void {
    this.pendingJoin.set(false);
    this.pendingRoom = null;
    this.pendingToken = null;
    this.router.navigate(['/login']);
  }

  private iniciarPollConsentimientoAsistente(): void {
    const salaId = this.videoconsulta.salaId();
    const token = this.tokenSala;
    if (!salaId || !token) return;
    this.consentimientoPollSub = interval(4000).pipe(
      switchMap(() => this.videoconsulta.solicitudAsistentePendiente(salaId, token))
    ).subscribe({
      next: (res) => this.consentimientoAsistentePendiente.set(res.pendiente),
    });
  }

  async habilitarAsistente(): Promise<void> {
    const salaId = this.salaIdActual ?? this.videoconsulta.salaId();
    const token = this.tokenSala;
    if (!salaId || !token) {
      this.toast.warning('No se pudo obtener la sala. Espera a que la sala esté lista e inténtalo de nuevo.', 'Incluir asistente');
      return;
    }
    try {
      const res = await firstValueFrom(this.videoconsulta.habilitarAsistente(salaId, token));
      this.linkAsistente.set(this.videoconsulta.getUrlAsistente(salaId, res.tokenAsistente));
      this.toast.success('Enlace del asistente listo. Cópialo y compártelo. El asistente podrá tomar notas cuando el paciente autorice.', 'Asistente');
    } catch (e: any) {
      const msg = e?.error?.message ?? e?.message ?? 'No se pudo habilitar el asistente.';
      this.toast.error(msg, 'Incluir asistente');
    }
  }

  copiarEnlaceAsistente(): void {
    const link = this.linkAsistente();
    if (link && navigator.clipboard) navigator.clipboard.writeText(link);
  }

  aceptarConsentimientoAsistente(): void {
    const salaId = this.videoconsulta.salaId();
    const token = this.tokenSala;
    if (!salaId || !token) return;
    this.videoconsulta.registrarConsentimientoAsistente(salaId, token).subscribe({
      next: () => this.consentimientoAsistentePendiente.set(false),
      error: () => {},
    });
  }

  rechazarConsentimientoAsistente(): void {
    this.consentimientoAsistentePendiente.set(false);
  }

  cerrarResumen(): void {
    this.showResumenModal.set(false);
    this.videoconsulta.limpiarSala();
    this.tokenSala = null;
    this.router.navigate(['/consulta-medica']);
  }

  /** Al volver a la pestaña, re-adquirir cámara/micrófono si el navegador los cerró. */
  @HostListener('document:visibilitychange')
  onVisibilityChange(): void {
    if (document.visibilityState !== 'visible') return;
    if (this.pendingJoin()) return;
    this.webrtc.reacquireLocalStreamIfNeeded(this.videoOn(), this.audioOn()).catch(() => {});
  }

  ngOnDestroy(): void {
    this.stopPolling();
    this.consentimientoPollSub?.unsubscribe();
    this.consentimientoPollSub = null;
    this.webrtc.reset();
    this.videoconsulta.limpiarSala();
    this.tokenSala = null;
    this.salaIdActual = null;
  }

  private async crearSala(citaId?: number): Promise<void> {
    try {
      await this.webrtc.getLocalStream();
      const s = await firstValueFrom(this.videoconsulta.crearSala(citaId));
      const salaId = s.salaId;
      this.salaIdActual = salaId;
      this.tokenSala = s.token ?? null;
      this.linkCompartir.set(this.videoconsulta.getUrlUnirse(salaId, this.tokenSala ?? ''));

      await this.webrtc.createPeerConnection(
        async (offer) => {
          await firstValueFrom(this.videoconsulta.enviarSignaling(salaId, { type: 'offer', payload: JSON.stringify(offer) }, this.tokenSala));
        },
        async (candidate) => {
          await firstValueFrom(this.videoconsulta.enviarSignaling(salaId, { type: 'ice', payload: JSON.stringify(candidate) }, this.tokenSala));
        }
      );
      this.startPolling(salaId);
    } catch (e: any) {
      this.webrtc.state.update(s => ({ ...s, error: e?.message || 'No se pudo crear la sala' }));
    }
  }

  private async unirseSala(salaId: string, token: string): Promise<void> {
    try {
      this.tokenSala = token;
      await firstValueFrom(this.videoconsulta.unirseSala(salaId, token));
      await this.webrtc.getLocalStream(this.videoOn(), this.audioOn());
      this.startPolling(salaId);
      this.iniciarPollConsentimientoAsistente();
    } catch (e: any) {
      const msg = e?.error?.message ?? e?.message ?? 'No se pudo unir a la sala';
      this.webrtc.state.update(s => ({ ...s, error: msg }));
    }
  }

  private lastIndex = 0;
  private offerProcessed = false;

  private startPolling(salaId: string): void {
    const role = this.videoconsulta.role();
    const token = this.tokenSala;
    this.pollingSub = interval(1500).pipe(
      switchMap(() => this.videoconsulta.obtenerSignaling(salaId, this.lastIndex, token))
    ).subscribe({
      next: async (res) => {
        for (const ev of (res.events || []) as Array<SignalingEventDto & { index: number }>) {
          this.lastIndex = Math.max(this.lastIndex, ev.index + 1);
          try {
            const payload = JSON.parse(ev.payload);
            if (ev.type === 'offer' && role === 'participante' && !this.offerProcessed) {
              this.offerProcessed = true;
              await this.webrtc.setOfferAndCreateAnswer(
                payload,
                async (answer) => {
                  await firstValueFrom(this.videoconsulta.enviarSignaling(salaId, { type: 'answer', payload: JSON.stringify(answer) }, this.tokenSala));
                },
                async (candidate) => {
                  await firstValueFrom(this.videoconsulta.enviarSignaling(salaId, { type: 'ice', payload: JSON.stringify(candidate) }, this.tokenSala));
                }
              );
            } else if (ev.type === 'answer' && role === 'creador') {
              await this.webrtc.setAnswer(payload);
            } else if (ev.type === 'ice') {
              await this.webrtc.addIceCandidate(payload);
            }
          } catch (err) {
            console.warn('Signaling event error', err);
          }
        }
      },
      error: (err) => {
        const msg = err?.error?.message ?? err?.message ?? 'Error de señalización con el servidor.';
        this.webrtc.state.update(s => ({ ...s, error: msg }));
        this.stopPolling();
      },
    });
  }

  private stopPolling(): void {
    this.pollingSub?.unsubscribe();
    this.pollingSub = null;
  }

  toggleVideo(): void {
    this.videoOn.update(v => !v);
    this.webrtc.setVideoEnabled(this.videoOn());
  }

  toggleAudio(): void {
    this.audioOn.update(a => !a);
    this.webrtc.setAudioEnabled(this.audioOn());
  }

  copiarEnlace(): void {
    const link = this.linkCompartir();
    if (link && navigator.clipboard) {
      navigator.clipboard.writeText(link);
    }
  }

  colgar(): void {
    this.stopPolling();
    this.consentimientoPollSub?.unsubscribe();
    this.consentimientoPollSub = null;
    this.webrtc.reset();
    const salaId = this.salaIdActual ?? this.videoconsulta.salaId();
    const token = this.tokenSala;
    if (this.esCreador() && salaId && token) {
      this.videoconsulta.obtenerNotas(salaId, token).subscribe({
        next: (r) => {
          this.resumenTexto.set(r.notas ?? '');
          this.showResumenModal.set(true);
          this.videoconsulta.limpiarSala();
          this.tokenSala = null;
          this.salaIdActual = null;
        },
        error: () => {
          this.videoconsulta.limpiarSala();
          this.tokenSala = null;
          this.salaIdActual = null;
          this.router.navigate(['/consulta-medica']);
        },
      });
    } else {
      this.videoconsulta.limpiarSala();
      this.tokenSala = null;
      this.salaIdActual = null;
      this.router.navigate(['/consulta-medica']);
    }
  }
}
