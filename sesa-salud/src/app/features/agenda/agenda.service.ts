/**
 * Servicio de datos para el módulo Agenda de Turnos.
 * Consume la API REST del backend Spring Boot.
 * Autor: Ing. J Sebastian Vargas S
 */
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Profesional, Turno, EstadoProgramacion,
  TipoTurno, ServicioClinico, TURNO_CONFIG, FESTIVOS_CO,
} from './agenda.models';

/* ── Tipos de respuesta del backend ──────────────────────────────── */

interface TurnoDtoBackend {
  id:                  number;
  personalId:          number;
  personalNombres:     string;
  personalApellidos:   string;
  programacionMesId:   number;
  servicio:            ServicioClinico;
  tipoTurno:           TipoTurno;
  duracionHoras:       number;
  fechaInicio:         string;  // ISO LocalDateTime
  fechaFin:            string;
  estado:              Turno['estado'];
  esFestivo:           boolean;
  notas?:              string;
}

interface ProgramacionMesDtoBackend {
  id:     number;
  anio:   number;
  mes:    number;
  estado: EstadoProgramacion;
}

interface ProfesionalBackend {
  id:        number;
  nombres:   string;
  apellidos: string;
  rol?:      string;
  activo:    boolean;
}

/* ── Colores por índice para distinción visual ───────────────────── */
const COLORES = [
  '#3b82f6','#8b5cf6','#f59e0b','#10b981',
  '#e11d48','#0891b2','#7c3aed','#059669',
  '#dc2626','#d97706','#0284c7','#16a34a',
];

function colorPorIndice(i: number): string {
  return COLORES[i % COLORES.length];
}

function rolATipo(rol: string): Profesional['tipo'] {
  const r = (rol ?? '').toUpperCase();
  if (r === 'ENFERMERO' || r === 'JEFE_ENFERMERIA') return 'ENFERMERO';
  if (r === 'AUXILIAR_ENFERMERIA')                  return 'AUXILIAR_ENFERMERIA';
  return 'MEDICO'; // MEDICO, COORDINADOR_MEDICO, ODONTOLOGO, etc.
}

/* ── Mapeo backend → frontend ────────────────────────────────────── */
function mapTurno(dto: TurnoDtoBackend): Turno {
  return {
    id:            String(dto.id),
    profesionalId: dto.personalId,
    servicio:      dto.servicio,
    tipo:          dto.tipoTurno,
    fechaInicio:   new Date(dto.fechaInicio),
    fechaFin:      new Date(dto.fechaFin),
    duracionHoras: dto.duracionHoras,
    estado:        dto.estado,
    esFestivo:     dto.esFestivo,
    notas:         dto.notas,
    alerta:        'OK',
    alertaMensaje: '',
  };
}

function mapProfesional(dto: ProfesionalBackend, idx: number): Profesional {
  return {
    id:        dto.id,
    nombre:    dto.nombres,
    apellido:  dto.apellidos,
    tipo:      rolATipo(dto.rol ?? ''),
    registro:  dto.rol ?? '',
    color:     colorPorIndice(idx),
    activo:    dto.activo,
  };
}

/* ── Servicio Angular ────────────────────────────────────────────── */
@Injectable({ providedIn: 'root' })
export class AgendaService {
  private readonly http   = inject(HttpClient);
  private readonly base   = `${environment.apiUrl}/agenda`;

  getProfesionales(): Observable<Profesional[]> {
    return this.http.get<ProfesionalBackend[]>(`${environment.apiUrl}/personal?size=200`).pipe(
      map((res: any) => {
        const lista: ProfesionalBackend[] = Array.isArray(res) ? res : (res.content ?? []);
        return lista
          .filter((p) => p.activo)
          .map((p, i) => mapProfesional(p, i));
      }),
    );
  }

  getTurnos(anio: number, mes: number,
            servicio?: ServicioClinico | 'TODOS',
            tipoTurno?: TipoTurno | 'TODOS',
            personalId?: number): Observable<Turno[]> {
    let params = new HttpParams().set('anio', anio).set('mes', mes);
    if (servicio   && servicio   !== 'TODOS') params = params.set('servicio',   servicio);
    if (tipoTurno  && tipoTurno  !== 'TODOS') params = params.set('tipoTurno',  tipoTurno);
    if (personalId && personalId  >  0)       params = params.set('personalId', personalId);

    return this.http.get<TurnoDtoBackend[]>(`${this.base}/turnos`, { params }).pipe(
      map((lista) => lista.map(mapTurno)),
    );
  }

  getEstado(anio: number, mes: number): Observable<EstadoProgramacion> {
    const params = new HttpParams().set('anio', anio).set('mes', mes);
    return this.http.get<ProgramacionMesDtoBackend>(`${this.base}/programacion`, { params }).pipe(
      map((p) => p.estado),
    );
  }

  guardarTurno(req: {
    personalId: number;
    tipoTurno:  TipoTurno;
    servicio:   ServicioClinico;
    fecha:      string;           // yyyy-MM-dd
    estado?:    Turno['estado'];
    notas?:     string;
  }): Observable<Turno> {
    return this.http.post<TurnoDtoBackend>(`${this.base}/turnos`, req).pipe(
      map(mapTurno),
    );
  }

  actualizarTurno(id: string, req: {
    personalId: number;
    tipoTurno:  TipoTurno;
    servicio:   ServicioClinico;
    fecha:      string;
    estado?:    Turno['estado'];
    notas?:     string;
  }): Observable<Turno> {
    return this.http.put<TurnoDtoBackend>(`${this.base}/turnos/${id}`, req).pipe(
      map(mapTurno),
    );
  }

  moverFecha(id: string, fecha: string): Observable<Turno> {
    const params = new HttpParams().set('fecha', fecha);
    return this.http.patch<TurnoDtoBackend>(`${this.base}/turnos/${id}/mover`, null, { params }).pipe(
      map(mapTurno),
    );
  }

  eliminarTurno(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/turnos/${id}`);
  }

  enviarARevision(anio: number, mes: number): Observable<void> {
    const params = new HttpParams().set('anio', anio).set('mes', mes);
    return this.http.post<void>(`${this.base}/programacion/revision`, null, { params });
  }

  aprobarProgramacion(anio: number, mes: number): Observable<void> {
    const params = new HttpParams().set('anio', anio).set('mes', mes);
    return this.http.post<void>(`${this.base}/programacion/aprobar`, null, { params });
  }

  cerrarProgramacion(anio: number, mes: number): Observable<void> {
    const params = new HttpParams().set('anio', anio).set('mes', mes);
    return this.http.post<void>(`${this.base}/programacion/cerrar`, null, { params });
  }
}
