/**
 * Servicio WebRTC — RTCPeerConnection, getUserMedia y flujo oferta/respuesta.
 * Independiente del canal de señalización (REST, WebSocket, etc.).
 * Autor: Ing. J Sebastian Vargas S
 */

import { Injectable, signal } from '@angular/core';

const ICE_SERVERS: RTCIceServer[] = [
  { urls: 'stun:stun.l.google.com:19302' },
  { urls: 'stun:stun1.l.google.com:19302' },
];

export interface WebrtcPeerState {
  localStream: MediaStream | null;
  remoteStream: MediaStream | null;
  connectionState: RTCPeerConnectionState;
  error: string | null;
}

@Injectable({ providedIn: 'root' })
export class WebrtcPeerService {
  private peerConnection: RTCPeerConnection | null = null;
  private localStream: MediaStream | null = null;
  private remoteStream: MediaStream | null = null;
  private lastIndex = 0;

  readonly state = signal<WebrtcPeerState>({
    localStream: null,
    remoteStream: null,
    connectionState: 'new',
    error: null,
  });

  /** Obtiene cámara y micrófono del usuario. */
  async getLocalStream(video = true, audio = true): Promise<MediaStream> {
    if (this.localStream) return this.localStream;
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: video ? { width: { ideal: 1280 }, height: { ideal: 720 } } : false,
        audio: audio ? { echoCancellation: true } : false,
      });
      this.localStream = stream;
      this.state.update(s => ({ ...s, localStream: stream, error: null }));
      return stream;
    } catch (err: unknown) {
      const message = this.getUserMediaErrorMessage(err);
      this.state.update(s => ({ ...s, error: message }));
      throw err;
    }
  }

  private getUserMediaErrorMessage(err: unknown): string {
    if (err instanceof DOMException) {
      switch (err.name) {
        case 'NotAllowedError':
        case 'PermissionDeniedError':
          return 'Se denegó el acceso a cámara o micrófono. Revisa los permisos del navegador.';
        case 'NotFoundError':
          return 'No se encontró cámara o micrófono. Conecta un dispositivo e inténtalo de nuevo.';
        case 'NotReadableError':
        case 'TrackStartError':
          return 'No se pudo usar la cámara o el micrófono. Cierra otras pestañas o apps que los usen (Zoom, Teams, etc.) y recarga.';
        case 'OverconstrainedError':
          return 'El dispositivo no cumple los requisitos de video/audio. Prueba otra cámara o micrófono.';
        default:
          return err.message || 'No se pudo iniciar la cámara o el micrófono.';
      }
    }
    const msg = err && typeof (err as Error).message === 'string' ? (err as Error).message : '';
    if (/could not start video source|video source/i.test(msg))
      return 'No se pudo iniciar la cámara. Cierra otras aplicaciones que la usen y recarga la página.';
    return msg || 'Error al acceder a cámara o micrófono. Revisa permisos y dispositivos.';
  }

  /** Indica si el stream local tiene algún track ya finalizado (p. ej. al volver a la pestaña). */
  hasEndedTracks(): boolean {
    if (!this.localStream) return false;
    return this.localStream.getTracks().some(t => t.readyState === 'ended');
  }

  /**
   * Re-adquiere cámara/micrófono si los tracks se cerraron (p. ej. al volver a la pestaña).
   * Reemplaza los tracks en la conexión peer si existe. Retorna true si se re-adquirió el stream.
   */
  async reacquireLocalStreamIfNeeded(video = true, audio = true): Promise<boolean> {
    if (!this.hasEndedTracks()) return false;
    const oldStream = this.localStream;
    this.localStream = null;
    oldStream?.getTracks().forEach(t => t.stop());
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: video ? { width: { ideal: 1280 }, height: { ideal: 720 } } : false,
        audio: audio ? { echoCancellation: true } : false,
      });
      this.localStream = stream;
    } catch (err: unknown) {
      this.state.update(s => ({ ...s, error: this.getUserMediaErrorMessage(err) }));
      throw err;
    }
    const stream = this.localStream!;
    if (this.peerConnection && this.peerConnection.connectionState !== 'closed') {
      const senders = this.peerConnection.getSenders();
      const videoTrack = stream.getVideoTracks()[0] ?? null;
      const audioTrack = stream.getAudioTracks()[0] ?? null;
      for (const sender of senders) {
        const kind = sender.track?.kind;
        if (kind === 'video') await sender.replaceTrack(videoTrack);
        else if (kind === 'audio') await sender.replaceTrack(audioTrack);
      }
    }
    this.state.update(s => ({ ...s, localStream: stream }));
    return true;
  }

  /** Solo cierra la conexión peer y el stream remoto; mantiene el stream local (evita pantalla negra). */
  private closeConnectionOnly(): void {
    this.peerConnection?.close();
    this.peerConnection = null;
    this.remoteStream = null;
    this.state.update(s => ({ ...s, remoteStream: null, connectionState: 'new' }));
  }

  /** Crea la conexión peer como creador (genera oferta). No detiene el stream local. */
  async createPeerConnection(
    onOffer: (offer: RTCSessionDescriptionInit) => Promise<void>,
    onIceCandidate: (candidate: RTCIceCandidateInit) => Promise<void>
  ): Promise<void> {
    this.closeConnectionOnly();
    this.peerConnection = new RTCPeerConnection({ iceServers: ICE_SERVERS });
    await this.setupPeerConnection(onIceCandidate);
    if (this.localStream) {
      this.localStream.getTracks().forEach(track => this.peerConnection!.addTrack(track, this.localStream!));
    }
    const offer = await this.peerConnection.createOffer();
    await this.peerConnection.setLocalDescription(offer);
    await onOffer(offer);
    this.updateConnectionState();
  }

  /** Añade la oferta remota y crea la respuesta (participante). No detiene el stream local. */
  async setOfferAndCreateAnswer(
    offer: RTCSessionDescriptionInit,
    onAnswer: (answer: RTCSessionDescriptionInit) => Promise<void>,
    onIceCandidate: (candidate: RTCIceCandidateInit) => Promise<void>
  ): Promise<void> {
    this.closeConnectionOnly();
    this.peerConnection = new RTCPeerConnection({ iceServers: ICE_SERVERS });
    await this.setupPeerConnection(onIceCandidate);
    if (this.localStream) {
      this.localStream.getTracks().forEach(track => this.peerConnection!.addTrack(track, this.localStream!));
    }
    await this.peerConnection.setRemoteDescription(new RTCSessionDescription(offer));
    const answer = await this.peerConnection.createAnswer();
    await this.peerConnection.setLocalDescription(answer);
    await onAnswer(answer);
    this.updateConnectionState();
  }

  /** Creador: recibe la respuesta del participante. */
  async setAnswer(answer: RTCSessionDescriptionInit): Promise<void> {
    if (!this.peerConnection) return;
    await this.peerConnection.setRemoteDescription(new RTCSessionDescription(answer));
    this.updateConnectionState();
  }

  /** Añade candidato ICE remoto. */
  async addIceCandidate(candidate: RTCIceCandidateInit): Promise<void> {
    if (!this.peerConnection) return;
    try {
      await this.peerConnection.addIceCandidate(new RTCIceCandidate(candidate));
    } catch (e) {
      console.warn('addIceCandidate error', e);
    }
  }

  private setupPeerConnection(onIceCandidate: (c: RTCIceCandidateInit) => Promise<void>): void {
    if (!this.peerConnection) return;
    this.peerConnection.ontrack = (e) => {
      if (e.streams?.[0]) {
        this.remoteStream = e.streams[0];
        this.state.update(s => ({ ...s, remoteStream: this.remoteStream }));
      }
    };
    this.peerConnection.onicecandidate = (e) => {
      if (e.candidate) onIceCandidate(e.candidate.toJSON());
    };
    this.peerConnection.onconnectionstatechange = () => this.updateConnectionState();
    this.peerConnection.oniceconnectionstatechange = () => this.updateConnectionState();
  }

  private updateConnectionState(): void {
    const conn = this.peerConnection?.connectionState ?? 'new';
    this.state.update(s => ({ ...s, connectionState: conn, error: conn === 'failed' ? 'Error de conexión' : null }));
  }

  /** Cierra streams y conexión. */
  reset(): void {
    this.peerConnection?.close();
    this.peerConnection = null;
    this.localStream?.getTracks().forEach(t => t.stop());
    this.localStream = null;
    this.remoteStream = null;
    this.state.set({
      localStream: null,
      remoteStream: null,
      connectionState: 'new',
      error: null,
    });
  }

  /** Activa/desactiva video local. */
  setVideoEnabled(enabled: boolean): void {
    this.localStream?.getVideoTracks().forEach(t => { t.enabled = enabled; });
  }

  /** Activa/desactiva audio local. */
  setAudioEnabled(enabled: boolean): void {
    this.localStream?.getAudioTracks().forEach(t => { t.enabled = enabled; });
  }
}
