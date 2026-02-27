/**
 * Modelos de diente y superficie para el odontograma interactivo (FDI).
 * Compatible con odontologia.models (PiezaDental, Superficie, EstadoDental).
 * Autor: Ing. J Sebastian Vargas S
 */

import type { Tratamiento } from './tratamiento.model';

/** Código corto de superficie FDI: M=Mesial, D=Distal, V=Vestibular, L=Lingual, O=Oclusal/Incisal */
export type SuperficieCodigo = 'M' | 'D' | 'V' | 'L' | 'O';

export interface SuperficieEstado {
  nombre: SuperficieCodigo;
  estado?: string;
  color?: string;
}

export type TipoDiente = 'incisivo' | 'canino' | 'premolar' | 'molar';
export type Arcada = 'superior' | 'inferior';

export interface Diente {
  numero: number; // FDI
  tipo: TipoDiente;
  arcada: Arcada;
  superficies: SuperficieEstado[];
  tratamientos: Tratamiento[];
  ausente?: boolean;
  implante?: boolean;
}

/** Mapeo superficie larga (odontologia.models) → código corto */
export const SUPERFICIE_TO_CODIGO: Record<string, SuperficieCodigo> = {
  MESIAL: 'M',
  DISTAL: 'D',
  VESTIBULAR: 'V',
  LINGUAL: 'L',
  OCLUSAL: 'O',
  GENERAL: 'O',
};

export const CODIGO_TO_SUPERFICIE: Record<SuperficieCodigo, string> = {
  M: 'MESIAL',
  D: 'DISTAL',
  V: 'VESTIBULAR',
  L: 'LINGUAL',
  O: 'OCLUSAL',
};

/** Obtiene tipo de diente según FDI (posición 1-8 en cuadrante) */
export function tipoDienteFromFdi(fdi: number): TipoDiente {
  const n = fdi % 10;
  if (n >= 1 && n <= 2) return 'incisivo';
  if (n === 3) return 'canino';
  if (n >= 4 && n <= 5) return 'premolar';
  return 'molar';
}

/** true si la pieza pertenece a arcada superior (FDI 11-28 o 51-65) */
export function esArcadaSuperior(fdi: number): boolean {
  return (fdi >= 11 && fdi <= 28) || (fdi >= 51 && fdi <= 65);
}
