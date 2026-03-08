/**
 * Servicio de Evoluciones médicas — registro de evolución clínica en urgencias/consultas.
 * Autor: Ing. J Sebastian Vargas S
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface EvolucionDto {
  id: number;
  atencionId: number;
  notaEvolucion: string;
  fecha: string;
  profesionalId?: number;
  profesionalNombre?: string;
  createdAt?: string;
}

export interface EvolucionRequestDto {
  atencionId: number;
  notaEvolucion: string;
  fecha?: string;
  profesionalId?: number;
}

@Injectable({ providedIn: 'root' })
export class EvolucionService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/evoluciones`;

  listarPorAtencion(atencionId: number): Observable<EvolucionDto[]> {
    const params = new HttpParams()
      .set('atencionId', atencionId.toString())
      .set('size', '100');
    return this.http.get<EvolucionDto[]>(this.apiUrl, { params });
  }

  /** Evoluciones del paciente (urgencias y otras atenciones) para timeline en Historia Clínica. */
  listarPorPaciente(pacienteId: number): Observable<EvolucionDto[]> {
    return this.http.get<EvolucionDto[]>(`${this.apiUrl}/paciente/${pacienteId}`, {
      params: new HttpParams().set('size', '100')
    });
  }

  crear(dto: EvolucionRequestDto): Observable<EvolucionDto> {
    return this.http.post<EvolucionDto>(this.apiUrl, dto);
  }

  actualizar(id: number, dto: Partial<EvolucionRequestDto>): Observable<EvolucionDto> {
    return this.http.put<EvolucionDto>(`${this.apiUrl}/${id}`, dto);
  }
}
