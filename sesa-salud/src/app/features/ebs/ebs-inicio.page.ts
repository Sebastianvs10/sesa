/**
 * Página Inicio EBS — resumen por rol (Profesional EBS, Coordinador Territorial, Supervisor APS).
 * Autor: Ing. J Sebastian Vargas S
 */

import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';
import { EbsService, EbsTerritorySummary, EbsDashboardData } from '../../core/services/ebs.service';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { SesaSkeletonComponent } from '../../shared/components/sesa-skeleton/sesa-skeleton.component';

@Component({
  standalone: true,
  selector: 'sesa-ebs-inicio',
  imports: [CommonModule, RouterLink, SesaCardComponent, SesaSkeletonComponent],
  templateUrl: './ebs-inicio.page.html',
  styleUrl: './ebs-inicio.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EbsInicioPageComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly ebs = inject(EbsService);

  readonly territories = signal<EbsTerritorySummary[]>([]);
  readonly dashboard = signal<EbsDashboardData | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly currentRoles = computed(() => this.auth.currentRoles());
  readonly isCoordinador = computed(() => this.currentRoles().includes('COORDINADOR_TERRITORIAL'));
  readonly isSupervisor = computed(() => this.currentRoles().includes('SUPERVISOR_APS'));
  readonly isProfesionalEbs = computed(() =>
    this.currentRoles().includes('EBS') && !this.isCoordinador() && !this.isSupervisor()
  );

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    const isSup = this.auth.currentRoles().includes('SUPERVISOR_APS');
    if (isSup) {
      this.ebs.getDashboard(30).subscribe({
        next: (d) => {
          this.dashboard.set(d);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('No se pudo cargar el dashboard');
          this.loading.set(false);
        },
      });
    } else {
      this.ebs.listTerritories({}).subscribe({
        next: (list) => {
          this.territories.set(list ?? []);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('No se pudieron cargar los territorios');
          this.loading.set(false);
        },
      });
    }
  }
}
