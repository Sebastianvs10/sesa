import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

/** S6: Body para referencia (consulta/atención). */
export interface AltaReferenciaRequestDto {
  diagnostico?: string;
  tratamiento?: string;
  recomendaciones?: string;
  proximaCita?: string;
  motivoReferencia?: string;
  nivelReferencia?: string;
}

export interface DiagnosticoDto {
  id?: number;
  codigoCie10: string;
  descripcion?: string;
  tipo?: string;
}

export interface ProcedimientoDto {
  id?: number;
  codigoCups: string;
  descripcion?: string;
}

export interface FormulaMedicaDto {
  id?: number;
  medicamento: string;
  dosis?: string;
  frecuencia?: string;
  duracion?: string;
}

export interface AtencionDto {
  id: number;
  historiaId: number;
  profesionalId: number;
  profesionalNombre: string;
  fechaAtencion: string;
  motivoConsulta?: string;
  enfermedadActual?: string;
  planTratamiento?: string;
  diagnosticos: DiagnosticoDto[];
  procedimientos: ProcedimientoDto[];
  formulasMedicas: FormulaMedicaDto[];
  /** S6: Referencia */
  referenciaMotivo?: string;
  referenciaNivel?: string;
  referenciaDiagnostico?: string;
  referenciaTratamiento?: string;
  referenciaRecomendaciones?: string;
  referenciaProximaCita?: string;
}

export interface AtencionRequestDto {
  historiaId: number;
  profesionalId: number;
  fechaAtencion?: string;
  motivoConsulta?: string;
  enfermedadActual?: string;
  planTratamiento?: string;
  diagnosticos?: DiagnosticoDto[];
  procedimientos?: ProcedimientoDto[];
  formulasMedicas?: FormulaMedicaDto[];
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({ providedIn: 'root' })
export class AtencionService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/atenciones`;

  listByHistoria(historiaId: number, page = 0, size = 20): Observable<PageResponse<AtencionDto>> {
    const params = new HttpParams()
      .set('historiaId', historiaId.toString())
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<AtencionDto>>(this.apiUrl, { params });
  }

  get(id: number): Observable<AtencionDto> {
    return this.http.get<AtencionDto>(`${this.apiUrl}/${id}`);
  }

  create(request: AtencionRequestDto): Observable<AtencionDto> {
    return this.http.post<AtencionDto>(this.apiUrl, request);
  }

  update(id: number, request: AtencionRequestDto): Observable<AtencionDto> {
    return this.http.put<AtencionDto>(`${this.apiUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /** S6: Guardar referencia (motivo, nivel, datos para PDF). */
  guardarReferencia(id: number, body: AltaReferenciaRequestDto): Observable<AtencionDto> {
    return this.http.post<AtencionDto>(`${this.apiUrl}/${id}/referencia`, body ?? {});
  }

  /** S6: Descargar PDF de referencia. */
  getPdfReferencia(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/referencia/pdf`, { responseType: 'blob' });
  }
}
