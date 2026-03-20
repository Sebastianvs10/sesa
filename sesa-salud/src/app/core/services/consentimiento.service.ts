/**
 * Servicio HTTP para gestión de Consentimientos Informados (Ley 23/1981).
 * Autor: Ing. J Sebastian Vargas S
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ConsentimientoInformadoDto {
  id?: number;
  pacienteId: number;
  pacienteNombre?: string;
  pacienteDocumento?: string;
  profesionalId: number;
  profesionalNombre?: string;
  tipo: string;
  estado?: string;
  procedimiento?: string;
  fechaSolicitud?: string;
  fechaFirma?: string;
  observaciones?: string;
  firmaCanvasData?: string;
  createdAt?: string;
  updatedAt?: string;
}

@Injectable({ providedIn: 'root' })
export class ConsentimientoService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/consentimientos`;

  listByPaciente(pacienteId: number): Observable<ConsentimientoInformadoDto[]> {
    return this.http.get<ConsentimientoInformadoDto[]>(`${this.apiUrl}/paciente/${pacienteId}`);
  }

  create(dto: ConsentimientoInformadoDto): Observable<ConsentimientoInformadoDto> {
    return this.http.post<ConsentimientoInformadoDto>(this.apiUrl, dto);
  }

  firmar(id: number, firmaCanvasData?: string, observaciones?: string): Observable<ConsentimientoInformadoDto> {
    return this.http.patch<ConsentimientoInformadoDto>(`${this.apiUrl}/${id}/firmar`, { firmaCanvasData, observaciones });
  }

  rechazar(id: number, observaciones?: string): Observable<ConsentimientoInformadoDto> {
    return this.http.patch<ConsentimientoInformadoDto>(`${this.apiUrl}/${id}/rechazar`, { observaciones });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
