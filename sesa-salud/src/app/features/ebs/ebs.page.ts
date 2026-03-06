/**
 * Módulo Equipos Básicos de Salud (EBS) — Front móvil.
 * Gestión territorial básica + hogares y estado de visitas.
 * Autor: Ing. J Sebastian Vargas S
 */

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { SesaSkeletonComponent } from '../../shared/components/sesa-skeleton/sesa-skeleton.component';
import { SesaEmptyStateComponent } from '../../shared/components/sesa-empty-state/sesa-empty-state.component';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';
import {
  EbsHouseholdSummary,
  EbsRiskLevel,
  EbsService,
  EbsTerritorySummary,
  EbsVisitState,
} from '../../core/services/ebs.service';

@Component({
  standalone: true,
  selector: 'sesa-ebs-page',
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    SesaCardComponent,
    SesaSkeletonComponent,
    SesaEmptyStateComponent,
  ],
  templateUrl: './ebs.page.html',
  styleUrl: './ebs.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EbsPageComponent implements OnInit {
  private readonly ebs = inject(EbsService);
  private readonly toast = inject(SesaToastService);

  readonly territories = signal<EbsTerritorySummary[]>([]);
  readonly households = signal<EbsHouseholdSummary[]>([]);

  readonly loadingTerritories = signal(false);
  readonly loadingHouseholds = signal(false);

  readonly selectedTerritoryId = signal<number | null>(null);

  riskFilter: EbsRiskLevel | 'TODOS' = 'TODOS';
  visitStatusFilter: EbsVisitState | 'TODOS' = 'TODOS';

  readonly error = signal<string | null>(null);

  readonly selectedTerritory = computed<EbsTerritorySummary | null>(() => {
    const id = this.selectedTerritoryId();
    if (!id) return null;
    return this.territories().find((t) => t.id === id) ?? null;
  });

  ngOnInit(): void {
    this.loadTerritories();
  }

  loadTerritories(): void {
    this.loadingTerritories.set(true);
    this.error.set(null);
    this.ebs
      .listTerritories({ riskLevel: this.riskFilter })
      .subscribe({
        next: (list) => {
          const items = list ?? [];
          this.territories.set(items);
          if (!this.selectedTerritoryId() && items.length > 0) {
            this.selectTerritory(items[0].id);
          }
          this.loadingTerritories.set(false);
        },
        error: (err) => {
          this.loadingTerritories.set(false);
          const msg =
            err?.error?.message ??
            'No se pudieron cargar los microterritorios EBS. Verifica tu conexión o permisos.';
          this.error.set(msg);
          this.toast.error(msg, 'Equipos Básicos de Salud');
        },
      });
  }

  selectTerritory(id: number): void {
    if (this.selectedTerritoryId() === id) return;
    this.selectedTerritoryId.set(id);
    this.loadHouseholds();
  }

  reloadHouseholds(): void {
    if (this.selectedTerritoryId()) {
      this.loadHouseholds();
    }
  }

  private loadHouseholds(): void {
    const territoryId = this.selectedTerritoryId();
    if (!territoryId) return;

    this.loadingHouseholds.set(true);
    this.ebs
      .listHouseholds(territoryId, {
        riskLevel: this.riskFilter,
        visitStatus: this.visitStatusFilter,
      })
      .subscribe({
        next: (rows) => {
          this.households.set(rows ?? []);
          this.loadingHouseholds.set(false);
        },
        error: (err) => {
          this.loadingHouseholds.set(false);
          const msg =
            err?.error?.message ??
            'No se pudieron cargar los hogares del microterritorio. Intenta de nuevo más tarde.';
          this.toast.error(msg, 'EBS — Hogares');
        },
      });
  }

  nuevaVisita(household: EbsHouseholdSummary): void {
    const now = new Date();
    const payload = {
      householdId: household.id,
      visitDate: now.toISOString(),
      visitType: 'DOMICILIARIA_APS',
    };

    this.ebs.createHomeVisit(payload).subscribe({
      next: (res: unknown) => {
        const body: any = res as any;
        if (body && body.offline) {
          this.toast.info(
            'Visita domiciliaria registrada en el dispositivo. Se sincronizará cuando haya conexión.',
            'EBS — Visita encolada',
          );
        } else {
          this.toast.success('Visita domiciliaria creada correctamente.', 'EBS — Nueva visita');
          this.reloadHouseholds();
        }
      },
      error: (err) => {
        const msg =
          err?.error?.message ??
          'No se pudo crear la visita domiciliaria. Verifica la conexión o intenta más tarde.';
        this.toast.error(msg, 'EBS — Nueva visita');
      },
    });
  }

  labelRisk(level?: EbsRiskLevel): string {
    switch (level) {
      case 'ALTO':
        return 'Riesgo alto';
      case 'MUY_ALTO':
        return 'Riesgo muy alto';
      case 'MEDIO':
        return 'Riesgo medio';
      case 'BAJO':
        return 'Riesgo bajo';
      default:
        return 'Sin estratificar';
    }
  }

  visitStateLabel(state: EbsVisitState): string {
    if (state === 'PENDIENTE_VISITA') return 'Pendiente de visita';
    if (state === 'EN_SEGUIMIENTO') return 'En seguimiento';
    return 'Cerrado';
  }

  badgeClassForRisk(level?: EbsRiskLevel): string {
    switch (level) {
      case 'ALTO':
      case 'MUY_ALTO':
        return 'ebs-chip ebs-chip-danger';
      case 'MEDIO':
        return 'ebs-chip ebs-chip-warning';
      case 'BAJO':
        return 'ebs-chip ebs-chip-success';
      default:
        return 'ebs-chip ebs-chip-muted';
    }
  }
}

