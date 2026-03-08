/**
 * Lista de Pacientes — skeleton, confirm dialog, spinner, toast CRUD.
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faUsers, faPlus } from '@fortawesome/free-solid-svg-icons';
import { SesaPageHeaderComponent } from '../../shared/components/sesa-page-header/sesa-page-header.component';
import { PacienteService, PacienteDto, PageResponse } from '../../core/services/paciente.service';
import { AuthService } from '../../core/services/auth.service';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';
import { SesaConfirmDialogService } from '../../shared/components/sesa-confirm-dialog/sesa-confirm-dialog.component';

@Component({
  standalone: true,
  selector: 'sesa-pacientes-list-page',
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    FontAwesomeModule,
    SesaPageHeaderComponent,
  ],
  templateUrl: './pacientes-list.page.html',
  styleUrl: './pacientes-list.page.scss',
})
export class PacientesListPageComponent implements OnInit {
  private readonly pacienteService = inject(PacienteService);
  readonly authService = inject(AuthService);
  private readonly toast = inject(SesaToastService);
  private readonly confirmDialog = inject(SesaConfirmDialogService);

  readonly faUsers = faUsers;
  readonly faPlus = faPlus;

  pacientes: PacienteDto[] = [];
  totalElements = 0;
  page = 0;
  size = 20;
  searchQ = '';
  /** Filtro por estado: null = todos, true = solo activos, false = solo inactivos */
  filterActivo: boolean | null = null;
  loading = false;
  deleting = signal<number | null>(null);
  error: string | null = null;

  get canCreate(): boolean {
    const role = this.authService.currentUser()?.role ?? '';
    return ['ADMIN', 'SUPERADMINISTRADOR', 'MEDICO', 'RECEPCIONISTA'].includes(role);
  }

  get canDelete(): boolean {
    const role = this.authService.currentUser()?.role ?? '';
    return ['ADMIN', 'SUPERADMINISTRADOR'].includes(role);
  }

  get totalPages(): number {
    if (this.size <= 0) return 0;
    return Math.ceil(this.totalElements / this.size) || 1;
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = null;
    const obs = this.searchQ.trim()
      ? this.pacienteService.list(this.page, this.size, this.searchQ, this.filterActivo)
      : this.pacienteService.list(this.page, this.size, undefined, this.filterActivo);
    obs.subscribe({
      next: (res: PageResponse<PacienteDto>) => {
        this.pacientes = res.content ?? [];
        this.totalElements = res.totalElements ?? 0;
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.error || err.message || 'Error al cargar pacientes';
        this.loading = false;
        this.toast.error(this.error!, 'Error');
      },
    });
  }

  onSearch(): void {
    this.page = 0;
    this.load();
  }

  onFilterActivoChange(): void {
    this.page = 0;
    this.load();
  }

  async delete(id: number, nombre: string): Promise<void> {
    const ok = await this.confirmDialog.confirm({
      title: 'Eliminar paciente',
      message: `¿Estás seguro de que deseas eliminar al paciente "${nombre}"? Esta acción no se puede deshacer.`,
      type: 'danger',
      confirmLabel: 'Eliminar',
    });
    if (!ok) return;

    this.deleting.set(id);
    this.pacienteService.delete(id).subscribe({
      next: () => {
        this.deleting.set(null);
        this.toast.success(`Paciente "${nombre}" eliminado correctamente.`, 'Eliminado');
        this.load();
      },
      error: (e) => {
        this.deleting.set(null);
        this.toast.error(e.error?.error || 'Error al eliminar el paciente.', 'Error');
      },
    });
  }

  nextPage(): void {
    if ((this.page + 1) * this.size < this.totalElements) {
      this.page++;
      this.load();
    }
  }

  prevPage(): void {
    if (this.page > 0) {
      this.page--;
      this.load();
    }
  }

  patientInitials(p: PacienteDto): string {
    const first = (p.nombres || '').trim().charAt(0).toUpperCase();
    const last  = (p.apellidos || '').trim().charAt(0).toUpperCase();
    return (first + last) || '?';
  }

  avatarStyle(name: string): Record<string, string> {
    const palettes: [string, string][] = [
      ['#2bb0a6', '#22c55e'],
      ['#1f6ae1', '#2bb0a6'],
      ['#7c3aed', '#a855f7'],
      ['#059669', '#10b981'],
      ['#d97706', '#f59e0b'],
      ['#0891b2', '#38bdf8'],
      ['#db2777', '#f472b6'],
    ];
    const idx = (name.charCodeAt(0) || 0) % palettes.length;
    const [from, to] = palettes[idx];
    return { background: `linear-gradient(135deg, ${from}, ${to})` };
  }
}
