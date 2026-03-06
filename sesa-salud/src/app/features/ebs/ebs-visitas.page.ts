/**
 * Página Historial de visitas domiciliarias EBS.
 * Autor: Ing. J Sebastian Vargas S
 */

import { ChangeDetectionStrategy, Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EbsService, EbsHomeVisitSummary, EbsTerritorySummary } from '../../core/services/ebs.service';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { SesaSkeletonComponent } from '../../shared/components/sesa-skeleton/sesa-skeleton.component';
import { SesaEmptyStateComponent } from '../../shared/components/sesa-empty-state/sesa-empty-state.component';

@Component({
  standalone: true,
  selector: 'sesa-ebs-visitas',
  imports: [
    CommonModule,
    FormsModule,
    SesaCardComponent,
    SesaSkeletonComponent,
    SesaEmptyStateComponent,
  ],
  templateUrl: './ebs-visitas.page.html',
  styleUrl: './ebs-visitas.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EbsVisitasPageComponent {
  private readonly ebs = inject(EbsService);

  readonly visits = signal<EbsHomeVisitSummary[]>([]);
  readonly territories = signal<EbsTerritorySummary[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly filterVisitType = signal<string>('');

  selectedTerritoryId: number | null = null;
  diasFiltro = 90;

  /** Visitas filtradas por tipo (client-side). */
  readonly visitsFiltered = computed(() => {
    const list = this.visits();
    const type = this.filterVisitType();
    if (!type) return list;
    return list.filter((v) => (v.visitType || '').toUpperCase() === type.toUpperCase());
  });

  readonly VISIT_TYPE_OPTIONS: { value: string; label: string }[] = [
    { value: '', label: 'Todos los tipos' },
    { value: 'DOMICILIARIA_APS', label: 'Domiciliaria APS' },
    { value: 'SEGUIMIENTO', label: 'Seguimiento' },
    { value: 'ESTRATIFICACION', label: 'Estratificación de riesgo' },
    { value: 'PROMOCION', label: 'Promoción y prevención' },
  ];

  visitTypeLabel(type: string): string {
    const opt = this.VISIT_TYPE_OPTIONS.find((o) => o.value === (type || '').toUpperCase());
    return opt?.label ?? type ?? '';
  }

  constructor() {
    this.loadTerritories();
    this.loadVisits();
  }

  loadTerritories(): void {
    this.ebs.listTerritories({}).subscribe({
      next: (list) => this.territories.set(list ?? []),
      error: () => {},
    });
  }

  loadVisits(): void {
    this.loading.set(true);
    this.error.set(null);
    const to = new Date();
    const from = new Date();
    from.setDate(from.getDate() - this.diasFiltro);
    const params: { territoryId?: number; dateFrom?: string; dateTo?: string } = {
      dateFrom: from.toISOString(),
      dateTo: to.toISOString(),
    };
    if (this.selectedTerritoryId != null) params.territoryId = this.selectedTerritoryId;

    this.ebs.listHomeVisits(params).subscribe({
      next: (list) => {
        this.visits.set(list ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar las visitas');
        this.loading.set(false);
      },
    });
  }

  onFilterChange(): void {
    this.loadVisits();
  }
}
