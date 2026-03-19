/**
 * Servicio de radicación de facturas ante EPS.
 * Autor: Ing. J Sebastian Vargas S
 */
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface RadicacionDto {
  id: number;
  facturaId: number;
  numeroFactura?: string;
  fechaRadicacion: string;
  numeroRadicado?: string;
  epsCodigo?: string;
  epsNombre?: string;
  estado: string;
  cuv?: string;
  observaciones?: string;
  createdAt?: string;
}

export interface RadicacionRequestDto {
  facturaId: number;
  fechaRadicacion?: string;
  numeroRadicado?: string;
  epsCodigo?: string;
  epsNombre?: string;
  estado?: string;
  cuv?: string;
  observaciones?: string;
}

export interface PageResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class RadicacionService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/radicaciones`;

  list(filters: { estado?: string; desde?: string; hasta?: string; facturaId?: number; page?: number; size?: number } = {}): Observable<PageResult<RadicacionDto>> {
    let params = new HttpParams()
      .set('page', filters.page ?? 0)
      .set('size', filters.size ?? 20);
    if (filters.estado) params = params.set('estado', filters.estado);
    if (filters.desde) params = params.set('desde', filters.desde);
    if (filters.hasta) params = params.set('hasta', filters.hasta);
    if (filters.facturaId) params = params.set('facturaId', filters.facturaId);
    return this.http.get<PageResult<RadicacionDto>>(this.apiUrl, { params });
  }

  listByFactura(facturaId: number): Observable<RadicacionDto[]> {
    return this.http.get<RadicacionDto[]>(`${this.apiUrl}/factura/${facturaId}`);
  }

  getById(id: number): Observable<RadicacionDto> {
    return this.http.get<RadicacionDto>(`${this.apiUrl}/${id}`);
  }

  create(dto: RadicacionRequestDto): Observable<RadicacionDto> {
    return this.http.post<RadicacionDto>(this.apiUrl, dto);
  }

  update(id: number, dto: RadicacionRequestDto): Observable<RadicacionDto> {
    return this.http.put<RadicacionDto>(`${this.apiUrl}/${id}`, dto);
  }
}
