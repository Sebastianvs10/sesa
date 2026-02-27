/**
 * Servicio de Urgencias — registro de pacientes, cambios de estado.
 * Autor: Ing. J Sebastian Vargas S
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface UrgenciaRegistroDto {
  id: number;
  pacienteId: number;
  pacienteNombre: string;
  pacienteDocumento?: string;
  nivelTriage: string;
  estado: string;
  fechaHoraIngreso: string;
  observaciones?: string;
  atencionId?: number;
  createdAt?: string;
  // Campos normativos Res. 5596/2015
  tipoLlegada?: string;
  motivoConsulta?: string;
  profesionalTriageId?: number;
  profesionalTriageNombre?: string;
  // Signos vitales al ingreso
  svPresionArterial?: string;
  svFrecuenciaCardiaca?: string;
  svFrecuenciaRespiratoria?: string;
  svTemperatura?: string;
  svSaturacionO2?: string;
  svPeso?: string;
  svDolorEva?: string;
  // Escala de Glasgow
  glasgowOcular?: number;
  glasgowVerbal?: number;
  glasgowMotor?: number;
}

export interface UrgenciaRegistroRequestDto {
  pacienteId: number;
  nivelTriage?: string;
  estado?: string;
  observaciones?: string;
  // Campos normativos Res. 5596/2015
  tipoLlegada?: string;
  motivoConsulta?: string;
  profesionalTriageId?: number;
  svPresionArterial?: string;
  svFrecuenciaCardiaca?: string;
  svFrecuenciaRespiratoria?: string;
  svTemperatura?: string;
  svSaturacionO2?: string;
  svPeso?: string;
  svDolorEva?: string;
  glasgowOcular?: number;
  glasgowVerbal?: number;
  glasgowMotor?: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements?: number;
  totalPages?: number;
}

@Injectable({ providedIn: 'root' })
export class UrgenciaRegistroService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/urgencias`;

  list(estado?: string): Observable<PageResponse<UrgenciaRegistroDto>> {
    let params = new HttpParams().set('page', '0').set('size', '100');
    if (estado?.trim()) params = params.set('estado', estado.trim());
    return this.http.get<PageResponse<UrgenciaRegistroDto>>(this.apiUrl, { params });
  }

  getById(id: number): Observable<UrgenciaRegistroDto> {
    return this.http.get<UrgenciaRegistroDto>(`${this.apiUrl}/${id}`);
  }

  crear(dto: UrgenciaRegistroRequestDto): Observable<UrgenciaRegistroDto> {
    return this.http.post<UrgenciaRegistroDto>(this.apiUrl, dto);
  }

  actualizar(id: number, dto: Partial<UrgenciaRegistroRequestDto>): Observable<UrgenciaRegistroDto> {
    return this.http.put<UrgenciaRegistroDto>(`${this.apiUrl}/${id}`, dto);
  }

  cambiarEstado(id: number, estado: string): Observable<UrgenciaRegistroDto> {
    return this.http.patch<UrgenciaRegistroDto>(`${this.apiUrl}/${id}/estado`, { estado });
  }
}
