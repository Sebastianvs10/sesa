import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { ImagenDiagnosticaDto, ImagenDiagnosticaService } from '../../core/services/imagen-diagnostica.service';

@Component({
  standalone: true,
  selector: 'sesa-imagenes-diagnosticas-page',
  imports: [CommonModule, FormsModule, SesaCardComponent],
  templateUrl: './imagenes-diagnosticas.page.html',
  styleUrl: './imagenes-diagnosticas.page.scss',
})
export class ImagenesDiagnosticasPageComponent {
  private readonly imagenService = inject(ImagenDiagnosticaService);

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
      error: (err) => (this.error = err?.error?.error || 'No se pudieron cargar imágenes'),
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
        this.buscar();
      },
      error: (err) => (this.error = err?.error?.error || 'No se pudo registrar imagen'),
    });
  }
}
