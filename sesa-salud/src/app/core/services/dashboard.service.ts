/**
 * Servicio de dashboard: carga datos en tiempo real según permisos del rol.
 * Autor: Ing. J Sebastian Vargas S
 */

import { Injectable, inject } from '@angular/core';
import { forkJoin, map, Observable, of, catchError } from 'rxjs';
import { AuthService } from './auth.service';
import { CitaService, CitaDto } from './cita.service';
import { PacienteService } from './paciente.service';
import { ReporteService, ReporteResumenDto } from './reporte.service';
import { PermissionsService } from './permissions.service';

export interface DashboardResumen {
  totalPacientes?: number;
  totalCitas?: number;
  totalConsultas?: number;
  totalOrdenes?: number;
  totalFacturas?: number;
  totalFacturado?: number;
}

export interface DashboardData {
  role: string;
  resumen: DashboardResumen;
  citasHoy: CitaDto[];
  fechaHoy: string;
  loading: boolean;
  error: string | null;
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly auth = inject(AuthService);
  private readonly reporteService = inject(ReporteService);
  private readonly citaService = inject(CitaService);
  private readonly pacienteService = inject(PacienteService);
  private readonly permissions = inject(PermissionsService);

  /** Carga todos los datos del dashboard según el rol del usuario */
  load(role: string): Observable<DashboardData> {
    const canReportes = this.canLoadResumen(role);
    const canCitas = this.permissions.canAccess('CITAS');

    const hoy = this.getFechaHoy();

    const obs: Record<string, Observable<unknown>> = {};
    if (canReportes) {
      obs['resumen'] = this.reporteService.resumen().pipe(catchError(() => of(null)));
    } else {
      obs['resumen'] = of(null);
    }
    if (canCitas) {
      obs['citasHoy'] = this.citaService.list(hoy).pipe(catchError(() => of([])));
    } else {
      obs['citasHoy'] = of([]);
    }

    return forkJoin(obs).pipe(
      map((r) => {
        const resumen = r['resumen'] as ReporteResumenDto | null;
        const citasHoy = (r['citasHoy'] ?? []) as CitaDto[];
        return {
          role,
          resumen: resumen
            ? {
                totalPacientes: resumen.totalPacientes,
                totalCitas: resumen.totalCitas,
                totalConsultas: resumen.totalConsultas,
                totalOrdenes: resumen.totalOrdenes,
                totalFacturas: resumen.totalFacturas,
                totalFacturado: resumen.totalFacturado,
              }
            : {},
          citasHoy,
          fechaHoy: hoy,
          loading: false,
          error: null,
        } as DashboardData;
      }),
      catchError((err) =>
        of({
          role,
          resumen: {},
          citasHoy: [],
          fechaHoy: hoy,
          loading: false,
          error: err?.error?.error || 'Error al cargar el dashboard',
        } as DashboardData)
      )
    );
  }

  private canLoadResumen(role: string): boolean {
    const r = (role || '').toUpperCase();
    return ['ADMIN', 'USER', 'MEDICO', 'ODONTOLOGO', 'BACTERIOLOGO', 'ENFERMERO', 'JEFE_ENFERMERIA', 'AUXILIAR_ENFERMERIA', 'PSICOLOGO', 'REGENTE_FARMACIA', 'RECEPCIONISTA', 'SUPERADMINISTRADOR'].includes(r);
  }

  private getFechaHoy(): string {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }

  /** Retorna fechas con citas para el calendario (YYYY-MM-DD) */
  getCitasDatesForMonth(citas: CitaDto[]): Set<string> {
    const dates = new Set<string>();
    for (const c of citas) {
      if (c.fechaHora) {
        const d = new Date(c.fechaHora);
        dates.add(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`);
      }
    }
    return dates;
  }
}
