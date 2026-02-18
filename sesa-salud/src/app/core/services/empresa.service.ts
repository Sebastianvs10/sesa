import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface EmpresaDto {
  id: number;
  schemaName: string;
  razonSocial: string;
  telefono?: string;
  segundoTelefono?: string;
  identificacion?: string;
  direccionEmpresa?: string;
  tipoDocumento?: string;
  regimen?: string;
  numeroDivipola?: string;
  pais?: string;
  departamento?: string;
  municipio?: string;
  imagenUrl?: string;
  adminCorreo?: string;
  adminIdentificacion?: string;
  adminPrimerNombre?: string;
  adminSegundoNombre?: string;
  adminPrimerApellido?: string;
  adminSegundoApellido?: string;
  adminCelular?: string;
  adminProveedorServicio?: string;
  usuarioMovilLimit?: number;
  usuarioWebLimit?: number;
  activo?: boolean;
  createdAt?: string;
  moduloCodigos?: string[];
  submoduloCodigos?: string[];
}

export interface AdminUserRequest {
  identificacion: string;
  primerNombre: string;
  segundoNombre?: string;
  primerApellido: string;
  segundoApellido?: string;
  telefonoCelular: string;
  proveedorServicio?: string;
  correo: string;
  contraseña: string;
}

export interface EmpresaCreateRequest {
  schemaName: string;
  razonSocial: string;
  telefono?: string;
  segundoTelefono?: string;
  identificacion?: string;
  direccionEmpresa?: string;
  tipoDocumento?: string;
  regimen?: string;
  numeroDivipola?: string;
  pais?: string;
  departamento?: string;
  municipio?: string;
  imagenUrl?: string;
  moduloCodigos?: string[];
  submoduloCodigos?: string[];
  usuarioMovilLimit?: number;
  usuarioWebLimit?: number;
  adminUser: AdminUserRequest;
}

export interface SubmoduloDto {
  id: number;
  codigo: string;
  nombre: string;
}

export interface ModuloDto {
  id: number;
  codigo: string;
  nombre: string;
  submodulos: SubmoduloDto[];
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({ providedIn: 'root' })
export class EmpresaService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/empresas`;

  list(page = 0, size = 20): Observable<PageResponse<EmpresaDto>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<EmpresaDto>>(this.apiUrl, { params });
  }

  get(id: number): Observable<EmpresaDto> {
    return this.http.get<EmpresaDto>(`${this.apiUrl}/${id}`);
  }

  create(request: EmpresaCreateRequest): Observable<EmpresaDto> {
    return this.http.post<EmpresaDto>(this.apiUrl, request);
  }

  update(id: number, request: EmpresaCreateRequest): Observable<EmpresaDto> {
    return this.http.put<EmpresaDto>(`${this.apiUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /** Empresa del tenant actual (para mostrar nombre y logo en el shell). */
  getCurrent(): Observable<EmpresaDto> {
    return this.http.get<EmpresaDto>(`${this.apiUrl}/current`);
  }

  /** Logo de la empresa actual como blob (para crear object URL en el frontend). */
  getLogoBlob(): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/logo`, { responseType: 'blob' });
  }

  /** Subir logo de la empresa del tenant actual. Solo ADMIN. */
  uploadLogo(file: File): Observable<void> {
    const form = new FormData();
    form.append('file', file);
    return this.http.request<void>('POST', `${this.apiUrl}/logo`, { body: form });
  }

  /** Obtener catálogo de módulos con submódulos. */
  getModulos(): Observable<ModuloDto[]> {
    return this.http.get<ModuloDto[]>(`${environment.apiUrl}/modulos`);
  }
}
