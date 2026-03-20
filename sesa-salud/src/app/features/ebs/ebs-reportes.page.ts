/**
 * Reportes EBS — cobertura, datos para exportar PDF/Excel.
 * Autor: Ing. J Sebastian Vargas S
 */

import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EbsService, EbsReportDataDto } from '../../core/services/ebs.service';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { SesaSkeletonComponent } from '../../shared/components/sesa-skeleton/sesa-skeleton.component';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';

@Component({
  standalone: true,
  selector: 'sesa-ebs-reportes',
  imports: [CommonModule, SesaCardComponent, SesaSkeletonComponent],
  templateUrl: './ebs-reportes.page.html',
  styleUrl: './ebs-reportes.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EbsReportesPageComponent {
  private readonly ebs = inject(EbsService);
  private readonly toast = inject(SesaToastService);

  readonly report = signal<EbsReportDataDto | null>(null);
  readonly loading = signal(false);
  readonly reportType = signal('COBERTURA');

  load(): void {
    this.loading.set(true);
    this.ebs.getReportData({ reportType: this.reportType() }).subscribe({
      next: (data) => {
        this.report.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.toast.error('Error al cargar reporte');
      },
    });
  }

  constructor() {
    this.load();
  }

  downloadExcel(): void {
    const data = this.report();
    if (!data?.rows?.length) {
      this.toast.error('No hay datos para exportar');
      return;
    }
    const headers = ['Territorio', 'Vereda', 'Hogares', 'Visitados', 'Cobertura %', 'Alto riesgo'];
    const rows = data.rows.map((r) => [
      r.territoryName ?? '',
      r.veredaName ?? '',
      String(r.households ?? 0),
      String(r.visited ?? 0),
      String(r.percent ?? 0),
      String(r.highRisk ?? 0),
    ]);
    const csv = [headers.join(','), ...rows.map((r) => r.join(','))].join('\n');
    const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `EBS_Reporte_${this.reportType()}_${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
    this.toast.success('Archivo descargado');
  }
}
