import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface FacturaDto {
  id: number;
  pacienteId: number;
  pacienteNombre: string;
  ordenId?: number;
  valorTotal: number;
  estado?: string;
  descripcion?: string;
  fechaFactura?: string;
}

export interface FacturaRequestDto {
  pacienteId: number;
  ordenId?: number;
  valorTotal: number;
  estado?: string;
  descripcion?: string;
}

@Injectable({ providedIn: 'root' })
export class FacturaService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/facturas`;

  listByPaciente(pacienteId: number, page = 0, size = 20): Observable<FacturaDto[]> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<FacturaDto[]>(`${this.apiUrl}/paciente/${pacienteId}`, { params });
  }

  create(request: FacturaRequestDto): Observable<FacturaDto> {
    return this.http.post<FacturaDto>(this.apiUrl, request);
  }

  /** Exporta RIPS en CSV para el rango de fechas. */
  exportRips(desde: string, hasta: string): Observable<string> {
    const params = new HttpParams().set('desde', desde).set('hasta', hasta);
    return this.http.get(`${this.apiUrl}/rips`, { params, responseType: 'text' }) as Observable<string>;
  }
}
