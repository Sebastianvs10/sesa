/**
 * Página Territorios y hogares EBS — microterritorios asignados y listado de hogares.
 * Autor: Ing. J Sebastian Vargas S
 */

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
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
import {
  IgacDepartamento,
  IgacMunicipio,
  IgacVereda,
  IgacService,
} from '../../core/services/igac.service';
import { EbsTerritoriosMapComponent } from './ebs-territorios-map.component';

export type EbsViewMode = 'list' | 'map';

@Component({
  standalone: true,
  selector: 'sesa-ebs-territorios',
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    SesaCardComponent,
    SesaSkeletonComponent,
    SesaEmptyStateComponent,
    EbsTerritoriosMapComponent,
  ],
  templateUrl: './ebs-territorios.page.html',
  styleUrl: './ebs-territorios.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EbsTerritoriosPageComponent implements OnInit {
  private readonly ebs = inject(EbsService);
  private readonly igac = inject(IgacService);
  private readonly toast = inject(SesaToastService);
  private readonly route = inject(ActivatedRoute);

  readonly territories = signal<EbsTerritorySummary[]>([]);
  readonly households = signal<EbsHouseholdSummary[]>([]);
  readonly loadingTerritories = signal(false);
  readonly loadingHouseholds = signal(false);
  readonly selectedTerritoryId = signal<number | null>(null);
  readonly error = signal<string | null>(null);

  /** Panel IGAC: asignar límites oficiales al territorio seleccionado */
  readonly showIgacPanel = signal(false);
  readonly igacDepartamentos = signal<IgacDepartamento[]>([]);
  readonly igacMunicipios = signal<IgacMunicipio[]>([]);
  readonly igacVeredas = signal<IgacVereda[]>([]);
  readonly savingIgac = signal(false);
  igacDepCodigo = '';
  igacMunCodigo = '';
  igacVeredaCodigo = '';

  riskFilter: EbsRiskLevel | 'TODOS' = 'TODOS';
  visitStatusFilter: EbsVisitState | 'TODOS' = 'TODOS';

  /** Vista principal: lista de hogares o mapa. */
  viewMode: EbsViewMode = 'list';
  /** Hogar seleccionado desde el mapa (panel de acciones). */
  readonly selectedHouseholdFromMap = signal<EbsHouseholdSummary | null>(null);

  readonly selectedTerritory = computed<EbsTerritorySummary | null>(() => {
    const id = this.selectedTerritoryId();
    if (!id) return null;
    return this.territories().find((t) => t.id === id) ?? null;
  });

  ngOnInit(): void {
    this.route.queryParams.subscribe((qp) => {
      const id = qp['territorio'];
      if (id != null && id !== '') {
        const num = Number(id);
        if (!isNaN(num)) {
          this.selectedTerritoryId.set(num);
          // Si ya tenemos territorios cargados y este id está en la lista, cargar hogares
          const list = this.territories();
          if (list.length > 0 && list.some((t) => t.id === num)) {
            this.loadHouseholds();
          }
        }
      }
    });
    this.loadTerritories();
  }

  loadTerritories(): void {
    this.loadingTerritories.set(true);
    this.error.set(null);
    this.ebs.listTerritories({ riskLevel: this.riskFilter }).subscribe({
      next: (list) => {
        const items = list ?? [];
        this.territories.set(items);
        const currentId = this.selectedTerritoryId();
        if (items.length === 0) {
          this.households.set([]);
        } else if (!currentId) {
          this.selectTerritory(items[0].id);
        } else if (items.some((t) => t.id === currentId)) {
          this.loadHouseholds();
        } else {
          this.selectedTerritoryId.set(items[0].id);
          this.loadHouseholds();
        }
        this.loadingTerritories.set(false);
      },
      error: (err) => {
        this.loadingTerritories.set(false);
        const msg = err?.error?.message ?? 'No se pudieron cargar los microterritorios EBS.';
        this.error.set(msg);
        this.toast.error(msg, 'EBS');
      },
    });
  }

  selectTerritory(id: number): void {
    if (this.selectedTerritoryId() === id) return;
    this.selectedTerritoryId.set(id);
    this.loadHouseholds();
  }

  reloadHouseholds(): void {
    if (this.selectedTerritoryId()) this.loadHouseholds();
  }

  private loadHouseholds(): void {
    const territoryId = this.selectedTerritoryId();
    if (!territoryId) return;
    this.loadingHouseholds.set(true);
    this.households.set([]);
    this.ebs
      .listHouseholds(territoryId, { riskLevel: this.riskFilter, visitStatus: this.visitStatusFilter })
      .subscribe({
        next: (rows) => {
          this.households.set(rows ?? []);
          this.loadingHouseholds.set(false);
        },
        error: (err) => {
          this.loadingHouseholds.set(false);
          this.households.set([]);
          const msg = err?.error?.message ?? err?.error?.error ?? 'No se pudieron cargar los hogares.';
          this.toast.error(msg, 'EBS');
        },
      });
  }

  nuevaVisita(household: EbsHouseholdSummary): void {
    this.ebs
      .createHomeVisit({
        householdId: household.id,
        visitDate: new Date().toISOString(),
        visitType: 'DOMICILIARIA_APS',
      })
      .subscribe({
        next: (res: unknown) => {
          const body = res as { offline?: boolean };
          if (body?.offline) {
            this.toast.info('Visita encolada. Se sincronizará con conexión.', 'EBS');
          } else {
            this.toast.success('Visita domiciliaria creada.', 'EBS');
            this.reloadHouseholds();
          }
        },
        error: (err) => {
          this.toast.error(err?.error?.message ?? 'No se pudo crear la visita.', 'EBS');
        },
      });
  }

  labelRisk(level?: EbsRiskLevel): string {
    switch (level) {
      case 'ALTO': return 'Riesgo alto';
      case 'MUY_ALTO': return 'Riesgo muy alto';
      case 'MEDIO': return 'Riesgo medio';
      case 'BAJO': return 'Riesgo bajo';
      default: return 'Sin estratificar';
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
      case 'MUY_ALTO': return 'ebs-chip ebs-chip-danger';
      case 'MEDIO': return 'ebs-chip ebs-chip-warning';
      case 'BAJO': return 'ebs-chip ebs-chip-success';
      default: return 'ebs-chip ebs-chip-muted';
    }
  }

  /** Ruta IGAC para mostrar (ej. Antioquia > Medellín > Santa Elena). */
  igacPath(t: EbsTerritorySummary): string {
    const parts = [t.igacDepartamentoNombre, t.igacMunicipioNombre, t.igacVeredaNombre].filter(Boolean);
    return parts.length ? parts.join(' › ') : '';
  }

  toggleIgacPanel(): void {
    const next = !this.showIgacPanel();
    this.showIgacPanel.set(next);
    if (next) {
      this.igacDepCodigo = this.selectedTerritory()?.igacDepartamentoCodigo ?? '';
      this.igacMunCodigo = this.selectedTerritory()?.igacMunicipioCodigo ?? '';
      this.igacVeredaCodigo = this.selectedTerritory()?.igacVeredaCodigo ?? '';
      this.igac.listDepartamentos().subscribe({
        next: (list) => this.igacDepartamentos.set(list ?? []),
        error: (err) => {
          this.igacDepartamentos.set([]);
          this.toast.error(err?.error?.message ?? 'No se pudieron cargar los departamentos IGAC.', 'IGAC');
        },
      });
      this.igacMunicipios.set([]);
      this.igacVeredas.set([]);
      if (this.igacDepCodigo) {
        this.igac.listMunicipios(this.igacDepCodigo).subscribe({
          next: (list) => this.igacMunicipios.set(list ?? []),
          error: () => this.igacMunicipios.set([]),
        });
      }
      if (this.igacMunCodigo) {
        this.igac.listVeredas(this.igacMunCodigo).subscribe({
          next: (list) => this.igacVeredas.set(list ?? []),
          error: () => this.igacVeredas.set([]),
        });
      }
    }
  }

  onIgacDepartamentoChange(): void {
    this.igacMunCodigo = '';
    this.igacVeredaCodigo = '';
    this.igacVeredas.set([]);
    if (!this.igacDepCodigo) {
      this.igacMunicipios.set([]);
      return;
    }
    this.igac.listMunicipios(this.igacDepCodigo).subscribe({
      next: (list) => this.igacMunicipios.set(list ?? []),
      error: () => this.igacMunicipios.set([]),
    });
  }

  onIgacMunicipioChange(): void {
    this.igacVeredaCodigo = '';
    if (!this.igacMunCodigo) {
      this.igacVeredas.set([]);
      return;
    }
    this.igac.listVeredas(this.igacMunCodigo).subscribe({
      next: (list) => this.igacVeredas.set(list ?? []),
      error: () => this.igacVeredas.set([]),
    });
  }

  /** Hogares con coordenadas para el mapa. */
  readonly householdsWithCoords = computed(() =>
    this.households().filter((h) => h.latitude != null && h.longitude != null)
  );

  onHouseholdSelectFromMap(household: EbsHouseholdSummary): void {
    this.selectedHouseholdFromMap.set(household);
  }

  closeMapHouseholdPanel(): void {
    this.selectedHouseholdFromMap.set(null);
  }

  saveIgac(): void {
    const territoryId = this.selectedTerritoryId();
    if (!territoryId) return;
    this.savingIgac.set(true);
    this.ebs
      .updateTerritoryIgac(territoryId, {
        igacDepartamentoCodigo: this.igacDepCodigo || undefined,
        igacMunicipioCodigo: this.igacMunCodigo || undefined,
        igacVeredaCodigo: this.igacVeredaCodigo || undefined,
      })
      .subscribe({
        next: () => {
          this.savingIgac.set(false);
          this.showIgacPanel.set(false);
          this.toast.success('Límites IGAC actualizados.', 'EBS');
          this.loadTerritories();
        },
        error: (err) => {
          this.savingIgac.set(false);
          this.toast.error(err?.error?.message ?? 'No se pudieron guardar los límites.', 'EBS');
        },
      });
  }
}
