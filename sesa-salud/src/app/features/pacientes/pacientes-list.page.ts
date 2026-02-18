import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faUsers, faPlus, faClipboardList } from '@fortawesome/free-solid-svg-icons';
import { PacienteService, PacienteDto, PageResponse } from '../../core/services/paciente.service';
import { AuthService } from '../../core/services/auth.service';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';

@Component({
  standalone: true,
  selector: 'sesa-pacientes-list-page',
  imports: [CommonModule, FormsModule, RouterLink, FontAwesomeModule, SesaCardComponent],
  templateUrl: './pacientes-list.page.html',
  styleUrl: './pacientes-list.page.scss',
})
export class PacientesListPageComponent implements OnInit {
  private readonly pacienteService = inject(PacienteService);
  readonly authService = inject(AuthService);

  faUsers = faUsers;
  faPlus = faPlus;
  faClipboardList = faClipboardList;

  pacientes: PacienteDto[] = [];
  totalElements = 0;
  page = 0;
  size = 20;
  searchQ = '';
  loading = false;
  error: string | null = null;

  get canCreate(): boolean {
    const role = this.authService.currentUser()?.role ?? '';
    return role === 'ADMIN' || role === 'SUPERADMINISTRADOR' || role === 'MEDICO' || role === 'RECEPCIONISTA';
  }

  get canDelete(): boolean {
    const role = this.authService.currentUser()?.role ?? '';
    return role === 'ADMIN' || role === 'SUPERADMINISTRADOR';
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
      ? this.pacienteService.list(this.page, this.size, this.searchQ)
      : this.pacienteService.list(this.page, this.size);
    obs.subscribe({
      next: (res: PageResponse<PacienteDto>) => {
        this.pacientes = res.content ?? [];
        this.totalElements = res.totalElements ?? 0;
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.error || err.message || 'Error al cargar pacientes';
        this.loading = false;
      },
    });
  }

  onSearch(): void {
    this.page = 0;
    this.load();
  }

  delete(id: number, nombre: string): void {
    if (!confirm(`¿Eliminar al paciente "${nombre}"?`)) return;
    this.pacienteService.delete(id).subscribe({
      next: () => this.load(),
      error: (e) => alert(e.error?.error || 'Error al eliminar'),
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
}
