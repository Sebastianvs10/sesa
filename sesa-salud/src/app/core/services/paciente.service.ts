import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface PacienteDto {
  id: number;
  tipoDocumento?: string;
  documento: string;
  nombres: string;
  apellidos?: string;
  fechaNacimiento?: string;
  sexo?: string;
  grupoSanguineo?: string;
  telefono?: string;
  email?: string;
  direccion?: string;
  epsId?: number;
  epsNombre?: string;
  activo: boolean;
  createdAt?: string;
}

export interface PacienteRequestDto {
  tipoDocumento?: string;
  documento: string;
  nombres: string;
  apellidos?: string;
  fechaNacimiento?: string;
  sexo?: string;
  grupoSanguineo?: string;
  telefono?: string;
  email?: string;
  direccion?: string;
  epsId?: number;
  activo: boolean;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({ providedIn: 'root' })
export class PacienteService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/pacientes`;

  list(page = 0, size = 20, q?: string): Observable<PageResponse<PacienteDto>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (q?.trim()) params = params.set('q', q.trim());
    return this.http.get<PageResponse<PacienteDto>>(this.apiUrl, { params });
  }

  get(id: number): Observable<PacienteDto> {
    return this.http.get<PacienteDto>(`${this.apiUrl}/${id}`);
  }

  create(request: PacienteRequestDto): Observable<PacienteDto> {
    return this.http.post<PacienteDto>(this.apiUrl, request);
  }

  update(id: number, request: PacienteRequestDto): Observable<PacienteDto> {
    return this.http.put<PacienteDto>(`${this.apiUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
