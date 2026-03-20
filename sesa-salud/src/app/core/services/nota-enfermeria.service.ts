/**
 * Servicio de Notas de Enfermería — registro de notas clínicas de enfermería.
 * Autor: Ing. J Sebastian Vargas S
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface NotaEnfermeriaDto {
  id: number;
  atencionId: number;
  nota: string;
  fechaNota: string;
  profesionalId?: number;
  profesionalNombre?: string;
  createdAt?: string;
}

export interface NotaEnfermeriaRequestDto {
  atencionId: number;
  nota: string;
  fechaNota?: string;
  profesionalId?: number;
}

@Injectable({ providedIn: 'root' })
export class NotaEnfermeriaService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/notas-enfermeria`;

  listarPorAtencion(atencionId: number): Observable<NotaEnfermeriaDto[]> {
    const params = new HttpParams()
      .set('atencionId', atencionId.toString())
      .set('size', '100');
    return this.http.get<NotaEnfermeriaDto[]>(this.apiUrl, { params });
  }

  crear(dto: NotaEnfermeriaRequestDto): Observable<NotaEnfermeriaDto> {
    return this.http.post<NotaEnfermeriaDto>(this.apiUrl, dto);
  }

  actualizar(id: number, dto: Partial<NotaEnfermeriaRequestDto>): Observable<NotaEnfermeriaDto> {
    return this.http.put<NotaEnfermeriaDto>(`${this.apiUrl}/${id}`, dto);
  }
}
