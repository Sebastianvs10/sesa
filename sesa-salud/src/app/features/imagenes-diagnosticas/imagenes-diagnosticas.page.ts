/**
 * Imágenes Diagnósticas — skeleton, toast.
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
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
}
