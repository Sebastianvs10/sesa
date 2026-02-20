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
import { faChartBar, faArrowLeft } from '@fortawesome/free-solid-svg-icons';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { DashboardChartBarComponent } from '../../shared/components/dashboard-chart-bar/dashboard-chart-bar.component';
import { DashboardChartDoughnutComponent } from '../../shared/components/dashboard-chart-doughnut/dashboard-chart-doughnut.component';
import { ReporteService, DashboardStatsDto } from '../../core/services/reporte.service';
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

  chartCitasPorDia: { labels: string[]; values: number[] } = { labels: [], values: [] };
  chartConsultasPorMes: { labels: string[]; values: number[] } = { labels: [], values: [] };
  chartFacturacionPorMes: { labels: string[]; values: number[] } = { labels: [], values: [] };
  chartCitasPorEstado: { labels: string[]; values: number[] } = { labels: [], values: [] };

  faChartBar = faChartBar;
  faArrowLeft = faArrowLeft;

  ngOnInit(): void {
    this.loadStats();
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
