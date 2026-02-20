/**
 * Servicio de validación de turnos — reglas laborales colombianas IPS Nivel II.
 * Código Sustantivo del Trabajo + Resolución 2003/2014 MSPS.
 * Autor: Ing. J Sebastian Vargas S
 */
import { Injectable } from '@angular/core';
import {
  Turno, AlertaTipo, LIMITES_LABORALES, TURNO_CONFIG,
} from './agenda.models';

export interface ValidacionResult {
  alerta:  AlertaTipo;
  mensaje: string;
}

export interface ResumenHoras {
  horasSemanales: number;
  horasMensuales: number;
  horasNocturnas: number;
  horasFestivos:  number;
}

@Injectable({ providedIn: 'root' })
export class AgendaValidationService {

  /**
   * Valida un turno respecto a todos los demás turnos del mismo profesional.
   * Devuelve la alerta más crítica detectada.
   */
  validar(turno: Turno, todosTurnos: Turno[]): ValidacionResult {
    const delMismo = todosTurnos.filter(
      (t) => t.profesionalId === turno.profesionalId && t.id !== turno.id,
    );

    // 1 — Solapamiento directo (conflicto bloqueante)
    for (const t of delMismo) {
      if (this.seSolapan(turno, t)) {
        return {
          alerta:  'CONFLICTO',
          mensaje: `Cruce con "${TURNO_CONFIG[t.tipo].label}" el ${this.fmtFecha(t.fechaInicio)}`,
        };
      }
    }

    // 2 — Descanso insuficiente tras turno nocturno o de 24 h
    for (const t of delMismo) {
      const cfg = TURNO_CONFIG[t.tipo];
      if (!cfg.esNocturno) continue;

      const minDescanso =
        t.tipo === 'TURNO_24H'
          ? LIMITES_LABORALES.DESCANSO_MIN_POST_24H_H
          : LIMITES_LABORALES.DESCANSO_MIN_POST_NOCTURNO_H;

      const gapHoras = this.gapHoras(t.fechaFin, turno.fechaInicio);
      if (gapHoras >= 0 && gapHoras < minDescanso) {
        return {
          alerta:  'CONFLICTO',
          mensaje: `Descanso insuficiente tras turno nocturno (mín. ${minDescanso} h, actual: ${gapHoras.toFixed(0)} h)`,
        };
      }

      // Verificar si el nuevo turno cae antes del anterior
      const gapInverso = this.gapHoras(turno.fechaFin, t.fechaInicio);
      if (gapInverso >= 0 && gapInverso < minDescanso) {
        return {
          alerta:  'CONFLICTO',
          mensaje: `El turno siguiente no respeta el descanso mínimo post-nocturno (${minDescanso} h)`,
        };
      }
    }

    // 3 — Horas en la semana del turno
    const horasSemana = this.horasEnSemana(turno.fechaInicio, [turno, ...delMismo]);
    if (horasSemana > LIMITES_LABORALES.MAX_HORAS_SEMANALES) {
      return {
        alerta:  'CONFLICTO',
        mensaje: `Excede las 48 h semanales máximas (${horasSemana.toFixed(0)} h acumuladas)`,
      };
    }
    if (horasSemana > LIMITES_LABORALES.ALERTA_HORAS_SEMANALES) {
      return {
        alerta:  'ADVERTENCIA',
        mensaje: `Cerca del límite semanal: ${horasSemana.toFixed(0)} / 48 h`,
      };
    }

    // 4 — Horas mensuales totales
    const horasMes = [turno, ...delMismo].reduce((s, t) => s + t.duracionHoras, 0);
    if (horasMes > LIMITES_LABORALES.MAX_HORAS_MENSUALES) {
      return {
        alerta:  'ADVERTENCIA',
        mensaje: `Supera 192 h mensuales (${horasMes} h). Verificar con Talento Humano`,
      };
    }

    return { alerta: 'OK', mensaje: '' };
  }

  /** Recalcula las alertas de todos los turnos de un profesional. */
  revalidarProfesional(profesionalId: number, todos: Turno[]): Turno[] {
    const delMismo = todos.filter((t) => t.profesionalId === profesionalId);
    return todos.map((t) => {
      if (t.profesionalId !== profesionalId) return t;
      const r = this.validar(t, delMismo.filter((x) => x.id !== t.id).concat(todos.filter((x) => x.profesionalId !== profesionalId)));
      return { ...t, alerta: r.alerta, alertaMensaje: r.mensaje };
    });
  }

  /** Calcula el resumen de horas de un profesional en una lista de turnos. */
  resumenHoras(profesionalId: number, turnos: Turno[]): ResumenHoras {
    const mine = turnos.filter((t) => t.profesionalId === profesionalId);
    return {
      horasMensuales: mine.reduce((s, t) => s + t.duracionHoras, 0),
      horasSemanales: this.horasEnSemana(new Date(), mine),
      horasNocturnas: mine
        .filter((t) => TURNO_CONFIG[t.tipo].esNocturno)
        .reduce((s, t) => s + t.duracionHoras, 0),
      horasFestivos: mine
        .filter((t) => t.esFestivo)
        .reduce((s, t) => s + t.duracionHoras, 0),
    };
  }

  /** Alerta global del profesional en el mes: la peor de todas sus validaciones. */
  alertaGlobal(profesionalId: number, todos: Turno[]): AlertaTipo {
    const mine = todos.filter((t) => t.profesionalId === profesionalId);
    if (mine.some((t) => t.alerta === 'CONFLICTO'))   return 'CONFLICTO';
    if (mine.some((t) => t.alerta === 'ADVERTENCIA')) return 'ADVERTENCIA';
    return 'OK';
  }

  /* ── helpers privados ──────────────────────────────────────────── */

  private seSolapan(a: Turno, b: Turno): boolean {
    return a.fechaInicio < b.fechaFin && a.fechaFin > b.fechaInicio;
  }

  private gapHoras(fin: Date, inicio: Date): number {
    return (inicio.getTime() - fin.getTime()) / 3_600_000;
  }

  private horasEnSemana(ref: Date, turnos: Turno[]): number {
    const lunes  = this.inicioSemana(ref);
    const sig    = new Date(lunes);
    sig.setDate(lunes.getDate() + 7);
    return turnos
      .filter((t) => t.fechaInicio >= lunes && t.fechaInicio < sig)
      .reduce((s, t) => s + t.duracionHoras, 0);
  }

  private inicioSemana(d: Date): Date {
    const c = new Date(d);
    c.setHours(0, 0, 0, 0);
    const day = c.getDay(); // 0=dom
    c.setDate(c.getDate() - (day === 0 ? 6 : day - 1));
    return c;
  }

  private fmtFecha(d: Date): string {
    return d.toLocaleDateString('es-CO', { day: '2-digit', month: 'short' });
  }
}
