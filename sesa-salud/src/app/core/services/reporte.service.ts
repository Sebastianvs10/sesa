import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ReporteResumenDto {
  totalPacientes: number;
  totalCitas: number;
  totalConsultas: number;
  totalOrdenes: number;
  totalFacturas: number;
  totalFacturado: number;
}

export interface DashboardCitasPorDiaDto {
  fecha: string;
  total: number;
}

export interface DashboardMesCountDto {
  anio: number;
  mes: number;
  total: number;
}

export interface DashboardFacturacionMesDto {
  anio: number;
  mes: number;
  cantidad: number;
  valorTotal: number;
}

export interface DashboardCitasPorEstadoDto {
  estado: string;
  total: number;
}

/** Respuesta del endpoint único GET /api/reportes/dashboard */
export interface DashboardStatsDto {
  citasPorDia?: DashboardCitasPorDiaDto[];
  consultasPorMes?: DashboardMesCountDto[];
  facturacionPorMes?: DashboardFacturacionMesDto[];
  citasPorEstado?: DashboardCitasPorEstadoDto[];
}

/** Indicador de calidad en salud — Res. 0256/2016 */
export interface IndicadorCalidadDto {
  codigo: string;
  nombre: string;
  categoria: string;
  valor: string;
  meta: string;
  unidad: string;
  interpretacion: string;
}

@Injectable({ providedIn: 'root' })
export class ReporteService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/reportes`;

  resumen(): Observable<ReporteResumenDto> {
    return this.http.get<ReporteResumenDto>(`${this.apiUrl}/resumen`);
  }

  /**
   * Una sola llamada para todas las gráficas del dashboard.
   * URL: GET /api/reportes/dashboard (el backend tiene context-path /api).
   */
  dashboardStats(): Observable<DashboardStatsDto> {
    return this.http.get<DashboardStatsDto>(`${this.apiUrl}/dashboard`);
  }

  citasPorDia(dias: number = 7): Observable<DashboardCitasPorDiaDto[]> {
    const params = new HttpParams().set('dias', String(dias));
    return this.http.get<DashboardCitasPorDiaDto[]>(`${this.apiUrl}/dashboard/citas-por-dia`, { params });
  }

  consultasPorMes(meses: number = 6): Observable<DashboardMesCountDto[]> {
    const params = new HttpParams().set('meses', String(meses));
    return this.http.get<DashboardMesCountDto[]>(`${this.apiUrl}/dashboard/consultas-por-mes`, { params });
  }

  facturacionPorMes(meses: number = 6): Observable<DashboardFacturacionMesDto[]> {
    const params = new HttpParams().set('meses', String(meses));
    return this.http.get<DashboardFacturacionMesDto[]>(`${this.apiUrl}/dashboard/facturacion-por-mes`, { params });
  }

  citasPorEstado(): Observable<DashboardCitasPorEstadoDto[]> {
    return this.http.get<DashboardCitasPorEstadoDto[]>(`${this.apiUrl}/dashboard/citas-por-estado`);
  }

  /** Tablero de calidad en salud — Res. 0256/2016 */
  indicadoresCalidad(): Observable<IndicadorCalidadDto[]> {
    return this.http.get<IndicadorCalidadDto[]>(`${this.apiUrl}/calidad`);
  }
}
