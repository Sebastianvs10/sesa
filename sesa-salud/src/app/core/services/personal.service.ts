import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

const HEADER_TENANT_SCHEMA = 'X-Tenant-Schema';

export interface PersonalDto {
  id: number;
  nombres: string;
  apellidos?: string;
  /** Tipo de documento (CC, CE, PA, PEP, TI, RC — Res. 3374/2000). */
  tipoDocumento?: string;
  identificacion?: string;
  primerNombre?: string;
  segundoNombre?: string;
  primerApellido?: string;
  segundoApellido?: string;
  celular?: string;
  /** Correo profesional para documentos clínicos. */
  email?: string;
  /** Rol primario (compatibilidad). */
  rol?: string;
  /** Todos los roles profesionales asignados (multi-rol). */
  roles?: string[];
  fotoUrl?: string;
  firmaUrl?: string;
  activo: boolean;
  createdAt?: string;
  // ── Normativos Res. 2003/2014, Ley 23/1981, Res. 1449/2016 ──
  tarjetaProfesional?: string;
  especialidadFormal?: string;
  numeroRethus?: string;
  // ── Demográficos RIPS Res. 3374/2000 ──
  fechaNacimiento?: string;
  sexo?: string;
  // ── Lugar de práctica Res. 2003/2014 ──
  municipio?: string;
  departamento?: string;
  // ── Vínculo laboral Circular 047/2007 ──
  tipoVinculacion?: string;
  fechaIngreso?: string;
  fechaRetiro?: string;
}

export interface PersonalRequestDto {
  nombres: string;
  apellidos?: string;
  /** Tipo de documento (CC, CE, PA, PEP, TI, RC). */
  tipoDocumento?: string;
  identificacion?: string;
  primerNombre?: string;
  segundoNombre?: string;
  primerApellido?: string;
  segundoApellido?: string;
  celular?: string;
  email?: string;
  password?: string;
  /** Rol primario (compatibilidad con campo simple). */
  rol?: string;
  /** Multi-rol: lista de roles profesionales asignados. Tiene prioridad sobre `rol`. */
  roles?: string[];
  activo: boolean;
  // ── Normativos ──
  tarjetaProfesional?: string;
  especialidadFormal?: string;
  numeroRethus?: string;
  // ── Demográficos ──
  fechaNacimiento?: string;
  sexo?: string;
  // ── Lugar de práctica ──
  municipio?: string;
  departamento?: string;
  // ── Vínculo laboral ──
  tipoVinculacion?: string;
  fechaIngreso?: string;
  fechaRetiro?: string;
}

/** Roles para profesionales de la salud. Super Administrador y Administrador deshabilitados en gestión de personal. */
export const ROLES_PERSONAL: { value: string; label: string; disabled?: boolean }[] = [
  { value: 'SUPERADMINISTRADOR', label: 'Super Administrador', disabled: true },
  { value: 'ADMIN', label: 'Administrador del Sistema', disabled: true },
  { value: 'MEDICO', label: 'Médico' },
  { value: 'COORDINADOR_MEDICO', label: 'Coordinador Médico' },
  { value: 'EBS', label: 'Profesional EBS' },
  { value: 'COORDINADOR_TERRITORIAL', label: 'Coordinador Territorial' },
  { value: 'SUPERVISOR_APS', label: 'Supervisor APS' },
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
