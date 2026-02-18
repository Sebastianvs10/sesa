import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface LaboratorioSolicitudDto {
  id: number;
  pacienteId: number;
  pacienteNombre: string;
  solicitanteId?: number;
  solicitanteNombre?: string;
  tipoPrueba: string;
  estado: string;
  fechaSolicitud: string;
}

interface PageResponse<T> {
  content: T[];
}

@Injectable({ providedIn: 'root' })
export class LaboratorioSolicitudService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/laboratorio-solicitudes`;

  list(estado?: string): Observable<PageResponse<LaboratorioSolicitudDto>> {
    let params = new HttpParams().set('page', '0').set('size', '50');
    if (estado?.trim()) params = params.set('estado', estado.trim());
    return this.http.get<PageResponse<LaboratorioSolicitudDto>>(this.apiUrl, { params });
  }
}
