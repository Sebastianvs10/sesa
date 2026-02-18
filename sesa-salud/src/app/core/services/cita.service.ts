import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface CitaDto {
  id: number;
  pacienteId: number;
  pacienteNombre: string;
  profesionalId: number;
  profesionalNombre: string;
  servicio: string;
  fechaHora: string;
  estado: string;
  notas?: string;
}

export interface CitaRequestDto {
  pacienteId: number;
  profesionalId: number;
  servicio: string;
  fechaHora: string;
  estado?: string;
  notas?: string;
}

@Injectable({ providedIn: 'root' })
export class CitaService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/citas`;

  list(fecha?: string): Observable<CitaDto[]> {
    let params = new HttpParams();
    if (fecha) params = params.set('fecha', fecha);
    return this.http.get<CitaDto[]>(this.apiUrl, { params });
  }

  create(request: CitaRequestDto): Observable<CitaDto> {
    return this.http.post<CitaDto>(this.apiUrl, request);
  }

  update(id: number, request: CitaRequestDto): Observable<CitaDto> {
    return this.http.put<CitaDto>(`${this.apiUrl}/${id}`, request);
  }
}
