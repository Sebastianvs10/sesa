/**
 * Servicio EBS — gestión territorial, hogares y visitas domiciliarias.
 * Autor: Ing. J Sebastian Vargas S
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type EbsRiskLevel = 'BAJO' | 'MEDIO' | 'ALTO' | 'MUY_ALTO';

export interface EbsTerritorySummary {
  id: number;
  code: string;
  name: string;
  type: string;
  householdsCount: number;
  visitedHouseholdsCount: number;
  highRiskHouseholdsCount: number;
  igacDepartamentoCodigo?: string;
  igacMunicipioCodigo?: string;
  igacVeredaCodigo?: string;
  igacDepartamentoNombre?: string;
  igacMunicipioNombre?: string;
  igacVeredaNombre?: string;
}

export type EbsVisitState = 'PENDIENTE_VISITA' | 'EN_SEGUIMIENTO' | 'CERRADO';

export interface EbsHouseholdSummary {
  id: number;
  territoryId: number;
  addressText: string;
  latitude?: number;
  longitude?: number;
  lastVisitDate?: string;
  riskLevel?: EbsRiskLevel;
  state: EbsVisitState;
}

export interface EbsTerritoryCreate {
  code: string;
  name: string;
  type?: string;
  igacDepartamentoCodigo?: string;
  igacMunicipioCodigo?: string;
  igacVeredaCodigo?: string;
}

export interface EbsBrigadeDto {
  id?: number;
  name: string;
  territoryId: number;
  territoryName?: string;
  dateStart: string;
  dateEnd: string;
  status?: string;
  notes?: string;
  teamMemberIds?: number[];
  teamMemberNames?: string[];
}

export interface EbsAlertDto {
  id?: number;
  type: string;
  veredaCodigo?: string;
  municipioCodigo?: string;
  departamentoCodigo?: string;
  title: string;
  description?: string;
  alertDate: string;
  status?: string;
  externalId?: string;
}

export interface EbsReportDataDto {
  reportType: string;
  periodFrom?: string;
  periodTo?: string;
  totalHouseholds?: number;
  visitedHouseholds?: number;
  coveragePercent?: number;
  rows?: { territoryName: string; veredaName?: string; households: number; visited: number; percent: number; highRisk: number }[];
}

export interface EbsHomeVisitPayload {
  householdId: number;
  familyGroupId?: number;
  visitDate: string;
  visitType: string;
  tipoIntervencion?: string;
  veredaCodigo?: string;
  diagnosticoCie10?: string;
  planCuidado?: string;
  brigadeId?: number;
  motivo?: string;
  notes?: string;
  riskFlags?: {
    cardiovascular?: boolean;
    materno?: boolean;
    cronico?: boolean;
  };
}

export interface EbsHomeVisitSummary {
  id: number;
  householdId: number;
  householdAddress?: string;
  territoryId: number;
  territoryName?: string;
  professionalId?: number;
  professionalName?: string;
  visitDate: string;
  visitType?: string;
  motivo?: string;
  notes?: string;
  status?: string;
  riskCardiovascular?: boolean;
  riskMaterno?: boolean;
  riskCronico?: boolean;
}

/** S13: DTO para sincronización de visitas EBS (offline-first). */
export interface VisitaEbsSyncDto {
  offlineUuid?: string;
  serverId?: number;
  clientUpdatedAt?: string;
  householdId?: number;
  familyGroupId?: number;
  visitDate?: string;
  visitType?: string;
  tipoIntervencion?: string;
  veredaCodigo?: string;
  diagnosticoCie10?: string;
  planCuidado?: string;
  brigadeId?: number;
  professionalId?: number;
  motivo?: string;
  notes?: string;
  status?: string;
  riskFlags?: Record<string, boolean | undefined>;
}

/** S13: Conflicto de sincronización. */
export interface VisitaEbsConflictDto {
  offlineUuid?: string;
  serverId?: number;
  message?: string;
}

/** S13: Respuesta del endpoint de sincronización. */
export interface VisitaEbsSyncResponseDto {
  savedIds: number[];
  conflicts: VisitaEbsConflictDto[];
}

export interface EbsDashboardData {
  totalTerritorios: number;
  totalHogares: number;
  hogaresVisitados: number;
  porcentajeCobertura: number;
  hogaresAltoRiesgo: number;
  visitasEnPeriodo: number;
  cronicosControlados: number;
  alertasSeguimiento: number;
  porTerritorio?: EbsTerritoryIndicator[];
}

export interface EbsTerritoryIndicator {
  territoryId: number;
  territoryName: string;
  territoryCode: string;
  totalHogares: number;
  hogaresVisitados: number;
  porcentajeCobertura: number;
  hogaresAltoRiesgo: number;
  visitasEnPeriodo: number;
}

@Injectable({ providedIn: 'root' })
export class EbsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  /**
   * Lista los microterritorios asignados al equipo EBS actual.
   * El backend debe respetar el tenant y los permisos de usuario.
   */
  listTerritories(params?: { riskLevel?: EbsRiskLevel | 'TODOS' }): Observable<EbsTerritorySummary[]> {
    let httpParams = new HttpParams();
    if (params?.riskLevel && params.riskLevel !== 'TODOS') {
      httpParams = httpParams.set('riskLevel', params.riskLevel);
    }
    return this.http.get<EbsTerritorySummary[]>(`${this.apiUrl}/ebs/territories`, {
      params: httpParams,
    });
  }

  /**
   * Lista hogares para un microterritorio, con filtros por riesgo y estado de visita.
   */
  listHouseholds(
    territoryId: number,
    params?: { riskLevel?: EbsRiskLevel | 'TODOS'; visitStatus?: EbsVisitState | 'TODOS' },
  ): Observable<EbsHouseholdSummary[]> {
    let httpParams = new HttpParams().set('territoryId', String(territoryId));
    if (params?.riskLevel && params.riskLevel !== 'TODOS') {
      httpParams = httpParams.set('riskLevel', params.riskLevel);
    }
    if (params?.visitStatus && params.visitStatus !== 'TODOS') {
      httpParams = httpParams.set('visitStatus', params.visitStatus);
    }
    return this.http.get<EbsHouseholdSummary[]>(`${this.apiUrl}/ebs/households`, {
      params: httpParams,
    });
  }

  /**
   * Crea una visita domiciliaria estructurada desde la app móvil.
   * La lógica offline y el cache GET la maneja el interceptor offline global.
   */
  createHomeVisit(payload: EbsHomeVisitPayload) {
    return this.http.post(`${this.apiUrl}/ebs/home-visits`, payload);
  }

  /**
   * Lista visitas domiciliarias con filtros opcionales.
   */
  listHomeVisits(params?: {
    territoryId?: number;
    professionalId?: number;
    dateFrom?: string;
    dateTo?: string;
  }): Observable<EbsHomeVisitSummary[]> {
    let httpParams = new HttpParams();
    if (params?.territoryId != null) httpParams = httpParams.set('territoryId', String(params.territoryId));
    if (params?.professionalId != null) httpParams = httpParams.set('professionalId', String(params.professionalId));
    if (params?.dateFrom) httpParams = httpParams.set('dateFrom', params.dateFrom);
    if (params?.dateTo) httpParams = httpParams.set('dateTo', params.dateTo);
    return this.http.get<EbsHomeVisitSummary[]>(`${this.apiUrl}/ebs/home-visits`, { params: httpParams });
  }

  /**
   * Dashboard gerencial para Supervisor APS.
   */
  getDashboard(diasPeriodo: number = 30): Observable<EbsDashboardData> {
    return this.http.get<EbsDashboardData>(`${this.apiUrl}/ebs/dashboard`, {
      params: { diasPeriodo: String(diasPeriodo) },
    });
  }

  createTerritory(dto: EbsTerritoryCreate): Observable<EbsTerritorySummary> {
    return this.http.post<EbsTerritorySummary>(`${this.apiUrl}/ebs/territories`, dto);
  }

  getTerritoryTeam(territoryId: number): Observable<number[]> {
    return this.http.get<number[]>(`${this.apiUrl}/ebs/territories/${territoryId}/team`);
  }

  setTerritoryTeam(territoryId: number, personalIds: number[]): Observable<unknown> {
    return this.http.put(`${this.apiUrl}/ebs/territories/${territoryId}/team`, personalIds);
  }

  /**
   * Actualiza los códigos IGAC (límites oficiales) de un territorio EBS.
   */
  updateTerritoryIgac(
    territoryId: number,
    dto: { igacDepartamentoCodigo?: string; igacMunicipioCodigo?: string; igacVeredaCodigo?: string }
  ): Observable<unknown> {
    return this.http.put(`${this.apiUrl}/ebs/territories/${territoryId}/igac`, dto);
  }

  listBrigades(territoryId?: number): Observable<EbsBrigadeDto[]> {
    const params = territoryId != null ? new HttpParams().set('territoryId', String(territoryId)) : undefined;
    return this.http.get<EbsBrigadeDto[]>(`${this.apiUrl}/ebs/brigades`, { params });
  }

  createBrigade(dto: EbsBrigadeDto): Observable<EbsBrigadeDto> {
    return this.http.post<EbsBrigadeDto>(`${this.apiUrl}/ebs/brigades`, dto);
  }

  updateBrigade(id: number, dto: EbsBrigadeDto): Observable<EbsBrigadeDto> {
    return this.http.put<EbsBrigadeDto>(`${this.apiUrl}/ebs/brigades/${id}`, dto);
  }

  deleteBrigade(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/ebs/brigades/${id}`);
  }

  getBrigadeTeam(brigadeId: number): Observable<number[]> {
    return this.http.get<number[]>(`${this.apiUrl}/ebs/brigades/${brigadeId}/team`);
  }

  setBrigadeTeam(brigadeId: number, personalIds: number[]): Observable<unknown> {
    return this.http.put(`${this.apiUrl}/ebs/brigades/${brigadeId}/team`, personalIds);
  }

  listAlerts(status?: string): Observable<EbsAlertDto[]> {
    const params = status ? new HttpParams().set('status', status) : undefined;
    return this.http.get<EbsAlertDto[]>(`${this.apiUrl}/ebs/alerts`, { params });
  }

  createAlert(dto: EbsAlertDto): Observable<EbsAlertDto> {
    return this.http.post<EbsAlertDto>(`${this.apiUrl}/ebs/alerts`, dto);
  }

  updateAlertStatus(id: number, status: string): Observable<EbsAlertDto> {
    return this.http.patch<EbsAlertDto>(`${this.apiUrl}/ebs/alerts/${id}/status`, null, {
      params: { status },
    });
  }

  getReportData(params?: { reportType?: string; periodFrom?: string; periodTo?: string }): Observable<EbsReportDataDto> {
    let httpParams = new HttpParams();
    if (params?.reportType) httpParams = httpParams.set('reportType', params.reportType);
    if (params?.periodFrom) httpParams = httpParams.set('periodFrom', params.periodFrom);
    if (params?.periodTo) httpParams = httpParams.set('periodTo', params.periodTo);
    return this.http.get<EbsReportDataDto>(`${this.apiUrl}/ebs/reports/data`, { params: httpParams });
  }

  /** S13: Visitas creadas después de una fecha (para pull de sincronización). */
  listPendientesSincronizar(desde?: string): Observable<EbsHomeVisitSummary[]> {
    const params = desde ? new HttpParams().set('desde', desde) : undefined;
    return this.http.get<EbsHomeVisitSummary[]>(`${this.apiUrl}/ebs/visitas/pendientes-sincronizar`, { params });
  }

  /** S13: Envía visitas del cliente (push) y devuelve IDs guardados y conflictos. */
  sincronizarVisitas(visitas: VisitaEbsSyncDto[]): Observable<VisitaEbsSyncResponseDto> {
    return this.http.post<VisitaEbsSyncResponseDto>(`${this.apiUrl}/ebs/visitas/sincronizar`, visitas ?? []);
  }
}

