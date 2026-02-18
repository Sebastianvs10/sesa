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
}
