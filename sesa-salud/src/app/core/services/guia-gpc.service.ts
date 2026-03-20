/**
 * S15: Servicio de guías de práctica clínica (GPC) por CIE-10.
 * Autor: Ing. J Sebastian Vargas S
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface GuiaGpcSugerenciaDto {
  id: number;
  titulo?: string;
  criteriosControl?: string;
  medicamentosPrimeraLinea?: string;
  estudiosSeguimiento?: string;
  fuente?: string;
}

export interface GuiaGpcRegistroVisualizacionDto {
  atencionId: number;
  codigoCie10: string;
  guiaId: number;
}

@Injectable({ providedIn: 'root' })
export class GuiaGpcService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/guia-gpc`;

  sugerir(codigoCie10: string): Observable<GuiaGpcSugerenciaDto[]> {
    if (!codigoCie10?.trim()) return new Observable((s) => { s.next([]); s.complete(); });
    const params = new HttpParams().set('codigoCie10', codigoCie10.trim());
    return this.http.get<GuiaGpcSugerenciaDto[]>(`${this.apiUrl}/sugerir`, { params });
  }

  registrarVisualizacion(dto: GuiaGpcRegistroVisualizacionDto): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/registrar-visualizacion`, dto);
  }
}
