import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface UsuarioDto {
  id: number;
  email: string;
  nombreCompleto: string;
  activo: boolean;
  roles: string[];
  createdAt?: string;
}

export interface UsuarioRequestDto {
  email: string;
  nombreCompleto: string;
  password?: string;
  activo?: boolean;
  roles?: string[];
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const ROLES_USUARIO = [
  { value: 'SUPERADMINISTRADOR',  label: 'Super Usuario' },
  { value: 'ADMIN',               label: 'Administrador del Sistema' },
  { value: 'MEDICO',              label: 'Médico' },
  { value: 'COORDINADOR_MEDICO',  label: 'Coordinador Médico' },
  { value: 'ODONTOLOGO',          label: 'Odontólogo/a' },
  { value: 'BACTERIOLOGO',        label: 'Bacteriólogo' },
  { value: 'ENFERMERO',           label: 'Enfermero/a' },
  { value: 'JEFE_ENFERMERIA',     label: 'Jefe de Enfermería' },
  { value: 'AUXILIAR_ENFERMERIA', label: 'Auxiliar de Enfermería' },
  { value: 'PSICOLOGO',           label: 'Psicólogo' },
  { value: 'REGENTE_FARMACIA',    label: 'Regente de Farmacia' },
  { value: 'RECEPCIONISTA',       label: 'Recepcionista' },
] as const;

@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/usuarios`;

  list(page = 0, size = 20): Observable<PageResponse<UsuarioDto>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<UsuarioDto>>(this.apiUrl, { params });
  }

  create(request: UsuarioRequestDto): Observable<UsuarioDto> {
    return this.http.post<UsuarioDto>(this.apiUrl, request);
  }

  update(id: number, request: UsuarioRequestDto): Observable<UsuarioDto> {
    return this.http.put<UsuarioDto>(`${this.apiUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
