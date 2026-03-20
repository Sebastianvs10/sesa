/**
 * Catálogo CUPS (Colombia) para facturación y órdenes clínicas.
 * Autor: Ing. J Sebastian Vargas S
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface CupCatalogoDto {
  id: number;
  codigo: string;
  descripcion: string;
  capitulo?: string;
  tipoServicio?: string;
  precioSugerido?: number;
}

@Injectable({ providedIn: 'root' })
export class CupsCatalogoService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/cups-catalogo`;

  /** Lista todos los CUPS activos del tenant. */
  listar(): Observable<CupCatalogoDto[]> {
    return this.http.get<CupCatalogoDto[]>(this.base);
  }

  /** Busca por código o descripción (máx. 200 resultados). */
  buscar(q: string, limit = 100): Observable<CupCatalogoDto[]> {
    const params = new HttpParams()
      .set('q', q ?? '')
      .set('limit', String(limit));
    return this.http.get<CupCatalogoDto[]>(`${this.base}/search`, { params });
  }

  /** Obtiene un CUPS por código. */
  porCodigo(codigo: string): Observable<CupCatalogoDto | null> {
    return this.http.get<CupCatalogoDto>(`${this.base}/codigo/${encodeURIComponent(codigo)}`);
  }
}
