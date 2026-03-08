/**
 * Página de Reportes con gráficas de indicadores clínicos.
 * Acceso restringido: solo ADMIN de la empresa y SUPERADMINISTRADOR.
 * Autor: Ing. J Sebastian Vargas S
 */

import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faChartBar, faArrowLeft, faFileCsv } from '@fortawesome/free-solid-svg-icons';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { DashboardChartBarComponent } from '../../shared/components/dashboard-chart-bar/dashboard-chart-bar.component';
import { DashboardChartDoughnutComponent } from '../../shared/components/dashboard-chart-doughnut/dashboard-chart-doughnut.component';
import { ReporteService, DashboardStatsDto, IndicadorCalidadDto } from '../../core/services/reporte.service';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';

const MESES_LABELS: Record<number, string> = {
  1: 'Ene', 2: 'Feb', 3: 'Mar', 4: 'Abr', 5: 'May', 6: 'Jun',
  7: 'Jul', 8: 'Ago', 9: 'Sep', 10: 'Oct', 11: 'Nov', 12: 'Dic',
};
const ESTADO_LABELS: Record<string, string> = {
  AGENDADA: 'Agendada', PENDIENTE: 'Pendiente', CONFIRMADA: 'Confirmada',
  ATENDIDO: 'Atendido', CANCELADA: 'Cancelada', NO_ASISTIO: 'No asistió',
  SIN_ESTADO: 'Sin estado',
};

@Component({
  standalone: true,
  selector: 'sesa-reportes-page',
  imports: [
    CommonModule,
    RouterLink,
    FontAwesomeModule,
    SesaCardComponent,
    DashboardChartBarComponent,
    DashboardChartDoughnutComponent,
  ],
  templateUrl: './reportes.page.html',
  styleUrl: './reportes.page.scss',
})
export class ReportesPageComponent implements OnInit {
  private readonly reporteService = inject(ReporteService);
  private readonly toast = inject(SesaToastService);

  loading = true;
  error = false;
  loadingCalidad = true;
  indicadoresCalidad: IndicadorCalidadDto[] = [];

  chartCitasPorDia: { labels: string[]; values: number[] } = { labels: [], values: [] };
  chartConsultasPorMes: { labels: string[]; values: number[] } = { labels: [], values: [] };
  chartFacturacionPorMes: { labels: string[]; values: number[] } = { labels: [], values: [] };
  chartCitasPorEstado: { labels: string[]; values: number[] } = { labels: [], values: [] };

  faChartBar = faChartBar;
  faArrowLeft = faArrowLeft;
  faFileCsv = faFileCsv;

  /** Exporta reportes a CSV (Res. 5521/2013 — auditoría y gestión). */
  exportarCsv(): void {
    const rows: string[] = [];
    const esc = (v: string | number): string => {
      const s = String(v ?? '');
      return s.includes(',') || s.includes('"') || s.includes('\n') ? `"${s.replace(/"/g, '""')}"` : s;
    };
    rows.push('Reportes SESA - Exportación');
    rows.push(`Fecha exportación,${new Date().toISOString()}`);
    rows.push('');

    if (this.chartCitasPorDia.labels.length) {
      rows.push('Citas por día');
      rows.push('Fecha,Total');
      this.chartCitasPorDia.labels.forEach((l, i) =>
        rows.push(`${esc(l)},${this.chartCitasPorDia.values[i] ?? 0}`)
      );
      rows.push('');
    }
    if (this.chartConsultasPorMes.labels.length) {
      rows.push('Consultas por mes');
      rows.push('Mes,Total');
      this.chartConsultasPorMes.labels.forEach((l, i) =>
        rows.push(`${esc(l)},${this.chartConsultasPorMes.values[i] ?? 0}`)
      );
      rows.push('');
    }
    if (this.chartFacturacionPorMes.labels.length) {
      rows.push('Facturación por mes (COP)');
      rows.push('Mes,Valor total');
      this.chartFacturacionPorMes.labels.forEach((l, i) =>
        rows.push(`${esc(l)},${this.chartFacturacionPorMes.values[i] ?? 0}`)
      );
      rows.push('');
    }
    if (this.chartCitasPorEstado.labels.length) {
      rows.push('Citas por estado');
      rows.push('Estado,Total');
      this.chartCitasPorEstado.labels.forEach((l, i) =>
        rows.push(`${esc(l)},${this.chartCitasPorEstado.values[i] ?? 0}`)
      );
      rows.push('');
    }
    if (this.indicadoresCalidad.length) {
      rows.push('Indicadores de calidad (Res. 0256/2016)');
      rows.push('Código,Nombre,Categoría,Valor,Meta,Unidad,Interpretación');
      this.indicadoresCalidad.forEach((ind) => {
        rows.push(
          [ind.codigo, ind.nombre, ind.categoria, ind.valor, ind.meta, ind.unidad, ind.interpretacion]
            .map(esc)
            .join(',')
        );
      });
    }
    const csv = '\uFEFF' + rows.join('\r\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `reportes-sesa-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(a.href);
    this.toast.success('Exportación descargada.', 'CSV');
  }

  ngOnInit(): void {
    this.loadStats();
    this.loadCalidad();
  }

  loadCalidad(): void {
    this.reporteService.indicadoresCalidad()
      .pipe(catchError(() => of<IndicadorCalidadDto[]>([])))
      .subscribe((list) => {
        this.indicadoresCalidad = list;
        this.loadingCalidad = false;
      });
  }

  loadStats(): void {
    this.loading = true;
    this.error = false;
    this.reporteService
      .dashboardStats()
      .pipe(catchError(() => {
        this.error = true;
        this.toast.error('No se pudieron cargar los reportes. Intenta de nuevo.', 'Error de reportes');
        return of<DashboardStatsDto>({});
      }))
      .subscribe((r) => {
        const citasPorDia = r.citasPorDia ?? [];
        const consultasPorMes = r.consultasPorMes ?? [];
        const facturacionPorMes = r.facturacionPorMes ?? [];
        const citasPorEstado = r.citasPorEstado ?? [];

        this.chartCitasPorDia = {
          labels: citasPorDia.map((x) => this.formatShortDate(x.fecha)),
          values: citasPorDia.map((x) => x.total),
        };
        this.chartConsultasPorMes = {
          labels: consultasPorMes.map((x) => MESES_LABELS[x.mes] ?? `${x.mes}`),
          values: consultasPorMes.map((x) => x.total),
        };
        this.chartFacturacionPorMes = {
          labels: facturacionPorMes.map((x) => MESES_LABELS[x.mes] ?? `${x.mes}`),
          values: facturacionPorMes.map((x) => Number(x.valorTotal)),
        };
        this.chartCitasPorEstado = {
          labels: citasPorEstado.map((x) => ESTADO_LABELS[x.estado] ?? x.estado),
          values: citasPorEstado.map((x) => x.total),
        };
        this.loading = false;
      });
  }

  get hasAnyData(): boolean {
    return (
      this.chartCitasPorDia.labels.length > 0 ||
      this.chartConsultasPorMes.labels.length > 0 ||
      this.chartFacturacionPorMes.labels.length > 0 ||
      this.chartCitasPorEstado.labels.length > 0
    );
  }

  private formatShortDate(fecha: string): string {
    const d = new Date(fecha + 'T12:00:00');
    return d.toLocaleDateString('es-CO', { day: 'numeric', month: 'short' });
  }
}
