/**
 * Lista de Empresas — confirm dialog, toast CRUD, skeleton loading.
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EmpresaService, EmpresaDto, PageResponse } from '../../core/services/empresa.service';
import { AuthService } from '../../core/services/auth.service';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { SesaSkeletonComponent } from '../../shared/components/sesa-skeleton/sesa-skeleton.component';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';
import { SesaConfirmDialogService } from '../../shared/components/sesa-confirm-dialog/sesa-confirm-dialog.component';

@Component({
  standalone: true,
  selector: 'sesa-empresas-list-page',
  imports: [CommonModule, RouterLink, SesaCardComponent, SesaSkeletonComponent],
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

  private readonly toast = inject(SesaToastService);
  private readonly confirmDialog = inject(SesaConfirmDialogService);

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

  async delete(id: number, razonSocial: string): Promise<void> {
    const ok = await this.confirmDialog.confirm({
      title: 'Eliminar empresa',
      message: `¿Estás seguro de eliminar la empresa "${razonSocial}"? Esta acción es irreversible.`,
      type: 'danger',
    });
    if (!ok) return;
    this.empresaService.delete(id).subscribe({
      next: () => {
        this.toast.success(`Empresa "${razonSocial}" eliminada.`, 'Eliminada');
        this.load();
      },
      error: (e) => {
        const msg = e.error?.error || 'Error al eliminar la empresa';
        this.toast.error(msg, 'Error');
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
}
