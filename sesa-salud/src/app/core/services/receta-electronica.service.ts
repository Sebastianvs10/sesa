/**
 * Receta electrónica con token verificable (anti-falsificación) y QR.
 * El backend firma la receta y devuelve un token; la verificación es pública por token.
 * Autor: Ing. J Sebastian Vargas S
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { FormulaMedicaDto } from './atencion.service';

export interface RecetaElectronicaDto {
  id: number;
  tokenVerificacion: string;
  urlVerificacion: string;
  atencionId?: number;
  consultaId?: number;
  pacienteNombre: string;
  pacienteDocumento?: string;
  medicoNombre: string;
  medicoTarjetaProfesional?: string;
  fechaEmision: string;
  medicamentos: FormulaMedicaDto[];
  diagnostico?: string;
  observaciones?: string;
  validaHasta?: string;
}

export interface RecetaVerificacionResponseDto {
  valida: boolean;
  mensaje?: string;
  receta?: {
    pacienteNombre: string;
    medicoNombre: string;
    fechaEmision: string;
    medicamentos: { medicamento: string; dosis?: string; frecuencia?: string; duracion?: string }[];
    diagnostico?: string;
  };
}

export interface CrearRecetaRequestDto {
  atencionId: number;
  observaciones?: string;
}

@Injectable({ providedIn: 'root' })
export class RecetaElectronicaService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/recetas`;

  /** Crea una receta electrónica a partir de una atención (con fórmulas ya guardadas). */
  crear(atencionId: number, observaciones?: string): Observable<RecetaElectronicaDto> {
    return this.http.post<RecetaElectronicaDto>(this.apiUrl, { atencionId, observaciones } as CrearRecetaRequestDto);
  }

  /** Crea receta con lista de medicamentos (si el backend acepta payload directo). */
  crearConFormulas(
    pacienteId: number,
    consultaId: number | undefined,
    medicamentos: FormulaMedicaDto[],
    medicoNombre: string,
    pacienteNombre: string,
    diagnostico?: string,
    observaciones?: string
  ): Observable<RecetaElectronicaDto> {
    return this.http.post<RecetaElectronicaDto>(`${this.apiUrl}/crear-con-formulas`, {
      pacienteId,
      consultaId,
      medicamentos,
      medicoNombre,
      pacienteNombre,
      diagnostico,
      observaciones,
    });
  }

  /** Verificación pública por token (sin auth). */
  verificar(token: string): Observable<RecetaVerificacionResponseDto> {
    return this.http.get<RecetaVerificacionResponseDto>(`${this.apiUrl}/verificar/${encodeURIComponent(token)}`);
  }

  /** Construye la URL de verificación para esta app (para QR y enlace). */
  getUrlVerificacion(token: string): string {
    const base = typeof window !== 'undefined' ? window.location.origin : '';
    return `${base}/verificar-receta?t=${encodeURIComponent(token)}`;
  }
}
