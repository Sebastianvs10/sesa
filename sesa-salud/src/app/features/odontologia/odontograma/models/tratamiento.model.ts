/**
 * Modelo de tratamiento por superficie para historial clínico del odontograma.
 * Autor: Ing. J Sebastian Vargas S
 */

import type { EstadoDental } from '../../odontologia.models';

export interface Tratamiento {
  tipo: EstadoDental;
  fecha: string; // ISO
  profesional: string;
  observacion?: string;
}

export interface TratamientoPorSuperficie {
  superficie: string; // M | D | V | L | O
  tratamientos: Tratamiento[];
}
