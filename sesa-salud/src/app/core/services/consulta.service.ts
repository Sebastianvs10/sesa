import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ConsultaDto {
  id: number;
  pacienteId: number;
  pacienteNombre: string;
  profesionalId?: number;
  profesionalNombre?: string;
  citaId?: number;
  motivoConsulta?: string;
  enfermedadActual?: string;
  antecedentesPersonales?: string;
  antecedentesFamiliares?: string;
  alergias?: string;
  fechaConsulta?: string;
}

export interface ConsultaRequestDto {
  pacienteId: number;
  profesionalId?: number;
  citaId?: number;
  motivoConsulta?: string;
  enfermedadActual?: string;
  antecedentesPersonales?: string;
  antecedentesFamiliares?: string;
  alergias?: string;
}

@Injectable({ providedIn: 'root' })
export class ConsultaService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/consultas`;

  listByPaciente(pacienteId: number, page = 0, size = 20): Observable<ConsultaDto[]> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ConsultaDto[]>(`${this.apiUrl}/paciente/${pacienteId}`, { params });
  }

  listMisConsultas(page = 0, size = 50): Observable<ConsultaDto[]> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ConsultaDto[]>(`${this.apiUrl}/mis-consultas`, { params });
  }

  create(request: ConsultaRequestDto): Observable<ConsultaDto> {
    return this.http.post<ConsultaDto>(this.apiUrl, request);
  }
}
