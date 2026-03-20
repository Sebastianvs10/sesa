/**
 * Brigadas EBS — listado y creación.
 * Autor: Ing. J Sebastian Vargas S
 */

import { ChangeDetectionStrategy, Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { EbsService, EbsBrigadeDto } from '../../core/services/ebs.service';
import { PersonalService, PersonalDto } from '../../core/services/personal.service';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { SesaSkeletonComponent } from '../../shared/components/sesa-skeleton/sesa-skeleton.component';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';

@Component({
  standalone: true,
  selector: 'sesa-ebs-brigadas',
  imports: [CommonModule, FormsModule, RouterLink, SesaCardComponent, SesaSkeletonComponent],
  templateUrl: './ebs-brigadas.page.html',
  styleUrl: './ebs-brigadas.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EbsBrigadasPageComponent {
  private readonly ebs = inject(EbsService);
  private readonly personalService = inject(PersonalService);
  private readonly toast = inject(SesaToastService);

  readonly brigades = signal<EbsBrigadeDto[]>([]);
  readonly personalList = signal<PersonalDto[]>([]);
  readonly loading = signal(true);
  readonly savingBrigade = signal(false);
  readonly filterTerritoryId = signal<number | null>(null);
  readonly territories = signal<{ id: number; name: string }[]>([]);
  readonly showNewForm = signal(false);

  newBrigadeName = '';
  newBrigadeTerritoryId: number | null = null;
  newBrigadeDateStart = '';
  newBrigadeDateEnd = '';
  newBrigadeTeamIds: number[] = [];

  readonly filterStatus = signal<string>('');

  filteredBrigades = computed(() => {
    let list = this.brigades();
    const tid = this.filterTerritoryId();
    if (tid != null) list = list.filter((b) => b.territoryId === tid);
    const status = this.filterStatus();
    if (status) list = list.filter((b) => (b.status || '').toUpperCase() === status.toUpperCase());
    return list;
  });

  readonly STATUS_OPTIONS: { value: string; label: string }[] = [
    { value: '', label: 'Todos los estados' },
    { value: 'PROGRAMADA', label: 'Programada' },
    { value: 'EN_CURSO', label: 'En curso' },
    { value: 'REALIZADA', label: 'Realizada' },
    { value: 'CANCELADA', label: 'Cancelada' },
  ];

  constructor() {
    this.load();
    this.ebs.listTerritories({}).subscribe({
      next: (t) => {
        this.territories.set(t.map((x) => ({ id: x.id, name: x.name })));
        if (t.length > 0 && !this.newBrigadeTerritoryId) this.newBrigadeTerritoryId = t[0].id;
      },
    });
    this.personalService.list(0, 200).subscribe({
      next: (res) => this.personalList.set(res.content ?? []),
    });
  }

  openNewBrigade(): void {
    this.showNewForm.set(true);
    this.newBrigadeName = '';
    this.newBrigadeDateStart = new Date().toISOString().slice(0, 10);
    this.newBrigadeDateEnd = new Date().toISOString().slice(0, 10);
    const t = this.territories();
    this.newBrigadeTerritoryId = t.length > 0 ? t[0].id : null;
    this.newBrigadeTeamIds = [];
  }

  closeNewBrigade(): void {
    this.showNewForm.set(false);
  }

  createBrigade(): void {
    if (!this.newBrigadeName.trim() || this.newBrigadeTerritoryId == null) {
      this.toast.warning('Nombre y territorio son requeridos.');
      return;
    }
    this.savingBrigade.set(true);
    this.ebs
      .createBrigade({
        name: this.newBrigadeName.trim(),
        territoryId: this.newBrigadeTerritoryId,
        dateStart: this.newBrigadeDateStart || new Date().toISOString().slice(0, 10),
        dateEnd: this.newBrigadeDateEnd || new Date().toISOString().slice(0, 10),
        status: 'PROGRAMADA',
        teamMemberIds: this.newBrigadeTeamIds.length > 0 ? this.newBrigadeTeamIds : undefined,
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

  toggleNewBrigadeTeam(id: number): void {
    const idx = this.newBrigadeTeamIds.indexOf(id);
    if (idx >= 0) this.newBrigadeTeamIds.splice(idx, 1);
    else this.newBrigadeTeamIds.push(id);
    this.newBrigadeTeamIds = [...this.newBrigadeTeamIds];
  }

  fullName(p: PersonalDto): string {
    return [p.nombres, p.apellidos].filter(Boolean).join(' ').trim() || `#${p.id}`;
  }

  load(): void {
    this.loading.set(true);
    this.ebs.listBrigades(this.filterTerritoryId() ?? undefined).subscribe({
      next: (list) => {
        this.brigades.set(list ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.toast.error('Error al cargar brigadas');
      },
    });
  }

  onFilterChange(id: number | null): void {
    this.filterTerritoryId.set(id);
    this.load();
  }

  deleteBrigade(id: number, name: string): void {
    if (!confirm(`¿Eliminar brigada "${name}"?`)) return;
    this.ebs.deleteBrigade(id).subscribe({
      next: () => {
        this.toast.success('Brigada eliminada');
        this.load();
      },
      error: () => this.toast.error('Error al eliminar'),
    });
  }

  statusBadge(status: string): string {
    const s = (status || '').toUpperCase();
    if (s === 'PROGRAMADA') return 'programada';
    if (s === 'EN_CURSO' || s === 'REALIZADA') return 'realizada';
    return 'cancelada';
  }
}
