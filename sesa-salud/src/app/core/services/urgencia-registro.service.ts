import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface UrgenciaRegistroDto {
  id: number;
  pacienteId: number;
  pacienteNombre: string;
  nivelTriage: string;
  estado: string;
  fechaHoraIngreso: string;
  observaciones?: string;
}

interface PageResponse<T> {
  content: T[];
}

@Injectable({ providedIn: 'root' })
export class UrgenciaRegistroService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/urgencias`;

  list(estado?: string): Observable<PageResponse<UrgenciaRegistroDto>> {
    let params = new HttpParams().set('page', '0').set('size', '50');
    if (estado?.trim()) params = params.set('estado', estado.trim());
    return this.http.get<PageResponse<UrgenciaRegistroDto>>(this.apiUrl, { params });
  }
}
