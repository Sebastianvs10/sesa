/**
 * Dashboard gerencial EBS — Supervisor APS.
 * Indicadores: cobertura, población visitada, visitas en período, hogares alto riesgo.
 * Autor: Ing. J Sebastian Vargas S
 */

import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { EbsService, EbsDashboardData } from '../../core/services/ebs.service';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { SesaSkeletonComponent } from '../../shared/components/sesa-skeleton/sesa-skeleton.component';

@Component({
  standalone: true,
  selector: 'sesa-ebs-dashboard-supervisor',
  imports: [CommonModule, RouterLink, SesaCardComponent, SesaSkeletonComponent],
  templateUrl: './ebs-dashboard-supervisor.page.html',
  styleUrl: './ebs-dashboard-supervisor.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EbsDashboardSupervisorPageComponent {
  private readonly ebs = inject(EbsService);

  readonly dashboard = signal<EbsDashboardData | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  diasPeriodo = 30;

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.ebs.getDashboard(this.diasPeriodo).subscribe({
      next: (d) => {
        this.dashboard.set(d);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el dashboard');
        this.loading.set(false);
      },
    });
  }

  changePeriodo(dias: number): void {
    this.diasPeriodo = dias;
    this.load();
  }
}
