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

/** Score de riesgo del paciente para cabecera HC (S1). */
export interface PacienteRiesgoDto {
  nivelRiesgo: string;
  puntos: number;
  factores: string[];
  recomendaciones: string[];
}

/** S4: Panel de cumplimiento normativo. */
export interface CumplimientoNormativoDto {
  periodoInicio: string;
  periodoFin: string;
  profesionalId?: number;
  porcentajeRdaEnviado: number;
  totalAtenciones: number;
  atencionesConRdaEnviado: number;
  porcentajeUrgenciasEnTiempo: number;
  totalUrgenciasAtendidas: number;
  urgenciasDentroTiempo: number;
  porcentajeHcConCie10YEvolucion24h: number;
  atencionesConCie10YEvolucion24h: number;
  totalResultadosCriticosNoLeidos: number;
  indicadores0256: IndicadorCalidadDto[];
}

/** S16: Auditoría de calidad HC — evaluación por atención. */
export interface EvaluacionHcDto {
  consultaId: number;
  camposCompletos: boolean;
  camposFaltantes: string[];
  puntuacion: number;
  umbralAceptable: number;
}

/** S16: Resumen por profesional. */
export interface AuditoriaHcProfesionalDto {
  profesionalId: number;
  profesionalNombre: string;
  totalAtenciones: number;
  atencionesCompletas: number;
  porcentajeCompletas: number;
  puntuacionMedia: number;
  bajoUmbral: boolean;
}

/** S16: Resumen por servicio (tipo consulta). */
export interface AuditoriaHcServicioDto {
  servicioId: number;
  servicioNombre: string;
  totalAtenciones: number;
  atencionesCompletas: number;
  porcentajeCompletas: number;
  puntuacionMedia: number;
  bajoUmbral: boolean;
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

  /** Score de riesgo del paciente para cabecera de historia clínica (S1). */
  getRiesgoPaciente(pacienteId: number): Observable<PacienteRiesgoDto> {
    return this.http.get<PacienteRiesgoDto>(`${this.apiUrl}/paciente/${pacienteId}/riesgo`);
  }

  /** S4: Panel de cumplimiento normativo. */
  getCumplimientoNormativo(params: {
    desde?: string;
    hasta?: string;
    profesionalId?: number;
  }): Observable<CumplimientoNormativoDto> {
    let httpParams = new HttpParams();
    if (params.desde) httpParams = httpParams.set('desde', params.desde);
    if (params.hasta) httpParams = httpParams.set('hasta', params.hasta);
    if (params.profesionalId != null) httpParams = httpParams.set('profesionalId', String(params.profesionalId));
    return this.http.get<CumplimientoNormativoDto>(`${this.apiUrl}/cumplimiento-normativo`, { params: httpParams });
  }

  /** S16: Auditoría de calidad HC por profesional. */
  getAuditoriaHcPorProfesional(params: { desde?: string; hasta?: string }): Observable<AuditoriaHcProfesionalDto[]> {
    let httpParams = new HttpParams();
    if (params.desde) httpParams = httpParams.set('desde', params.desde);
    if (params.hasta) httpParams = httpParams.set('hasta', params.hasta);
    return this.http.get<AuditoriaHcProfesionalDto[]>(`${this.apiUrl}/auditoria-hc/por-profesional`, { params: httpParams });
  }

  /** S16: Auditoría de calidad HC por servicio. */
  getAuditoriaHcPorServicio(params: { desde?: string; hasta?: string }): Observable<AuditoriaHcServicioDto[]> {
    let httpParams = new HttpParams();
    if (params.desde) httpParams = httpParams.set('desde', params.desde);
    if (params.hasta) httpParams = httpParams.set('hasta', params.hasta);
    return this.http.get<AuditoriaHcServicioDto[]>(`${this.apiUrl}/auditoria-hc/por-servicio`, { params: httpParams });
  }
}
