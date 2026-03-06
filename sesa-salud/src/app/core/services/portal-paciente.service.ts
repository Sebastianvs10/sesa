/**
 * Autor: Ing. J Sebastian Vargas S
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { HistoriaClinicaDto } from './historia-clinica.service';
import { PacienteDto } from './paciente.service';
import type { ConsentimientoInformadoDto } from './consentimiento.service';

export interface PortalResumenDto {
  paciente: PacienteDto;
  totalHistorias: number;
  totalLaboratorios: number;
  totalOrdenes: number;
  proximaCita?: ProximaCitaDto;
  ultimaConsulta?: UltimaConsultaDto;
}

export interface ProximaCitaDto {
  id: number;
  fecha: string;
  hora: string;
  medico: string;
  especialidad: string;
  estado: string;
}

export interface UltimaConsultaDto {
  id: number;
  fecha: string;
  medico: string;
  motivo: string;
  diagnostico?: string;
}

export interface PortalLaboratorioDto {
  id: number;
  fechaSolicitud: string;
  fechaResultado?: string;
  nombreExamen: string;
  resultado?: string;
  valorReferencia?: string;
  estado: string;
  medico: string;
  critico?: boolean;
}

export interface PortalOrdenDto {
  id: number;
  fecha: string;
  tipo: string;
  descripcion: string;
  medico: string;
  estado: string;
  cups?: string;
  diagnostico?: string;
}

export interface PortalConsultaDto {
  id: number;
  fecha: string;
  medico: string;
  especialidad?: string;
  motivo: string;
  diagnostico?: string;
  codigoCie10?: string;
  planTratamiento?: string;
  observaciones?: string;
}

@Injectable({ providedIn: 'root' })
export class PortalPacienteService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  /** Resumen del paciente autenticado (citas, stats, última consulta) */
  getResumen(): Observable<PortalResumenDto> {
    return this.http.get<PortalResumenDto>(`${this.apiUrl}/portal/resumen`);
  }

  /** Historia clínica del paciente autenticado */
  getHistoriaClinica(): Observable<HistoriaClinicaDto[]> {
    return this.http.get<HistoriaClinicaDto[]>(`${this.apiUrl}/portal/historia-clinica`);
  }

  /** Consultas del paciente autenticado */
  getConsultas(page = 0, size = 10): Observable<{ content: PortalConsultaDto[]; totalElements: number }> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<{ content: PortalConsultaDto[]; totalElements: number }>(
      `${this.apiUrl}/portal/consultas`, { params }
    );
  }

  /** Resultados de laboratorio del paciente autenticado */
  getLaboratorios(page = 0, size = 10): Observable<{ content: PortalLaboratorioDto[]; totalElements: number }> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<{ content: PortalLaboratorioDto[]; totalElements: number }>(
      `${this.apiUrl}/portal/laboratorios`, { params }
    );
  }

  /** Órdenes médicas del paciente autenticado */
  getOrdenes(page = 0, size = 10): Observable<{ content: PortalOrdenDto[]; totalElements: number }> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<{ content: PortalOrdenDto[]; totalElements: number }>(
      `${this.apiUrl}/portal/ordenes`, { params }
    );
  }

  /** Perfil del paciente autenticado */
  getMiPerfil(): Observable<PacienteDto> {
    return this.http.get<PacienteDto>(`${this.apiUrl}/portal/perfil`);
  }

  /** Consentimientos pendientes de firma (portal/móvil paciente) */
  getConsentimientosPendientes(): Observable<ConsentimientoInformadoDto[]> {
    return this.http.get<ConsentimientoInformadoDto[]>(`${this.apiUrl}/portal/consentimientos-pendientes`);
  }
}
