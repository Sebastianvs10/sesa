/**
 * Asignación territorial — Coordinador: equipos por territorio y brigadas.
 * Autor: Ing. J Sebastian Vargas S
 */

import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { EbsService, EbsTerritorySummary, EbsBrigadeDto } from '../../core/services/ebs.service';
import { PersonalService, PersonalDto } from '../../core/services/personal.service';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { SesaEmptyStateComponent } from '../../shared/components/sesa-empty-state/sesa-empty-state.component';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';

@Component({
  standalone: true,
  selector: 'sesa-ebs-asignacion',
  imports: [CommonModule, FormsModule, RouterLink, SesaCardComponent, SesaEmptyStateComponent],
  templateUrl: './ebs-asignacion.page.html',
  styleUrl: './ebs-asignacion.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EbsAsignacionPageComponent {
  private readonly ebs = inject(EbsService);
  private readonly personalService = inject(PersonalService);
  private readonly toast = inject(SesaToastService);

  readonly territories = signal<EbsTerritorySummary[]>([]);
  readonly personalList = signal<PersonalDto[]>([]);
  readonly brigadesByTerritory = signal<Map<number, EbsBrigadeDto[]>>(new Map());
  readonly loading = signal(true);
  readonly savingTeam = signal(false);
  readonly savingBrigade = signal(false);

  /** Territorio cuyo equipo se está editando */
  readonly editingTeamTerritoryId = signal<number | null>(null);
  /** IDs de personal seleccionados para el territorio en edición */
  readonly selectedTeamIds = signal<Set<number>>(new Set());
  /** Brigada en edición (team) */
  readonly editingBrigadeId = signal<number | null>(null);
  readonly selectedBrigadeTeamIds = signal<Set<number>>(new Set());

  /** Mostrar formulario nueva brigada (territoryId o null) */
  readonly showNewBrigadeForTerritoryId = signal<number | null>(null);
  newBrigadeName = '';
  newBrigadeDateStart = '';
  newBrigadeDateEnd = '';
  newBrigadeTeamIds: number[] = [];

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.ebs.listTerritories({}).subscribe({
      next: (list) => {
        this.territories.set(list ?? []);
        this.loading.set(false);
        this.loadBrigadesForAllTerritories(list ?? []);
      },
      error: () => {
        this.loading.set(false);
        this.toast.error('Error al cargar territorios.');
      },
    });
    this.personalService.list(0, 200).subscribe({
      next: (res) => this.personalList.set(res.content ?? []),
    });
  }

  loadBrigadesForAllTerritories(territories: EbsTerritorySummary[]): void {
    const map = new Map<number, EbsBrigadeDto[]>();
    if (territories.length === 0) {
      this.ebs.listBrigades().subscribe({
        next: (all) => {
          all.forEach((b) => {
            const tid = b.territoryId!;
            if (!map.has(tid)) map.set(tid, []);
            map.get(tid)!.push(b);
          });
          this.brigadesByTerritory.set(map);
        },
      });
      return;
    }
    let pending = territories.length;
    territories.forEach((t) => {
      this.ebs.listBrigades(t.id).subscribe({
        next: (list) => {
          map.set(t.id, list ?? []);
          if (--pending === 0) this.brigadesByTerritory.set(new Map(map));
        },
      });
    });
  }

  openTeamEditor(territoryId: number): void {
    this.editingTeamTerritoryId.set(territoryId);
    this.ebs.getTerritoryTeam(territoryId).subscribe({
      next: (ids) => this.selectedTeamIds.set(new Set(ids)),
    });
  }

  closeTeamEditor(): void {
    this.editingTeamTerritoryId.set(null);
  }

  toggleTeamMember(id: number): void {
    const set = new Set(this.selectedTeamIds());
    if (set.has(id)) set.delete(id);
    else set.add(id);
    this.selectedTeamIds.set(set);
  }

  saveTerritoryTeam(): void {
    const tid = this.editingTeamTerritoryId();
    if (tid == null) return;
    this.savingTeam.set(true);
    const ids = Array.from(this.selectedTeamIds());
    this.ebs.setTerritoryTeam(tid, ids).subscribe({
      next: () => {
        this.savingTeam.set(false);
        this.editingTeamTerritoryId.set(null);
        this.toast.success('Equipo del territorio actualizado.');
      },
      error: () => {
        this.savingTeam.set(false);
        this.toast.error('Error al guardar equipo.');
      },
    });
  }

  openBrigadeTeamEditor(brigadeId: number): void {
    this.editingBrigadeId.set(brigadeId);
    this.ebs.getBrigadeTeam(brigadeId).subscribe({
      next: (ids) => this.selectedBrigadeTeamIds.set(new Set(ids)),
    });
  }

  closeBrigadeTeamEditor(): void {
    this.editingBrigadeId.set(null);
  }

  toggleBrigadeTeamMember(id: number): void {
    const set = new Set(this.selectedBrigadeTeamIds());
    if (set.has(id)) set.delete(id);
    else set.add(id);
    this.selectedBrigadeTeamIds.set(set);
  }

  saveBrigadeTeam(): void {
    const bid = this.editingBrigadeId();
    if (bid == null) return;
    this.savingTeam.set(true);
    const ids = Array.from(this.selectedBrigadeTeamIds());
    this.ebs.setBrigadeTeam(bid, ids).subscribe({
      next: () => {
        this.savingTeam.set(false);
        this.editingBrigadeId.set(null);
        this.toast.success('Equipo de la brigada actualizado.');
        this.load();
      },
      error: () => {
        this.savingTeam.set(false);
        this.toast.error('Error al guardar equipo.');
      },
    });
  }

  openNewBrigade(territoryId: number): void {
    this.showNewBrigadeForTerritoryId.set(territoryId);
    this.newBrigadeName = '';
    this.newBrigadeDateStart = new Date().toISOString().slice(0, 10);
    this.newBrigadeDateEnd = new Date().toISOString().slice(0, 10);
    this.newBrigadeTeamIds = [];
  }

  closeNewBrigade(): void {
    this.showNewBrigadeForTerritoryId.set(null);
  }

  createBrigade(territoryId: number): void {
    if (!this.newBrigadeName.trim()) {
      this.toast.warning('Nombre de brigada requerido.');
      return;
    }
    this.savingBrigade.set(true);
    this.ebs
      .createBrigade({
        name: this.newBrigadeName.trim(),
        territoryId,
        dateStart: this.newBrigadeDateStart || new Date().toISOString().slice(0, 10),
        dateEnd: this.newBrigadeDateEnd || new Date().toISOString().slice(0, 10),
        status: 'PROGRAMADA',
        teamMemberIds: this.newBrigadeTeamIds.length ? this.newBrigadeTeamIds : undefined,
      })
      .subscribe({
        next: () => {
          this.savingBrigade.set(false);
          this.closeNewBrigade();
          this.toast.success('Brigada creada.');
          this.load();
        },
        error: () => {
          this.savingBrigade.set(false);
          this.toast.error('Error al crear brigada.');
        },
      });
  }

  deleteBrigade(b: EbsBrigadeDto): void {
    if (!b.id || !confirm(`¿Eliminar brigada "${b.name}"?`)) return;
    this.ebs.deleteBrigade(b.id).subscribe({
      next: () => {
        this.toast.success('Brigada eliminada.');
        this.load();
      },
      error: () => this.toast.error('Error al eliminar.'),
    });
  }

  brigadesFor(territoryId: number): EbsBrigadeDto[] {
    return this.brigadesByTerritory().get(territoryId) ?? [];
  }

  fullName(p: PersonalDto): string {
    return [p.nombres, p.apellidos].filter(Boolean).join(' ').trim() || `#${p.id}`;
  }
}
