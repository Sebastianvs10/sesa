/**
 * Servicio de plantillas SOAP para evolución en historia clínica (Res. 1995/1999).
 * Autor: Ing. J Sebastian Vargas S
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface PlantillaSoapDto {
  id: number;
  nombre: string;
  motivoTipo?: string;
  contenidoSubjetivo?: string;
  contenidoObjetivo?: string;
  contenidoAnalisis?: string;
  contenidoPlan?: string;
  codigoCie10Sugerido?: string;
  activo?: boolean;
  createdAt?: string;
}

/** Plantillas por defecto cuando el backend no tiene datos (Res. 1995/1999 — contenido mínimo). */
export const PLANTILLAS_SOAP_DEFAULT: PlantillaSoapDto[] = [
  {
    id: 0,
    nombre: 'Control',
    motivoTipo: 'CONTROL',
    contenidoSubjetivo: 'Paciente en control. Refiere evolución favorable / estable.',
    contenidoObjetivo: 'Estado general bueno. Signos vitales estables.',
    contenidoAnalisis: 'Control de patología de base.',
    contenidoPlan: 'Continuar manejo. Próximo control.',
    codigoCie10Sugerido: 'Z00.0',
  },
  {
    id: -1,
    nombre: 'Primera vez',
    motivoTipo: 'PRIMERA_VEZ',
    contenidoSubjetivo: '',
    contenidoObjetivo: '',
    contenidoAnalisis: '',
    contenidoPlan: '',
    codigoCie10Sugerido: '',
  },
  {
    id: -2,
    nombre: 'Enfermedad aguda',
    motivoTipo: 'SEGUIMIENTO_AGUDO',
    contenidoSubjetivo: 'Paciente refiere: [describir síntomas y evolución].',
    contenidoObjetivo: 'Hallazgos al examen físico y signos vitales.',
    contenidoAnalisis: 'Impresión diagnóstica.',
    contenidoPlan: 'Manejo, medicación, órdenes e indicaciones.',
    codigoCie10Sugerido: '',
  },
];

@Injectable({ providedIn: 'root' })
export class PlantillaSoapService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/plantillas-soap`;

  listarActivas(): Observable<PlantillaSoapDto[]> {
    return this.http.get<PlantillaSoapDto[]>(this.apiUrl).pipe(
      catchError(() => of([])),
      map((list) => (list?.length ? list : PLANTILLAS_SOAP_DEFAULT)),
    );
  }

  getById(id: number): Observable<PlantillaSoapDto | null> {
    if (id <= 0) {
      const found = PLANTILLAS_SOAP_DEFAULT.find((p) => p.id === id);
      return of(found ?? null);
    }
    return this.http.get<PlantillaSoapDto>(`${this.apiUrl}/${id}`).pipe(
      catchError(() => of(null)),
    );
  }
}
