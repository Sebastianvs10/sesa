/**
 * Servicio HTTP del módulo Odontología.
 * Autor: Ing. J Sebastian Vargas S
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ConsultaOdontologicaDto,
  OdontogramaEstadoDto,
  PlanTratamientoDto,
  ProcedimientoCatalogo,
  ImagenClinicaDto,
  EvolucionOdontologicaDto,
} from '../../features/odontologia/odontologia.models';

@Injectable({ providedIn: 'root' })
export class OdontologiaService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/odontologia`;

  // ── Consultas ────────────────────────────────────────────────────────

  getConsultasByPaciente(pacienteId: number, page = 0, size = 20): Observable<ConsultaOdontologicaDto[]> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ConsultaOdontologicaDto[]>(`${this.base}/consultas/paciente/${pacienteId}`, { params });
  }

  getConsulta(id: number): Observable<ConsultaOdontologicaDto> {
    return this.http.get<ConsultaOdontologicaDto>(`${this.base}/consultas/${id}`);
  }

  crearConsulta(dto: ConsultaOdontologicaDto): Observable<ConsultaOdontologicaDto> {
    return this.http.post<ConsultaOdontologicaDto>(`${this.base}/consultas`, dto);
  }

  actualizarConsulta(id: number, dto: ConsultaOdontologicaDto): Observable<ConsultaOdontologicaDto> {
    return this.http.put<ConsultaOdontologicaDto>(`${this.base}/consultas/${id}`, dto);
  }

  // ── Odontograma ──────────────────────────────────────────────────────

  getOdontograma(pacienteId: number): Observable<OdontogramaEstadoDto[]> {
    return this.http.get<OdontogramaEstadoDto[]>(`${this.base}/odontograma/${pacienteId}`);
  }

  getOdontogramaByConsulta(consultaId: number): Observable<OdontogramaEstadoDto[]> {
    return this.http.get<OdontogramaEstadoDto[]>(`${this.base}/odontograma/consulta/${consultaId}`);
  }

  guardarEstadoPieza(dto: OdontogramaEstadoDto): Observable<OdontogramaEstadoDto> {
    return this.http.post<OdontogramaEstadoDto>(`${this.base}/odontograma`, dto);
  }

  guardarOdontogramaBatch(dtos: OdontogramaEstadoDto[]): Observable<OdontogramaEstadoDto[]> {
    return this.http.post<OdontogramaEstadoDto[]>(`${this.base}/odontograma/batch`, dtos);
  }

  // ── Catálogo de procedimientos ───────────────────────────────────────

  getCatalogoProcedimientos(): Observable<ProcedimientoCatalogo[]> {
    return this.http.get<ProcedimientoCatalogo[]>(`${this.base}/procedimientos/catalogo`);
  }

  crearProcedimiento(dto: ProcedimientoCatalogo): Observable<ProcedimientoCatalogo> {
    return this.http.post<ProcedimientoCatalogo>(`${this.base}/procedimientos/catalogo`, dto);
  }

  actualizarProcedimiento(id: number, dto: ProcedimientoCatalogo): Observable<ProcedimientoCatalogo> {
    return this.http.put<ProcedimientoCatalogo>(`${this.base}/procedimientos/catalogo/${id}`, dto);
  }

  // ── Planes de tratamiento ────────────────────────────────────────────

  getPlanesByPaciente(pacienteId: number): Observable<PlanTratamientoDto[]> {
    return this.http.get<PlanTratamientoDto[]>(`${this.base}/planes/paciente/${pacienteId}`);
  }

  getPlan(id: number): Observable<PlanTratamientoDto> {
    return this.http.get<PlanTratamientoDto>(`${this.base}/planes/${id}`);
  }

  crearPlan(dto: PlanTratamientoDto): Observable<PlanTratamientoDto> {
    return this.http.post<PlanTratamientoDto>(`${this.base}/planes`, dto);
  }

  actualizarPlan(id: number, dto: PlanTratamientoDto): Observable<PlanTratamientoDto> {
    return this.http.put<PlanTratamientoDto>(`${this.base}/planes/${id}`, dto);
  }

  cambiarEstadoPlan(id: number, estado: string): Observable<PlanTratamientoDto> {
    return this.http.patch<PlanTratamientoDto>(`${this.base}/planes/${id}/estado`, { estado });
  }

  registrarAbono(id: number, monto: number): Observable<PlanTratamientoDto> {
    return this.http.patch<PlanTratamientoDto>(`${this.base}/planes/${id}/abono`, { monto });
  }

  // ── Imágenes clínicas ────────────────────────────────────────────────

  getImagenesByPaciente(pacienteId: number): Observable<ImagenClinicaDto[]> {
    return this.http.get<ImagenClinicaDto[]>(`${this.base}/imagenes/paciente/${pacienteId}`);
  }

  subirImagen(dto: ImagenClinicaDto): Observable<ImagenClinicaDto> {
    return this.http.post<ImagenClinicaDto>(`${this.base}/imagenes`, dto);
  }

  eliminarImagen(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/imagenes/${id}`);
  }

  // ── Evoluciones ──────────────────────────────────────────────────────

  getEvolucionesByPaciente(pacienteId: number): Observable<EvolucionOdontologicaDto[]> {
    return this.http.get<EvolucionOdontologicaDto[]>(`${this.base}/evoluciones/paciente/${pacienteId}`);
  }

  registrarEvolucion(dto: EvolucionOdontologicaDto): Observable<EvolucionOdontologicaDto> {
    return this.http.post<EvolucionOdontologicaDto>(`${this.base}/evoluciones`, dto);
  }

  // ── Stats ────────────────────────────────────────────────────────────

  getStats(profesionalId: number): Observable<Record<string, unknown>> {
    return this.http.get<Record<string, unknown>>(`${this.base}/stats/${profesionalId}`);
  }
}
