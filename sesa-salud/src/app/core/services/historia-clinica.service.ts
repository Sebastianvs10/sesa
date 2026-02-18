import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface HistoriaClinicaDto {
  id: number;
  pacienteId: number;
  pacienteNombre: string;
  pacienteDocumento: string;
  fechaApertura: string;
  estado: string;
  grupoSanguineo?: string;
  alergiasGenerales?: string;
  antecedentesPersonales?: string;
  antecedentesFamiliares?: string;
}

export interface HistoriaClinicaRequestDto {
  estado?: string;
  grupoSanguineo?: string;
  alergiasGenerales?: string;
  antecedentesPersonales?: string;
  antecedentesQuirurgicos?: string;
  antecedentesFarmacologicos?: string;
  antecedentesTraumaticos?: string;
  antecedentesGinecoobstetricos?: string;
  antecedentesFamiliares?: string;
  habitosTabaco?: boolean;
  habitosAlcohol?: boolean;
  habitosSustancias?: boolean;
  habitosDetalles?: string;
}

export interface CrearHistoriaCompletaRequestDto {
  /** Si se envía, se usa este profesional para la primera atención; si no, el asociado al usuario logueado. */
  profesionalId?: number;
  grupoSanguineo?: string;
  alergiasGenerales?: string;
  antecedentesPersonales?: string;
  antecedentesQuirurgicos?: string;
  antecedentesFarmacologicos?: string;
  antecedentesTraumaticos?: string;
  antecedentesGinecoobstetricos?: string;
  antecedentesFamiliares?: string;
  habitosTabaco?: boolean;
  habitosAlcohol?: boolean;
  habitosSustancias?: boolean;
  habitosDetalles?: string;
  motivoConsulta: string;
  enfermedadActual: string;
  versionEnfermedad?: string;
  sintomasAsociados?: string;
  factoresMejoran?: string;
  factoresEmpeoran?: string;
  revisionSistemas?: string;
  presionArterial?: string;
  frecuenciaCardiaca?: string;
  frecuenciaRespiratoria?: string;
  temperatura?: string;
  peso?: string;
  talla?: string;
  imc?: string;
  evaluacionGeneral?: string;
  hallazgos?: string;
  diagnostico?: string;
  codigoCie10?: string;
  planTratamiento?: string;
  tratamientoFarmacologico?: string;
  ordenesMedicas?: string;
  examenesSolicitados?: string;
  incapacidad?: string;
  recomendaciones?: string;
}

@Injectable({ providedIn: 'root' })
export class HistoriaClinicaService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/historia-clinica`;

  getByPaciente(pacienteId: number): Observable<HistoriaClinicaDto> {
    return this.http.get<HistoriaClinicaDto>(`${this.apiUrl}/paciente/${pacienteId}`);
  }

  getByPacienteOrNull(pacienteId: number): Observable<HistoriaClinicaDto | null> {
    return this.http.get<HistoriaClinicaDto>(`${this.apiUrl}/paciente/${pacienteId}`).pipe(
      catchError(() => of(null))
    );
  }

  create(pacienteId: number, dto: HistoriaClinicaRequestDto): Observable<HistoriaClinicaDto> {
    return this.http.post<HistoriaClinicaDto>(`${this.apiUrl}/paciente/${pacienteId}`, dto);
  }

  createCompleta(pacienteId: number, dto: CrearHistoriaCompletaRequestDto): Observable<HistoriaClinicaDto> {
    return this.http.post<HistoriaClinicaDto>(`${this.apiUrl}/paciente/${pacienteId}/completa`, dto);
  }

  update(id: number, dto: HistoriaClinicaRequestDto): Observable<HistoriaClinicaDto> {
    return this.http.put<HistoriaClinicaDto>(`${this.apiUrl}/${id}`, dto);
  }
}
