/**
 * Servicio Angular para el módulo RDA (Resumen Digital de Atención en Salud)
 * Resolución 1888 de 2025 — IHCE Colombia — HL7 FHIR R4
 * Autor: Ing. J Sebastian Vargas S
 */
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type TipoRda = 'CONSULTA_EXTERNA' | 'HOSPITALIZACION' | 'URGENCIAS' | 'PACIENTE';
export type EstadoRda = 'PENDIENTE' | 'ENVIADO' | 'CONFIRMADO' | 'ERROR';

export interface RdaStatus {
  rdaId: number;
  atencionId: number;
  tipoRda: TipoRda;
  estadoEnvio: EstadoRda;
  idMinisterio: string | null;
  fechaGeneracion: string;
  fechaEnvio: string | null;
  fechaConfirmacion: string | null;
  errorMensaje: string | null;
  reintentos: number;
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
