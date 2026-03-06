import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AdjuntoInfo {
  id: number;
  nombreArchivo: string;
  contentType?: string;
  tamano?: number;
}

export interface DestinatarioInfo {
  usuarioId: number;
  usuarioEmail?: string;
  usuarioNombre?: string;
  leido: boolean;
  fechaLectura?: string;
}

export interface NotificacionDto {
  id: number;
  titulo: string;
  contenido: string;
  tipo: string;
  remitenteId: number;
  remitenteNombre?: string;
  fechaEnvio: string;
  adjuntos?: AdjuntoInfo[];
  destinatarios?: DestinatarioInfo[];
}

export interface DestinatarioDisponible {
  id: number;
  nombre?: string;
  email: string;
  rol?: string;
}

export interface NotificacionBroadcastResult {
  schemasProcessados: number;
  totalDestinatarios: number;
  errores: string[];
}

export interface NotificacionCreateRequest {
  titulo: string;
  contenido: string;
  tipo?: string;
  destinatarioIds?: number[];
  broadcastTodos?: boolean;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({ providedIn: 'root' })
export class NotificacionService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/notificaciones`;

  /** Crear notificación */
  create(request: NotificacionCreateRequest): Observable<NotificacionDto> {
    return this.http.post<NotificacionDto>(this.apiUrl, request);
  }

  /** Subir adjunto a una notificación */
  uploadAdjunto(notificacionId: number, file: File): Observable<void> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<void>(`${this.apiUrl}/${notificacionId}/adjuntos`, form);
  }

  /** Obtener notificación por ID */
  get(id: number): Observable<NotificacionDto> {
    return this.http.get<NotificacionDto>(`${this.apiUrl}/${id}`);
  }

  /** Listar notificaciones enviadas (paginado) */
  listEnviadas(page = 0, size = 20): Observable<PageResponse<NotificacionDto>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<NotificacionDto>>(`${this.apiUrl}/enviadas`, { params });
  }

  /** Listar notificaciones recibidas (paginado) */
  listRecibidas(page = 0, size = 20): Observable<PageResponse<NotificacionDto>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<NotificacionDto>>(`${this.apiUrl}/recibidas`, { params });
  }

  /** Contar no leídas */
  countNoLeidas(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/recibidas/count`);
  }

  /** Marcar notificación como leída */
  marcarLeida(id: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/leer`, {});
  }

  /** Marcar varias notificaciones como leídas (solo donde el usuario es destinatario) */
  marcarLeidas(ids: number[]): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/marcar-leidas`, { notificacionIds: ids });
  }

  /** Marcar una notificación como no leída (solo destinatario) */
  marcarNoLeida(id: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/no-leer`, {});
  }

  /** Marcar varias notificaciones como no leídas (solo donde el usuario es destinatario) */
  marcarNoLeidas(ids: number[]): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/marcar-no-leidas`, { notificacionIds: ids });
  }

  /** Eliminar un adjunto de una notificación (solo el remitente puede) */
  deleteAdjunto(notificacionId: number, adjuntoId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${notificacionId}/adjuntos/${adjuntoId}`);
  }

  /** URL de descarga de adjunto */
  adjuntoUrl(notificacionId: number, adjuntoId: number): string {
    return `${this.apiUrl}/${notificacionId}/adjuntos/${adjuntoId}`;
  }

  /** Descargar adjunto como blob */
  downloadAdjunto(notificacionId: number, adjuntoId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${notificacionId}/adjuntos/${adjuntoId}`, { responseType: 'blob' });
  }

  /** Lista usuarios disponibles como destinatarios en el schema actual */
  getDestinatariosDisponibles(): Observable<DestinatarioDisponible[]> {
    return this.http.get<DestinatarioDisponible[]>(`${this.apiUrl}/destinatarios-disponibles`);
  }

  /** SUPERADMINISTRADOR: enviar notificación a los admins de todos los schemas */
  broadcastAdmins(request: NotificacionCreateRequest): Observable<NotificacionBroadcastResult> {
    return this.http.post<NotificacionBroadcastResult>(`${this.apiUrl}/broadcast-admins`, request);
  }
}
