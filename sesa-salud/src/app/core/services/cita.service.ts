/**
 * Servicio HTTP para gestión de citas y módulo Consulta Médica.
 * Autor: Ing. J Sebastian Vargas S
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface CitaDto {
  id: number;
  pacienteId: number;
  pacienteNombre: string;
  profesionalId: number;
  profesionalNombre: string;
  servicio: string;
  fechaHora: string;
  estado: string;
  notas?: string;
  createdAt?: string;
  // Campos normativos Res. 2953/2014
  tipoCita?: string;
  numeroAutorizacionEps?: string;
  duracionEstimadaMin?: number;
  diasEspera?: number;
  alertaOportunidad?: boolean;
}

export interface ConsultaMedicaDto {
  id: number;
  fechaHora: string;
  servicio: string;
  estado: string;
  notas?: string;
  motivoCancelacion?: string;
  // Paciente
  pacienteId: number;
  pacienteNombreCompleto: string;
  pacienteDocumento: string;
  pacienteTipoDocumento?: string;
  pacienteEdad?: number;
  pacienteSexo?: string;
  pacienteGrupoSanguineo?: string;
  pacienteTelefono?: string;
  pacienteEps?: string;
  pacienteEpsCodigo?: string;
  // Profesional
  profesionalId: number;
  profesionalNombre: string;
  profesionalRol?: string;
  // Alertas
  tieneHistoriaClinica: boolean;
  tieneFacturasPendientes: boolean;
  ultimaAtencion?: string;
  createdAt?: string;
}

export interface ConsultasStatsDto {
  total: number;
  agendadas: number;
  atendidas: number;
  canceladas: number;
  porcentajeAsistencia: number;
}

export interface ProfesionalDto {
  id: number;
  nombre: string;
  rol: string;
}

@Injectable({ providedIn: 'root' })
export class CitaService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/citas`;

  /** Citas de la fecha (o hoy). Si profesionalId se indica, solo citas de ese profesional (horas por especialista). */
  list(fecha?: string, profesionalId?: number): Observable<CitaDto[]> {
    let params = new HttpParams();
    if (fecha) params = params.set('fecha', fecha);
    if (profesionalId != null && profesionalId > 0) params = params.set('profesionalId', String(profesionalId));
    return this.http.get<CitaDto[]>(this.apiUrl, { params });
  }

  listByFecha(fecha?: string): Observable<CitaDto[]> {
    let params = new HttpParams();
    if (fecha) params = params.set('fecha', fecha);
    return this.http.get<CitaDto[]>(this.apiUrl, { params });
  }

  getById(id: number): Observable<CitaDto> {
    return this.http.get<CitaDto>(`${this.apiUrl}/${id}`);
  }

  create(dto: Partial<CitaDto>): Observable<CitaDto> {
    return this.http.post<CitaDto>(this.apiUrl, dto);
  }

  update(id: number, dto: Partial<CitaDto>): Observable<CitaDto> {
    return this.http.put<CitaDto>(`${this.apiUrl}/${id}`, dto);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // ── Módulo Consulta Médica ─────────────────────────────────────────────

  /** Lista enriquecida para el módulo Consulta Médica. */
  getConsultasMedicas(fecha?: string, profesionalId?: number): Observable<ConsultaMedicaDto[]> {
    let params = new HttpParams();
    if (fecha) params = params.set('fecha', fecha);
    if (profesionalId != null) params = params.set('profesionalId', profesionalId);
    return this.http.get<ConsultaMedicaDto[]>(`${this.apiUrl}/consulta-medica`, { params });
  }

  /** Estadísticas del día. */
  getStats(fecha?: string, profesionalId?: number): Observable<ConsultasStatsDto> {
    let params = new HttpParams();
    if (fecha) params = params.set('fecha', fecha);
    if (profesionalId != null) params = params.set('profesionalId', profesionalId);
    return this.http.get<ConsultasStatsDto>(`${this.apiUrl}/consulta-medica/stats`, { params });
  }

  /** Lista de profesionales médicos para filtrado admin. */
  getProfesionalesMedicos(): Observable<ProfesionalDto[]> {
    return this.http.get<ProfesionalDto[]>(`${this.apiUrl}/consulta-medica/profesionales`);
  }

  /** Cancela la cita con motivo. */
  cancelarCita(id: number, motivo: string): Observable<CitaDto> {
    return this.http.patch<CitaDto>(`${this.apiUrl}/${id}/cancelar`, { motivo });
  }

  /** Cambia el estado de la cita. */
  cambiarEstado(id: number, estado: string): Observable<CitaDto> {
    return this.http.patch<CitaDto>(`${this.apiUrl}/${id}/estado`, { estado });
  }
}
