import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EmpresaService, EmpresaDto, PageResponse } from '../../core/services/empresa.service';
import { AuthService } from '../../core/services/auth.service';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';

@Component({
  standalone: true,
  selector: 'sesa-empresas-list-page',
  imports: [CommonModule, RouterLink, SesaCardComponent],
  templateUrl: './empresas-list.page.html',
  styleUrl: './empresas-list.page.scss',
})
export class EmpresasListPageComponent implements OnInit {
  empresas: EmpresaDto[] = [];
  totalElements = 0;
  page = 0;
  size = 20;
  loading = false;
  error: string | null = null;

  constructor(
    private empresaService: EmpresaService,
    public authService: AuthService,
  ) {}

  /** Solo SUPERADMINISTRADOR puede crear empresas */
  get isSuperAdmin() {
    return this.authService.isSuperAdmin;
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = null;
    this.empresaService.list(this.page, this.size).subscribe({
      next: (res: PageResponse<EmpresaDto>) => {
        this.empresas = res.content ?? [];
        this.totalElements = res.totalElements ?? 0;
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.error || err.message || 'Error al cargar empresas';
        this.loading = false;
      },
    });
  }

  delete(id: number, razonSocial: string): void {
    if (!confirm(`¿Eliminar la empresa "${razonSocial}"?`)) return;
    this.empresaService.delete(id).subscribe({
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
