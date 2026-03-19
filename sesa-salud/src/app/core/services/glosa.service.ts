/**
 * S9: Servicio de glosas y recuperación de cartera.
 * Autor: Ing. J Sebastian Vargas S
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface GlosaAdjuntoDto {
  id: number;
  glosaId: number;
  nombreArchivo: string;
  tipo?: string;
  urlOBlob?: string;
  createdAt?: string;
}

export interface GlosaDto {
  id: number;
  facturaId: number;
  numeroFactura?: string;
  motivoRechazo: string;
  estado: string;
  fechaRegistro: string;
  fechaRespuesta?: string;
  observaciones?: string;
  creadoPorId?: number;
  creadoPorNombre?: string;
  createdAt?: string;
  updatedAt?: string;
  adjuntos: GlosaAdjuntoDto[];
}

export interface GlosaRequestDto {
  facturaId: number;
  motivoRechazo: string;
  estado?: string;
  observaciones?: string;
}

export interface RecuperacionCarteraDto {
  totalGlosas: number;
  pendientes: number;
  enviadas: number;
  aceptadas: number;
  rechazadas: number;
  totalRecuperado: number;
  porEstado: { estado: string; cantidad: number }[];
}

@Injectable({ providedIn: 'root' })
export class GlosaService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/glosas`;
  private readonly reportesUrl = `${environment.apiUrl}/reportes`;

  list(params: { facturaId?: number; estado?: string; desde?: string; hasta?: string }): Observable<GlosaDto[]> {
    let httpParams = new HttpParams();
    if (params.facturaId != null) httpParams = httpParams.set('facturaId', params.facturaId);
    if (params.estado) httpParams = httpParams.set('estado', params.estado);
    if (params.desde) httpParams = httpParams.set('desde', params.desde);
    if (params.hasta) httpParams = httpParams.set('hasta', params.hasta);
    return this.http.get<GlosaDto[]>(this.apiUrl, { params: httpParams });
  }

  findByFacturaId(facturaId: number): Observable<GlosaDto[]> {
    return this.http.get<GlosaDto[]>(`${this.apiUrl}/factura/${facturaId}`);
  }

  getById(id: number): Observable<GlosaDto> {
    return this.http.get<GlosaDto>(`${this.apiUrl}/${id}`);
  }

  create(dto: GlosaRequestDto): Observable<GlosaDto> {
    return this.http.post<GlosaDto>(this.apiUrl, dto);
  }

  update(id: number, dto: GlosaRequestDto): Observable<GlosaDto> {
    return this.http.put<GlosaDto>(`${this.apiUrl}/${id}`, dto);
  }

  cambiarEstado(id: number, estado: string): Observable<GlosaDto> {
    return this.http.patch<GlosaDto>(`${this.apiUrl}/${id}/estado`, null, { params: { estado } });
  }

  uploadAdjunto(glosaId: number, file: File): Observable<GlosaDto> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<GlosaDto>(`${this.apiUrl}/${glosaId}/adjuntos`, form);
  }

  recuperacionCartera(desde?: string, hasta?: string, contratoId?: number): Observable<RecuperacionCarteraDto> {
    let httpParams = new HttpParams();
    if (desde) httpParams = httpParams.set('desde', desde);
    if (hasta) httpParams = httpParams.set('hasta', hasta);
    if (contratoId != null) httpParams = httpParams.set('contratoId', contratoId);
    return this.http.get<RecuperacionCarteraDto>(`${this.reportesUrl}/recuperacion-cartera`, { params: httpParams });
  }
}
