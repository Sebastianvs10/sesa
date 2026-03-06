/**
 * Servicio de Laboratorio — solicitudes, órdenes de HC y registro de resultados.
 * Autor: Ing. J Sebastian Vargas S
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface LaboratorioSolicitudDto {
  id: number;
  pacienteId: number;
  pacienteNombre: string;
  pacienteDocumento?: string;
  solicitanteId?: number;
  solicitanteNombre?: string;
  tipoPrueba: string;
  estado: string;
  fechaSolicitud: string;
  resultado?: string;
  observaciones?: string;
  fechaResultado?: string;
  bacteriologoId?: number;
  bacteriologoNombre?: string;
  createdAt?: string;
}

export interface LaboratorioSolicitudRequestDto {
  pacienteId: number;
  solicitanteId?: number;
  tipoPrueba: string;
  estado?: string;
}

export interface ResultadoRequestDto {
  resultado: string;
  observaciones?: string;
  bacteriologoId?: number;
}

export interface OrdenClinicaLabDto {
  id: number;
  pacienteId: number;
  pacienteNombre: string;
  consultaId: number;
  tipo: string;
  detalle: string;
  estado: string;
  resultado?: string;
  fechaResultado?: string;
  resultadoRegistradoPorNombre?: string;
  resultadoRegistradoPorRol?: string;
  createdAt?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements?: number;
  totalPages?: number;
}

@Injectable({ providedIn: 'root' })
export class LaboratorioSolicitudService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/laboratorio-solicitudes`;
  private readonly ordenesUrl = `${environment.apiUrl}/ordenes-clinicas`;

  list(estado?: string): Observable<PageResponse<LaboratorioSolicitudDto>> {
    let params = new HttpParams().set('page', '0').set('size', '100');
    if (estado?.trim()) params = params.set('estado', estado.trim());
    return this.http.get<PageResponse<LaboratorioSolicitudDto>>(this.apiUrl, { params });
  }

  getById(id: number): Observable<LaboratorioSolicitudDto> {
    return this.http.get<LaboratorioSolicitudDto>(`${this.apiUrl}/${id}`);
  }

  getByPaciente(pacienteId: number): Observable<LaboratorioSolicitudDto[]> {
    return this.http.get<LaboratorioSolicitudDto[]>(`${this.apiUrl}/paciente/${pacienteId}`);
  }

  crear(dto: LaboratorioSolicitudRequestDto): Observable<LaboratorioSolicitudDto> {
    return this.http.post<LaboratorioSolicitudDto>(this.apiUrl, dto);
  }

  actualizar(id: number, dto: Partial<LaboratorioSolicitudRequestDto>): Observable<LaboratorioSolicitudDto> {
    return this.http.put<LaboratorioSolicitudDto>(`${this.apiUrl}/${id}`, dto);
  }

  registrarResultado(id: number, dto: ResultadoRequestDto): Observable<LaboratorioSolicitudDto> {
    return this.http.patch<LaboratorioSolicitudDto>(`${this.apiUrl}/${id}/resultado`, dto);
  }

  cambiarEstado(id: number, estado: string): Observable<LaboratorioSolicitudDto> {
    return this.http.patch<LaboratorioSolicitudDto>(`${this.apiUrl}/${id}/estado`, { estado });
  }

  getOrdenesLab(): Observable<OrdenClinicaLabDto[]> {
    const params = new HttpParams().set('page', '0').set('size', '100');
    return this.http.get<OrdenClinicaLabDto[]>(`${this.ordenesUrl}/laboratorio`, { params });
  }
}
