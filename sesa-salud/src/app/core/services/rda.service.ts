/**
 * Servicio Angular para el módulo RDA (Resumen Digital de Atención en Salud)
 * Resolución 1888 de 2025 — IHCE Colombia — HL7 FHIR R4
 * Autor: Ing. J Sebastian Vargas S
 */
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export type TipoRda = 'CONSULTA_EXTERNA' | 'HOSPITALIZACION' | 'URGENCIAS' | 'PACIENTE';
export type EstadoRda = 'PENDIENTE' | 'ENVIADO' | 'CONFIRMADO' | 'ERROR';

export interface RdaStatus {
  rdaId: number;
  atencionId: number;
  /** S11: ID del registro de urgencia cuando tipo es URGENCIAS */
  urgenciaRegistroId?: number;
  /** S11: ID de hospitalización cuando tipo es HOSPITALIZACION */
  hospitalizacionId?: number;
  tipoRda: TipoRda;
  estadoEnvio: EstadoRda;
  idMinisterio: string | null;
  fechaGeneracion: string;
  fechaEnvio: string | null;
  fechaConfirmacion: string | null;
  errorMensaje: string | null;
  reintentos: number;
}

/** S17: Resumen de RDA recibido de otra IPS (IHCE). */
export interface RdaRecibidoResumen {
  id: number;
  pacienteId: number;
  idMinisterio: string | null;
  tipoRda: string;
  fechaAtencion: string | null;
  institucionOrigen: string | null;
  fetchedAt: string | null;
}

/** S17: Detalle de RDA recibido. */
export interface RdaRecibidoDetalle {
  id: number;
  pacienteId: number;
  idMinisterio: string | null;
  tipoRda: string;
  fechaAtencion: string | null;
  institucionOrigen: string | null;
  resumenLegible: string | null;
  fetchedAt: string | null;
}

@Injectable({ providedIn: 'root' })
export class RdaService {

  private readonly base = `${environment.apiUrl}/rda`;

  constructor(private http: HttpClient) {}

  /** Genera el Bundle FHIR para la atención */
  generar(atencionId: number, tipoRda: TipoRda = 'CONSULTA_EXTERNA'): Observable<RdaStatus> {
    const params = new HttpParams().set('tipoRda', tipoRda);
    return this.http.post<RdaStatus>(
      `${this.base}/generar/${atencionId}`, null, { params });
  }

  /** Envía el Bundle al servidor FHIR del Ministerio */
  enviarAlMinisterio(atencionId: number, tipoRda: TipoRda = 'CONSULTA_EXTERNA'): Observable<RdaStatus> {
    const params = new HttpParams().set('tipoRda', tipoRda);
    return this.http.post<RdaStatus>(
      `${this.base}/enviar/${atencionId}`, null, { params });
  }

  /** Genera Y envía en un solo paso */
  generarYEnviar(atencionId: number, tipoRda: TipoRda = 'CONSULTA_EXTERNA'): Observable<RdaStatus> {
    const params = new HttpParams().set('tipoRda', tipoRda);
    return this.http.post<RdaStatus>(
      `${this.base}/generar-y-enviar/${atencionId}`, null, { params });
  }

  /** S11: Genera el RDA de Urgencias para un registro de urgencia */
  generarRdaUrgencia(urgenciaRegistroId: number): Observable<RdaStatus> {
    return this.http.post<RdaStatus>(
      `${this.base}/generar/urgencia/${urgenciaRegistroId}`, null);
  }

  /** S11: Genera y envía RDA de Urgencias en un paso */
  generarYEnviarUrgencia(urgenciaRegistroId: number): Observable<RdaStatus> {
    return this.http.post<RdaStatus>(
      `${this.base}/generar-y-enviar/urgencia/${urgenciaRegistroId}`, null);
  }

  /** S11: Genera el RDA de Hospitalización para un ingreso */
  generarRdaHospitalizacion(hospitalizacionId: number): Observable<RdaStatus> {
    return this.http.post<RdaStatus>(
      `${this.base}/generar/hospitalizacion/${hospitalizacionId}`, null);
  }

  /** S11: Genera y envía RDA de Hospitalización en un paso */
  generarYEnviarHospitalizacion(hospitalizacionId: number): Observable<RdaStatus> {
    return this.http.post<RdaStatus>(
      `${this.base}/generar-y-enviar/hospitalizacion/${hospitalizacionId}`, null);
  }

  /** Lista todos los envíos de una atención */
  listarPorAtencion(atencionId: number): Observable<RdaStatus[]> {
    return this.http.get<RdaStatus[]>(`${this.base}/estado/${atencionId}`);
  }

  /** Obtiene el último RDA de una atención por tipo */
  obtenerUltimo(atencionId: number, tipoRda: TipoRda = 'CONSULTA_EXTERNA'): Observable<RdaStatus> {
    const params = new HttpParams().set('tipoRda', tipoRda);
    return this.http.get<RdaStatus>(`${this.base}/estado/${atencionId}/ultimo`, { params });
  }

  /** Descarga el Bundle FHIR en formato JSON */
  descargarBundle(rdaId: number): Observable<Blob> {
    return this.http.get(`${this.base}/bundle/${rdaId}`, {
      responseType: 'blob',
      headers: { Accept: 'application/fhir+json' }
    });
  }

  /** Info normativa del módulo */
  infoNormativo(): Observable<Record<string, string>> {
    return this.http.get<Record<string, string>>(`${this.base}/info`);
  }

  /** S17: RDA recibidos de otras IPS (IHCE). Lista por paciente. */
  getRdaRecibidosPaciente(pacienteId: number): Observable<RdaRecibidoResumen[]> {
    return this.http.get<RdaRecibidoResumen[]>(`${this.base}/recibidos/paciente/${pacienteId}`);
  }

  /** S17: Detalle de un RDA recibido. */
  getRdaRecibidoDetalle(id: number): Observable<RdaRecibidoDetalle | null> {
    return this.http.get<RdaRecibidoDetalle>(`${this.base}/recibidos/${id}`).pipe(
      catchError(() => of(null))
    );
  }

  /** Helpers UI */
  estadoLabel(estado: EstadoRda): string {
    const labels: Record<EstadoRda, string> = {
      PENDIENTE:   'Pendiente',
      ENVIADO:     'Enviado',
      CONFIRMADO:  'Confirmado',
      ERROR:       'Error'
    };
    return labels[estado] ?? estado;
  }

  estadoColor(estado: EstadoRda): string {
    const colors: Record<EstadoRda, string> = {
      PENDIENTE:  'warning',
      ENVIADO:    'info',
      CONFIRMADO: 'success',
      ERROR:      'danger'
    };
    return colors[estado] ?? 'secondary';
  }
}
