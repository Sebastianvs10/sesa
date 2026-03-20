/**
 * Imágenes Diagnósticas — skeleton, toast.
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { ImagenDiagnosticaDto, ImagenDiagnosticaService } from '../../core/services/imagen-diagnostica.service';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';

@Component({
  standalone: true,
  selector: 'sesa-imagenes-diagnosticas-page',
  imports: [CommonModule, FormsModule, SesaCardComponent],
  templateUrl: './imagenes-diagnosticas.page.html',
  styleUrl: './imagenes-diagnosticas.page.scss',
})
export class ImagenesDiagnosticasPageComponent {
  private readonly imagenService = inject(ImagenDiagnosticaService);
  private readonly toast = inject(SesaToastService);

  atencionId: number | null = null;
  imagenes: ImagenDiagnosticaDto[] = [];
  error: string | null = null;

  globalList = signal<ImagenDiagnosticaDto[]>([]);
  globalLoading = signal(false);
  globalTotalElements = signal(0);
  globalTotalPages = signal(0);
  globalPage = signal(0);
  globalPageSize = 20;
  globalFilters = {
    pacienteId: null as number | null,
    atencionId: null as number | null,
    tipo: '',
    fechaDesde: '',
    fechaHasta: '',
  };

  form = {
    atencionId: '' as string,
    tipo: '',
    resultado: '',
    urlArchivo: '',
  };

  buscar(): void {
    if (!this.atencionId) {
      this.error = 'Ingresa ID de atención';
      return;
    }
    this.error = null;
    this.imagenService.listByAtencion(this.atencionId).subscribe({
      next: (res) => (this.imagenes = res ?? []),
      error: (err) => {
        this.error = err?.error?.error || 'No se pudieron cargar imágenes';
        this.toast.error(this.error!, 'Error de carga');
      },
    });
  }

  crear(): void {
    const atencionId = Number(this.form.atencionId);
    if (!atencionId) {
      this.error = 'Atención es obligatoria';
      return;
    }
    this.error = null;
    this.imagenService.create({
      atencionId,
      tipo: this.form.tipo || undefined,
      resultado: this.form.resultado || undefined,
      urlArchivo: this.form.urlArchivo || undefined,
    }).subscribe({
      next: () => {
        this.atencionId = atencionId;
        this.form.tipo = '';
        this.form.resultado = '';
        this.form.urlArchivo = '';
        this.toast.success('Imagen diagnóstica registrada correctamente.', 'Imagen creada');
        this.buscar();
      },
      error: (err) => {
        this.error = err?.error?.error || 'No se pudo registrar imagen';
        this.toast.error(this.error!, 'Error');
      },
    });
  }

  loadGlobal(): void {
    this.globalLoading.set(true);
    const fd = this.globalFilters.fechaDesde ? new Date(this.globalFilters.fechaDesde).toISOString() : undefined;
    const fh = this.globalFilters.fechaHasta ? new Date(this.globalFilters.fechaHasta).toISOString() : undefined;
    this.imagenService.listGlobal({
      pacienteId: this.globalFilters.pacienteId ?? undefined,
      atencionId: this.globalFilters.atencionId ?? undefined,
      tipo: this.globalFilters.tipo || undefined,
      fechaDesde: fd,
      fechaHasta: fh,
      page: this.globalPage(),
      size: this.globalPageSize,
    }).subscribe({
      next: (res) => {
        this.globalList.set(res.content ?? []);
        this.globalTotalElements.set(res.totalElements ?? 0);
        this.globalTotalPages.set(res.totalPages ?? 0);
        this.globalPage.set(res.number ?? 0);
        this.globalLoading.set(false);
      },
      error: () => {
        this.globalList.set([]);
        this.globalLoading.set(false);
        this.toast.error('Error al cargar listado global.', 'Error');
      },
    });
  }

  setGlobalPage(p: number): void {
    if (p >= 0 && p < this.globalTotalPages()) {
      this.globalPage.set(p);
      this.loadGlobal();
    }
  }

  formatFecha(v: string | undefined): string {
    if (!v) return '—';
    try {
      const d = new Date(v);
      return d.toLocaleDateString('es-CO', { dateStyle: 'short' }) + ' ' + d.toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' });
    } catch {
      return v;
    }
  }
}
