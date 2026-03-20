/**
 * Lista de Pacientes — Premium SaaS redesign con sidepanel contextual.
 * Autor: Ing. J Sebastian Vargas S
 */
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import {
  PacienteService,
  PacienteDto,
  PageResponse,
} from '../../core/services/paciente.service';
import { AuthService } from '../../core/services/auth.service';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';
import { SesaConfirmDialogService } from '../../shared/components/sesa-confirm-dialog/sesa-confirm-dialog.component';

@Component({
  standalone: true,
  selector: 'sesa-pacientes-list-page',
  imports: [FormsModule, RouterLink, DatePipe],
  templateUrl: './pacientes-list.page.html',
  styleUrl: './pacientes-list.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PacientesListPageComponent implements OnInit {
  private readonly pacienteService = inject(PacienteService);
  readonly authService             = inject(AuthService);
  private readonly toast           = inject(SesaToastService);
  private readonly confirmDialog   = inject(SesaConfirmDialogService);

  pacientes: PacienteDto[] = [];
  totalElements = 0;
  page          = 0;
  size          = 20;
  searchQ       = '';
  filterActivo: boolean | null = null;

  readonly loading          = signal(false);
  readonly error            = signal<string | null>(null);
  readonly deleting         = signal<number | null>(null);
  readonly selectedPaciente = signal<PacienteDto | null>(null);

  readonly canCreate = computed(() => {
    const role = this.authService.currentUser()?.role ?? '';
    return ['ADMIN', 'SUPERADMINISTRADOR', 'MEDICO', 'RECEPCIONISTA'].includes(role);
  });

  readonly canDelete = computed(() => {
    const role = this.authService.currentUser()?.role ?? '';
    return ['ADMIN', 'SUPERADMINISTRADOR'].includes(role);
  });

  get totalPages(): number {
    return this.size > 0 ? Math.ceil(this.totalElements / this.size) || 1 : 0;
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    const q = this.searchQ.trim() || undefined;
    this.pacienteService.list(this.page, this.size, q, this.filterActivo).subscribe({
      next: (res: PageResponse<PacienteDto>) => {
        this.pacientes     = res.content ?? [];
        this.totalElements = res.totalElements ?? 0;
        this.loading.set(false);
      },
      error: (err) => {
        const msg = err.error?.error || err.message || 'Error al cargar pacientes';
        this.error.set(msg);
        this.loading.set(false);
        this.toast.error(msg, 'Error');
      },
    });
  }

  onSearch(): void            { this.page = 0; this.load(); }
  onFilterActivoChange(): void { this.page = 0; this.load(); }
  openPanel(p: PacienteDto): void { this.selectedPaciente.set(p); }
  closePanel(): void              { this.selectedPaciente.set(null); }

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
        if (this.selectedPaciente()?.id === id) this.closePanel();
        this.load();
      },
      error: (e) => {
        this.deleting.set(null);
        this.toast.error(e.error?.error || 'Error al eliminar el paciente.', 'Error');
      },
    });
  }

  nextPage(): void { if ((this.page + 1) * this.size < this.totalElements) { this.page++; this.load(); } }
  prevPage(): void { if (this.page > 0) { this.page--; this.load(); } }

  patientInitials(p: PacienteDto): string {
    const f = (p.nombres   || '').trim().charAt(0).toUpperCase();
    const l = (p.apellidos || '').trim().charAt(0).toUpperCase();
    return (f + l) || '?';
  }

  patientFullName(p: PacienteDto): string {
    return [p.nombres, p.apellidos].filter(Boolean).join(' ');
  }

  avatarGradient(name: string): string {
    const palettes: [string, string][] = [
      ['#0d9488', '#22c55e'],
      ['#1f6ae1', '#0d9488'],
      ['#7c3aed', '#a855f7'],
      ['#059669', '#10b981'],
      ['#d97706', '#f59e0b'],
      ['#0891b2', '#38bdf8'],
      ['#db2777', '#f472b6'],
    ];
    const idx = (name.charCodeAt(0) || 0) % palettes.length;
    const [from, to] = palettes[idx];
    return `linear-gradient(135deg, ${from}, ${to})`;
  }
}
