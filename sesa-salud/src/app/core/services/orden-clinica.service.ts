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
  estado?: string;
  valorEstimado?: number;
  createdAt?: string;
}

export interface OrdenClinicaRequestDto {
  pacienteId: number;
  consultaId: number;
  tipo: string;
  detalle?: string;
  estado?: string;
  valorEstimado?: number;
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
}
