/**
 * Servicio para gestión de roles del sistema (SUPERADMINISTRADOR).
 * Autor: Ing. J Sebastian Vargas S
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface RolDto {
  codigo: string;
  nombre: string;
  modulos: string[];
}

@Injectable({ providedIn: 'root' })
export class RolesService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  list(): Observable<RolDto[]> {
    return this.http.get<RolDto[]>(`${this.apiUrl}/roles`);
  }

  updateModulos(rol: string, modulos: string[]): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/roles/${rol}/modulos`, { modulos });
  }

  create(codigo: string, nombre: string): Observable<RolDto> {
    return this.http.post<RolDto>(`${this.apiUrl}/roles`, { codigo, nombre });
  }
}
