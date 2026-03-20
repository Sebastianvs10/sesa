/**
 * Modelos de datos para el módulo de Agenda / Turnos — IPS Nivel II Colombia.
 * Autor: Ing. J Sebastian Vargas S
 */

/* ── Tipos base ──────────────────────────────────────────────────── */

export type TipoTurno =
  | 'URG_DIA'        // Urgencias Día  7am–7pm  (12 h)
  | 'URG_NOCHE'      // Urgencias Noche 7pm–7am (12 h)
  | 'TURNO_6H'       // Turno de  6 horas
  | 'TURNO_8H'       // Turno de  8 horas
  | 'TURNO_12H'      // Turno de 12 horas (diurno genérico)
  | 'TURNO_24H'      // Turno de 24 horas (fin de semana)
  | 'DISPONIBILIDAD';// On-call / guardia pasiva

export type ServicioClinico =
  | 'URGENCIAS'
  | 'HOSPITALIZACION'
  | 'OBSERVACION'
  | 'UCI'
  | 'CONSULTA_EXTERNA';

export type EstadoTurno         = 'BORRADOR' | 'APROBADO' | 'CERRADO';
export type EstadoProgramacion  = 'BORRADOR' | 'EN_REVISION' | 'APROBADO' | 'CERRADO';
export type TipoPersonal        =
  | 'MEDICO'
  | 'ENFERMERO'
  | 'AUXILIAR_ENFERMERIA'
  | 'ODONTOLOGO'
  | 'RECEPCIONISTA'
  | 'OTRO';  // BACTERIOLOGO, PSICOLOGO, REGENTE_FARMACIA, USER, ADMIN, etc.
export type AlertaTipo          = 'CONFLICTO' | 'ADVERTENCIA' | 'OK';

/* ── Entidades principales ───────────────────────────────────────── */

export interface Profesional {
  id:           number;
  nombre:       string;
  apellido:     string;
  tipo:         TipoPersonal;
  especialidad?: string;
  registro:     string;   // RM (médico) o RE (enfermero)
  color:        string;   // color de identificación en grilla
  activo:       boolean;
}

export interface Turno {
  id:             string;
  profesionalId:  number;
  servicio:       ServicioClinico;
  tipo:           TipoTurno;
  fechaInicio:    Date;
  fechaFin:       Date;
  duracionHoras:  number;
  estado:         EstadoTurno;
  notas?:         string;
  esFestivo:      boolean;
  alerta:         AlertaTipo;
  alertaMensaje:  string;
}

export interface DiaCalendario {
  fecha:         Date;
  esHoy:         boolean;
  esMesActual:   boolean;
  esFestivo:     boolean;
  festivoNombre?: string;
  turnos:        Turno[];
}

export interface ResumenProfesional {
  profesional:    Profesional;
  horasTotales:   number;
  horasNocturnas: number;
  horasFestivos:  number;
  turnosCount:    number;
  alerta:         AlertaTipo;
}

export interface HistorialCambio {
  id:         string;
  turnoId:    string;
  accion:     'CREADO' | 'MODIFICADO' | 'ELIMINADO' | 'APROBADO';
  usuario:    string;
  fecha:      Date;
  detalle:    string;
}

/* ── Catálogos de configuración ──────────────────────────────────── */

export const TURNO_CONFIG: Record<TipoTurno, {
  label: string; horas: number; horaInicio: number; horaFin: number;
  color: string; colorBg: string; esNocturno: boolean;
}> = {
  URG_DIA:       { label: 'Urgencias Día',   horas: 12, horaInicio: 7,  horaFin: 19, color: '#f97316', colorBg: '#fff7ed', esNocturno: false },
  URG_NOCHE:     { label: 'Urgencias Noche', horas: 12, horaInicio: 19, horaFin: 7,  color: '#6366f1', colorBg: '#eef2ff', esNocturno: true  },
  TURNO_6H:      { label: 'Turno 6 h',       horas: 6,  horaInicio: 6,  horaFin: 12, color: '#10b981', colorBg: '#ecfdf5', esNocturno: false },
  TURNO_8H:      { label: 'Turno 8 h',       horas: 8,  horaInicio: 7,  horaFin: 15, color: '#3b82f6', colorBg: '#eff6ff', esNocturno: false },
  TURNO_12H:     { label: 'Turno 12 h',      horas: 12, horaInicio: 7,  horaFin: 19, color: '#8b5cf6', colorBg: '#f5f3ff', esNocturno: false },
  TURNO_24H:     { label: 'Turno 24 h',      horas: 24, horaInicio: 7,  horaFin: 7,  color: '#f43f5e', colorBg: '#fff1f2', esNocturno: true  },
  DISPONIBILIDAD:{ label: 'Disponibilidad',  horas: 8,  horaInicio: 0,  horaFin: 8,  color: '#64748b', colorBg: '#f8fafc', esNocturno: false },
};

export const SERVICIO_CONFIG: Record<ServicioClinico, { label: string; color: string; icon: string }> = {
  URGENCIAS:       { label: 'Urgencias',        color: '#ef4444', icon: '🚑' },
  HOSPITALIZACION: { label: 'Hospitalización',  color: '#3b82f6', icon: '🛏️' },
  OBSERVACION:     { label: 'Observación',      color: '#8b5cf6', icon: '👁️' },
  UCI:             { label: 'UCI',              color: '#f59e0b', icon: '⚡' },
  CONSULTA_EXTERNA:{ label: 'Consulta Externa', color: '#22c55e', icon: '🩺' },
};

/* ── Festivos Colombia 2025 (Ley Emiliani) ───────────────────────── */

export const FESTIVOS_CO: { fecha: string; nombre: string }[] = [
  // 2025
  { fecha: '2025-01-01', nombre: 'Año Nuevo' },
  { fecha: '2025-01-06', nombre: 'Reyes Magos' },
  { fecha: '2025-03-24', nombre: 'San José' },
  { fecha: '2025-04-13', nombre: 'Jueves Santo' },
  { fecha: '2025-04-14', nombre: 'Viernes Santo' },
  { fecha: '2025-05-01', nombre: 'Día del Trabajo' },
  { fecha: '2025-06-02', nombre: 'Ascensión del Señor' },
  { fecha: '2025-06-23', nombre: 'Corpus Christi' },
  { fecha: '2025-06-30', nombre: 'Sagrado Corazón' },
  { fecha: '2025-07-07', nombre: 'San Pedro y San Pablo' },
  { fecha: '2025-07-20', nombre: 'Independencia de Colombia' },
  { fecha: '2025-08-07', nombre: 'Batalla de Boyacá' },
  { fecha: '2025-08-18', nombre: 'Asunción de la Virgen' },
  { fecha: '2025-10-13', nombre: 'Día de la Raza' },
  { fecha: '2025-11-03', nombre: 'Todos los Santos' },
  { fecha: '2025-11-17', nombre: 'Independencia de Cartagena' },
  { fecha: '2025-12-08', nombre: 'Inmaculada Concepción' },
  { fecha: '2025-12-25', nombre: 'Navidad' },
  // 2026
  { fecha: '2026-01-01', nombre: 'Año Nuevo' },
  { fecha: '2026-01-12', nombre: 'Reyes Magos' },
  { fecha: '2026-03-23', nombre: 'San José' },
  { fecha: '2026-04-02', nombre: 'Jueves Santo' },
  { fecha: '2026-04-03', nombre: 'Viernes Santo' },
  { fecha: '2026-05-01', nombre: 'Día del Trabajo' },
  { fecha: '2026-07-20', nombre: 'Independencia de Colombia' },
  { fecha: '2026-08-07', nombre: 'Batalla de Boyacá' },
  { fecha: '2026-12-08', nombre: 'Inmaculada Concepción' },
  { fecha: '2026-12-25', nombre: 'Navidad' },
];

/* ── Constantes de límites laborales (Código Sustantivo del Trabajo) */

export const LIMITES_LABORALES = {
  MAX_HORAS_SEMANALES:           48,
  MAX_HORAS_MENSUALES:          192,
  DESCANSO_MIN_NORMAL_H:          8,
  DESCANSO_MIN_POST_NOCTURNO_H:  12,
  DESCANSO_MIN_POST_24H_H:       24,
  ALERTA_HORAS_SEMANALES:        40, // advertencia antes del límite
} as const;

/* ── Días de la semana en español ────────────────────────────────── */

export const DIAS_SEMANA = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];

export const MESES_ES = [
  'Enero','Febrero','Marzo','Abril','Mayo','Junio',
  'Julio','Agosto','Septiembre','Octubre','Noviembre','Diciembre',
];
