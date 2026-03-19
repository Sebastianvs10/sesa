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
  /** S6: Datos de alta */
  altaDiagnostico?: string;
  altaTratamiento?: string;
  altaRecomendaciones?: string;
  altaProximaCita?: string;
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

export interface UrgenciaDashboardDto {
  conteoPorEstado: Record<string, number>;
  conteoPorTriage: Record<string, number>;
  fueraDeTiempo: UrgenciaFueraDeTiempoItemDto[];
  tiempoPromedioEsperaMinutos: number;
  totalEnEspera: number;
}

export interface UrgenciaFueraDeTiempoItemDto {
  id: number;
  pacienteId: number;
  pacienteNombre: string;
  nivelTriage: string;
  fechaHoraIngreso: string;
  minutosEspera: number;
  limiteMinutos: number;
}

export interface UrgenciaReporteCumplimientoDto {
  desde: string;
  hasta: string;
  porTriage: CumplimientoTriageDto[];
  totalRegistros: number;
  totalAtendidos: number;
  totalDentroTiempo: number;
  totalFueraTiempo: number;
  porcentajeCumplimiento: number;
}

export interface CumplimientoTriageDto {
  nivelTriage: string;
  total: number;
  dentroTiempo: number;
  fueraTiempo: number;
  porcentajeCumplimiento: number;
}

export interface UrgenciaTriagePatchDto {
  nivelTriage?: string;
  profesionalTriageId?: number;
}

/** S6: Body para dar alta o referencia (checklist + PDF). */
export interface AltaReferenciaRequestDto {
  diagnostico?: string;
  tratamiento?: string;
  recomendaciones?: string;
  proximaCita?: string;
  motivoReferencia?: string;
  nivelReferencia?: string;
}

export interface SignosVitalesUrgenciaDto {
  id: number;
  urgenciaRegistroId: number;
  fechaHora: string;
  presionArterial?: string;
  frecuenciaCardiaca?: string;
  frecuenciaRespiratoria?: string;
  temperatura?: string;
  saturacionO2?: string;
  peso?: string;
  dolorEva?: string;
  glasgowOcular?: number;
  glasgowVerbal?: number;
  glasgowMotor?: number;
  createdAt?: string;
}

export interface SignosVitalesUrgenciaRequestDto {
  urgenciaRegistroId?: number;
  fechaHora?: string;
  presionArterial?: string;
  frecuenciaCardiaca?: string;
  frecuenciaRespiratoria?: string;
  temperatura?: string;
  saturacionO2?: string;
  peso?: string;
  dolorEva?: string;
  glasgowOcular?: number;
  glasgowVerbal?: number;
  glasgowMotor?: number;
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

  dashboard(): Observable<UrgenciaDashboardDto> {
    return this.http.get<UrgenciaDashboardDto>(`${this.apiUrl}/dashboard`);
  }

  reporteCumplimiento(desde: string, hasta: string): Observable<UrgenciaReporteCumplimientoDto> {
    const params = new HttpParams().set('desde', desde).set('hasta', hasta);
    return this.http.get<UrgenciaReporteCumplimientoDto>(`${this.apiUrl}/reporte-cumplimiento`, { params });
  }

  updateTriage(id: number, dto: UrgenciaTriagePatchDto): Observable<UrgenciaRegistroDto> {
    return this.http.patch<UrgenciaRegistroDto>(`${this.apiUrl}/${id}/triage`, dto);
  }

  listSignosVitales(urgenciaId: number): Observable<SignosVitalesUrgenciaDto[]> {
    return this.http.get<SignosVitalesUrgenciaDto[]>(`${this.apiUrl}/${urgenciaId}/signos-vitales`);
  }

  createSignosVitales(urgenciaId: number, dto: SignosVitalesUrgenciaRequestDto): Observable<SignosVitalesUrgenciaDto> {
    dto.urgenciaRegistroId = urgenciaId;
    return this.http.post<SignosVitalesUrgenciaDto>(`${this.apiUrl}/${urgenciaId}/signos-vitales`, dto);
  }

  /** S6: Dar alta (estado ALTA + datos para PDF). */
  darAlta(id: number, body: AltaReferenciaRequestDto): Observable<UrgenciaRegistroDto> {
    return this.http.post<UrgenciaRegistroDto>(`${this.apiUrl}/${id}/alta`, body ?? {});
  }

  /** S6: Descargar PDF de resumen de alta. */
  getPdfAlta(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/alta/pdf`, { responseType: 'blob' });
  }
}
