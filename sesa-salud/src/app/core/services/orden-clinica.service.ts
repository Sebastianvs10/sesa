import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface OrdenClinicaDto {
  id: number;
  pacienteId: number;
  pacienteNombre: string;
  consultaId: number;
  tipo: string;
  detalle?: string;
  cantidadPrescrita?: number;
  unidadMedida?: string;
  frecuencia?: string;
  duracionDias?: number;
  estado?: string;
  resultado?: string;
  fechaResultado?: string;
  resultadoRegistradoPorNombre?: string;
  resultadoRegistradoPorRol?: string;
  valorEstimado?: number;
  createdAt?: string;
  /** Ítems de la orden (varios en una sola orden). */
  items?: OrdenClinicaItemDto[];
}

export interface OrdenClinicaRequestDto {
  pacienteId: number;
  consultaId: number;
  tipo: string;
  detalle?: string;
  cantidadPrescrita?: number;
  unidadMedida?: string;
  frecuencia?: string;
  duracionDias?: number;
  estado?: string;
  valorEstimado?: number;
}

/** Un ítem de una orden (respuesta del backend o para creación por lotes). */
export interface OrdenClinicaItemDto {
  id?: number;
  tipo: string;
  detalle?: string;
  cantidadPrescrita?: number;
  unidadMedida?: string;
  frecuencia?: string;
  duracionDias?: number;
  valorEstimado?: number;
}

export interface OrdenClinicaBatchRequestDto {
  pacienteId: number;
  consultaId: number;
  items: OrdenClinicaItemDto[];
}

@Injectable({ providedIn: 'root' })
export class OrdenClinicaService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/ordenes-clinicas`;

  listByPaciente(pacienteId: number, page = 0, size = 20): Observable<OrdenClinicaDto[]> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<OrdenClinicaDto[]>(`${this.apiUrl}/paciente/${pacienteId}`, { params });
  }

  create(request: OrdenClinicaRequestDto): Observable<OrdenClinicaDto> {
    return this.http.post<OrdenClinicaDto>(this.apiUrl, request);
  }

  createBatch(batch: OrdenClinicaBatchRequestDto): Observable<OrdenClinicaDto> {
    return this.http.post<OrdenClinicaDto>(`${this.apiUrl}/batch`, batch);
  }

  update(id: number, request: OrdenClinicaRequestDto): Observable<OrdenClinicaDto> {
    return this.http.put<OrdenClinicaDto>(`${this.apiUrl}/${id}`, request);
  }

  registrarResultado(ordenId: number, resultado: string): Observable<OrdenClinicaDto> {
    return this.http.patch<OrdenClinicaDto>(`${this.apiUrl}/${ordenId}/resultado`, { resultado });
  }
}
