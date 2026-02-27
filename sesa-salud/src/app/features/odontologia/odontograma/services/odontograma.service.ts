/**
 * Servicio de estado del odontograma: vista, tratamiento seleccionado, historial.
 * Autor: Ing. J Sebastian Vargas S
 */

import { Injectable, signal, computed } from '@angular/core';
import type { EstadoDental } from '../../odontologia.models';
import type { Tratamiento } from '../models/tratamiento.model';

export type TipoVista =
  | 'frontal'
  | 'oclusal'
  | 'lateral_derecha'
  | 'lateral_izquierda'
  | 'arcada_superior'
  | 'arcada_inferior';

export const VISTAS_LABEL: Record<TipoVista, string> = {
  frontal: 'Vista frontal',
  oclusal: 'Vista oclusal',
  lateral_derecha: 'Lateral derecha',
  lateral_izquierda: 'Lateral izquierda',
  arcada_superior: 'Arcada superior',
  arcada_inferior: 'Arcada inferior',
};

/** Acciones de la toolbar (tipo de tratamiento a aplicar) */
export type AccionTratamiento = EstadoDental;

export const ACCIONES_TOOLBAR: { estado: AccionTratamiento; label: string; icon?: string }[] = [
  { estado: 'CARIES', label: 'Caries', icon: '🔴' },
  { estado: 'OBTURACION', label: 'Restauración', icon: '🔵' },
  { estado: 'CORONA', label: 'Corona', icon: '🟡' },
  { estado: 'EXTRACCION_INDICADA', label: 'Extracción', icon: '❌' },
  { estado: 'IMPLANTE', label: 'Implante', icon: '⚙' },
  { estado: 'ENDODONCIA', label: 'Endodoncia', icon: '🟢' },
  { estado: 'SELLANTE', label: 'Sellante', icon: '🟩' },
  { estado: 'PROTESIS', label: 'Prótesis', icon: '🟣' },
];

@Injectable({ providedIn: 'root' })
export class OdontogramaService {
  private readonly _vista = signal<TipoVista>('oclusal');
  private readonly _tratamientoSeleccionado = signal<EstadoDental | null>(null);
  private readonly _zoomOclusal = signal(1);
  private readonly _modo = signal<'adulto' | 'pediatrico'>('adulto');
  /** Historial por clave "fdi-superficie" (ej. "16-MESIAL") */
  private readonly _historial = signal<Map<string, Tratamiento[]>>(new Map());

  readonly vista = this._vista.asReadonly();
  readonly tratamientoSeleccionado = this._tratamientoSeleccionado.asReadonly();
  readonly zoomOclusal = this._zoomOclusal.asReadonly();
  readonly modo = this._modo.asReadonly();
  readonly historial = this._historial.asReadonly();

  readonly tieneTratamientoSeleccionado = computed(
    () => this._tratamientoSeleccionado() !== null
  );

  setVista(v: TipoVista): void {
    this._vista.set(v);
  }

  setTratamientoSeleccionado(estado: EstadoDental | null): void {
    this._tratamientoSeleccionado.set(estado);
  }

  setZoomOclusal(zoom: number): void {
    this._zoomOclusal.set(Math.max(0.5, Math.min(2.5, zoom)));
  }

  setModo(modo: 'adulto' | 'pediatrico'): void {
    this._modo.set(modo);
  }

  agregarAlHistorial(fdi: number, superficie: string, tratamiento: Tratamiento): void {
    const key = `${fdi}-${superficie}`;
    const map = new Map(this._historial());
    const list = map.get(key) ?? [];
    list.push(tratamiento);
    map.set(key, list);
    this._historial.set(map);
  }

  getHistorial(fdi: number, superficie: string): Tratamiento[] {
    return this._historial().get(`${fdi}-${superficie}`) ?? [];
  }

  limpiarHistorial(): void {
    this._historial.set(new Map());
  }
}
