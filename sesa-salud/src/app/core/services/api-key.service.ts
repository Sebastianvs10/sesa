/**
 * S12: Servicio para gestión de API Keys (integradores).
 * Autor: Ing. J Sebastian Vargas S
 */
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ApiKeyResponse {
  id: number;
  nombreIntegrador: string;
  permisos: string;
  activo: boolean;
  createdAt: string;
}

export interface ApiKeyCreateResponse {
  id: number;
  nombreIntegrador: string;
  /** Solo al crear; no se vuelve a mostrar. */
  apiKeyRaw: string;
  permisos: string;
}

@Injectable({ providedIn: 'root' })
export class ApiKeyService {
  private readonly base = `${environment.apiUrl}/api-keys`;

  constructor(private http: HttpClient) {}

  listar(): Observable<ApiKeyResponse[]> {
    return this.http.get<ApiKeyResponse[]>(this.base);
  }

  crear(nombreIntegrador: string, permisos: string): Observable<ApiKeyCreateResponse> {
    return this.http.post<ApiKeyCreateResponse>(this.base, {
      nombreIntegrador: nombreIntegrador || 'Integrador',
      permisos: permisos || 'LABORATORIO',
    });
  }

  desactivar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
