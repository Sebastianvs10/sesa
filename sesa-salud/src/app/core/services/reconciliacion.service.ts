/**
 * S5: Reconciliación de medicamentos y alergias por atención.
 * Autor: Ing. J Sebastian Vargas S
 */
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ReconciliacionAtencionDto {
  id?: number;
  atencionId: number;
  profesionalId?: number;
  nombreProfesional?: string;
  medicamentosReferidos: string[];
  medicamentosHc: string[];
  alergiasReferidas: string[];
  alergiasHc: string[];
  reconciliadoAt?: string;
  observaciones?: string;
}

export interface ReconciliacionAtencionRequestDto {
  medicamentosReferidos?: string[];
  alergiasReferidas?: string[];
  observaciones?: string;
}

@Injectable({ providedIn: 'root' })
export class ReconciliacionService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/atenciones`;

  getByAtencionId(atencionId: number): Observable<ReconciliacionAtencionDto> {
    return this.http.get<ReconciliacionAtencionDto>(`${this.apiUrl}/${atencionId}/reconciliacion`);
  }

  guardar(atencionId: number, request: ReconciliacionAtencionRequestDto): Observable<ReconciliacionAtencionDto> {
    return this.http.post<ReconciliacionAtencionDto>(`${this.apiUrl}/${atencionId}/reconciliacion`, request);
  }
}
