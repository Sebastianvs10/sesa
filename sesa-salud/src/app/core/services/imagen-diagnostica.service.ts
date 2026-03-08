import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ImagenDiagnosticaDto {
  id: number;
  atencionId: number;
  pacienteId?: number;
  pacienteNombres?: string;
  fechaAtencion?: string;
  tipo?: string;
  resultado?: string;
  urlArchivo?: string;
  createdAt?: string;
}

export interface ImagenDiagnosticaRequestDto {
  atencionId: number;
  tipo?: string;
  resultado?: string;
  urlArchivo?: string;
}

@Injectable({ providedIn: 'root' })
export class ImagenDiagnosticaService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/imagenes-diagnosticas`;

  listByAtencion(atencionId: number): Observable<ImagenDiagnosticaDto[]> {
    const params = new HttpParams().set('atencionId', atencionId).set('page', '0').set('size', '50');
    return this.http.get<ImagenDiagnosticaDto[]>(this.apiUrl, { params });
  }

  /** Listado global con filtros y paginación. */
  listGlobal(filters: {
    pacienteId?: number;
    atencionId?: number;
    tipo?: string;
    fechaDesde?: string;
    fechaHasta?: string;
    page?: number;
    size?: number;
  }): Observable<{ content: ImagenDiagnosticaDto[]; totalElements: number; totalPages: number; number: number; size: number }> {
    let params = new HttpParams().set('page', String(filters.page ?? 0)).set('size', String(filters.size ?? 20));
    if (filters.pacienteId != null) params = params.set('pacienteId', String(filters.pacienteId));
    if (filters.atencionId != null) params = params.set('atencionId', String(filters.atencionId));
    if (filters.tipo?.trim()) params = params.set('tipo', filters.tipo.trim());
    if (filters.fechaDesde) params = params.set('fechaDesde', filters.fechaDesde);
    if (filters.fechaHasta) params = params.set('fechaHasta', filters.fechaHasta);
    return this.http.get(this.apiUrl + '/global', { params }) as Observable<{
      content: ImagenDiagnosticaDto[];
      totalElements: number;
      totalPages: number;
      number: number;
      size: number;
    }>;
  }

  create(request: ImagenDiagnosticaRequestDto): Observable<ImagenDiagnosticaDto> {
    return this.http.post<ImagenDiagnosticaDto>(this.apiUrl, request);
  }
}
