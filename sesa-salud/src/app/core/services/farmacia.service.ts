import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface FarmaciaMedicamentoDto {
  id: number;
  nombre: string;
  lote?: string;
  fechaVencimiento?: string;
  cantidad: number;
  precio?: number;
  stockMinimo?: number;
  activo: boolean;
}

export interface FarmaciaMedicamentoRequestDto {
  nombre: string;
  lote?: string;
  fechaVencimiento?: string;
  cantidad?: number;
  precio?: number;
  stockMinimo?: number;
  activo?: boolean;
}

export interface FarmaciaDispensacionDto {
  id: number;
  medicamentoId: number;
  medicamentoNombre: string;
  pacienteId: number;
  pacienteNombre: string;
  cantidad: number;
  fechaDispensacion?: string;
  entregadoPor?: string;
}

export interface FarmaciaDispensacionRequestDto {
  medicamentoId: number;
  pacienteId: number;
  cantidad: number;
  entregadoPor?: string;
}

/** Orden clínica tipo MEDICAMENTO (o COMPUESTA con ítems medicamento) pendiente o parcial de dispensar */
export interface OrdenFarmaciaPendienteDto {
  id: number;
  pacienteId: number;
  pacienteNombre: string;
  pacienteDocumento?: string;
  tipoDocumentoPaciente?: string;
  alergiasPaciente?: string;
  detalle?: string;
  cantidadPrescrita?: number;
  unidadMedida?: string;
  frecuencia?: string;
  duracionDias?: number;
  fechaOrden?: string;
  medicoNombre?: string;
  estadoDispensacionFarmacia: string;
  /** Ítems de medicamento en órdenes compuestas (varios en una sola orden). */
  items?: OrdenFarmaciaPendienteItemDto[];
}

export interface OrdenFarmaciaPendienteItemDto {
  id?: number;
  detalle?: string;
  cantidadPrescrita?: number;
  unidadMedida?: string;
  frecuencia?: string;
  duracionDias?: number;
}

export interface LineaDispensacionDto {
  medicamentoId: number;
  lote?: string;
  cantidad: number;
}

export interface DispensarOrdenRequestDto {
  ordenId: number;
  lineas: LineaDispensacionDto[];
}

@Injectable({ providedIn: 'root' })
export class FarmaciaService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/farmacia`;

  listMedicamentos(): Observable<FarmaciaMedicamentoDto[]> {
    const params = new HttpParams().set('page', '0').set('size', '100');
    return this.http.get<FarmaciaMedicamentoDto[]>(`${this.apiUrl}/medicamentos`, { params });
  }

  createMedicamento(request: FarmaciaMedicamentoRequestDto): Observable<FarmaciaMedicamentoDto> {
    return this.http.post<FarmaciaMedicamentoDto>(`${this.apiUrl}/medicamentos`, request);
  }

  dispensar(request: FarmaciaDispensacionRequestDto): Observable<FarmaciaDispensacionDto> {
    return this.http.post<FarmaciaDispensacionDto>(`${this.apiUrl}/dispensar`, request);
  }

  listDispensacionesByPaciente(pacienteId: number): Observable<FarmaciaDispensacionDto[]> {
    const params = new HttpParams().set('page', '0').set('size', '50');
    return this.http.get<FarmaciaDispensacionDto[]>(`${this.apiUrl}/dispensaciones/paciente/${pacienteId}`, { params });
  }

  /** Órdenes clínicas tipo MEDICAMENTO pendientes o parciales de dispensar (desde Historia Clínica) */
  getOrdenesPendientes(): Observable<OrdenFarmaciaPendienteDto[]> {
    const params = new HttpParams().set('page', '0').set('size', '100');
    return this.http.get<OrdenFarmaciaPendienteDto[]>(`${this.apiUrl}/ordenes-pendientes`, { params });
  }

  /** Dispensar una orden médica por ID con varias líneas (medicamento, cantidad) */
  dispensarOrden(request: DispensarOrdenRequestDto): Observable<FarmaciaDispensacionDto[]> {
    return this.http.post<FarmaciaDispensacionDto[]>(`${this.apiUrl}/dispensar-orden`, request);
  }
}
