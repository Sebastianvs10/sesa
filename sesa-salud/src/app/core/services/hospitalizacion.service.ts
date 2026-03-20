import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface HospitalizacionDto {
  id: number;
  pacienteId: number;
  pacienteNombre: string;
  pacienteDocumento?: string;
  servicio?: string;
  cama?: string;
  estado?: string;
  fechaIngreso?: string;
  fechaEgreso?: string;
  evolucionDiaria?: string;
  ordenesMedicas?: string;
  epicrisis?: string;
}

export interface HospitalizacionRequestDto {
  pacienteId: number;
  servicio?: string;
  cama?: string;
  estado?: string;
  evolucionDiaria?: string;
  ordenesMedicas?: string;
  epicrisis?: string;
}

@Injectable({ providedIn: 'root' })
export class HospitalizacionService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/hospitalizaciones`;

  listAll(size = 200): Observable<HospitalizacionDto[]> {
    const params = new HttpParams().set('page', '0').set('size', String(size));
    return this.http.get<HospitalizacionDto[]>(this.apiUrl, { params });
  }

  listByEstado(estado?: string, size = 200): Observable<HospitalizacionDto[]> {
    let params = new HttpParams().set('page', '0').set('size', String(size));
    if (estado?.trim()) params = params.set('estado', estado.trim());
    return this.http.get<HospitalizacionDto[]>(this.apiUrl, { params });
  }

  listByPaciente(pacienteId: number): Observable<HospitalizacionDto[]> {
    const params = new HttpParams().set('page', '0').set('size', '50');
    return this.http.get<HospitalizacionDto[]>(`${this.apiUrl}/paciente/${pacienteId}`, { params });
  }

  create(request: HospitalizacionRequestDto): Observable<HospitalizacionDto> {
    return this.http.post<HospitalizacionDto>(this.apiUrl, request);
  }

  update(id: number, request: HospitalizacionRequestDto): Observable<HospitalizacionDto> {
    return this.http.put<HospitalizacionDto>(`${this.apiUrl}/${id}`, request);
  }
}
