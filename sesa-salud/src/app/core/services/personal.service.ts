import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

const HEADER_TENANT_SCHEMA = 'X-Tenant-Schema';

export interface PersonalDto {
  id: number;
  nombres: string;
  apellidos?: string;
  cargo: string;
  servicio?: string;
  turno?: string;
  identificacion?: string;
  primerNombre?: string;
  segundoNombre?: string;
  primerApellido?: string;
  segundoApellido?: string;
  celular?: string;
  email?: string;
  rol?: string;
  institucionPrestadora?: string;
  fotoUrl?: string;
  firmaUrl?: string;
  activo: boolean;
  createdAt?: string;
}

export interface PersonalRequestDto {
  nombres: string;
  apellidos?: string;
  cargo: string;
  servicio?: string;
  turno?: string;
  identificacion?: string;
  primerNombre?: string;
  segundoNombre?: string;
  primerApellido?: string;
  segundoApellido?: string;
  celular?: string;
  email?: string;
  password?: string;
  rol?: string;
  institucionPrestadora?: string;
  activo: boolean;
}

/** Roles para profesionales de la salud. Super Administrador y Administrador deshabilitados en gestión de personal. */
export const ROLES_PERSONAL: { value: string; label: string; disabled?: boolean }[] = [
  { value: 'SUPERADMINISTRADOR', label: 'Super Administrador', disabled: true },
  { value: 'ADMIN', label: 'Administrador del Sistema', disabled: true },
  { value: 'MEDICO', label: 'Médico' },
  { value: 'ODONTOLOGO', label: 'Odontólogo/a' },
  { value: 'BACTERIOLOGO', label: 'Bacteriólogo' },
  { value: 'ENFERMERO', label: 'Enfermero/a' },
  { value: 'JEFE_ENFERMERIA', label: 'Jefe de Enfermería' },
  { value: 'AUXILIAR_ENFERMERIA', label: 'Auxiliar de Enfermería' },
  { value: 'PSICOLOGO', label: 'Psicólogo' },
  { value: 'REGENTE_FARMACIA', label: 'Regente de Farmacia' },
  { value: 'RECEPCIONISTA', label: 'Recepcionista' },
];

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({ providedIn: 'root' })
export class PersonalService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/personal`;

  private optionsWithSchema(schema?: string | null): { headers?: HttpHeaders } {
    if (schema != null && schema.trim() !== '') {
      return {
        headers: new HttpHeaders().set(HEADER_TENANT_SCHEMA, schema.trim()),
      };
    }
    return {};
  }

  list(page = 0, size = 20, q?: string, schema?: string | null): Observable<PageResponse<PersonalDto>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (q != null && q.trim() !== '') {
      params = params.set('q', q.trim());
    }
    if (schema != null && schema.trim() !== '') {
      params = params.set('schema', schema.trim());
    }
    const opts = this.optionsWithSchema(schema);
    return this.http.get<PageResponse<PersonalDto>>(this.apiUrl, { params, ...opts });
  }

  get(id: number, schema?: string | null): Observable<PersonalDto> {
    return this.http.get<PersonalDto>(`${this.apiUrl}/${id}`, this.optionsWithSchema(schema));
  }

  create(request: PersonalRequestDto, schema?: string | null): Observable<PersonalDto> {
    return this.http.post<PersonalDto>(this.apiUrl, request, this.optionsWithSchema(schema));
  }

  update(id: number, request: PersonalRequestDto, schema?: string | null): Observable<PersonalDto> {
    return this.http.put<PersonalDto>(`${this.apiUrl}/${id}`, request, this.optionsWithSchema(schema));
  }

  delete(id: number, schema?: string | null): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, this.optionsWithSchema(schema));
  }

  uploadFoto(id: number, file: File, schema?: string | null): Observable<void> {
    const form = new FormData();
    form.append('file', file);
    return this.http.request<void>('POST', `${this.apiUrl}/${id}/foto`, {
      body: form,
      ...this.optionsWithSchema(schema),
    });
  }

  uploadFirma(id: number, file: File, schema?: string | null): Observable<void> {
    const form = new FormData();
    form.append('file', file);
    return this.http.request<void>('POST', `${this.apiUrl}/${id}/firma`, {
      body: form,
      ...this.optionsWithSchema(schema),
    });
  }

  getFotoBlob(id: number, schema?: string | null): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/foto`, {
      responseType: 'blob',
      ...this.optionsWithSchema(schema),
    });
  }

  getFirmaBlob(id: number, schema?: string | null): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/firma`, {
      responseType: 'blob',
      ...this.optionsWithSchema(schema),
    });
  }
}
