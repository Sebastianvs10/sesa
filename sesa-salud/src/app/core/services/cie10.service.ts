/**
 * S8: Servicio de sugerencias CIE-10 (motivo de consulta y texto de análisis).
 * Autor: Ing. J Sebastian Vargas S
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Cie10SugerenciaDto {
  codigo: string;
  descripcion: string;
  relevancia: number;
}

@Injectable({ providedIn: 'root' })
export class Cie10Service {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/cie10`;

  sugerirCie10(motivo: string, texto: string): Observable<Cie10SugerenciaDto[]> {
    const params = new HttpParams()
      .set('motivo', motivo ?? '')
      .set('texto', texto ?? '');
    return this.http.get<Cie10SugerenciaDto[]>(`${this.apiUrl}/sugerir`, { params });
  }
}
