/**
 * Evolución de Enfermería — registro de notas de enfermería por atención.
 * Res. 1995/1999 (HC), Decreto 1011/2006 (seguridad del paciente).
 * Autor: Ing. J Sebastian Vargas S
 */

import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import {
  NotaEnfermeriaService,
  NotaEnfermeriaDto,
  NotaEnfermeriaRequestDto,
} from '../../core/services/nota-enfermeria.service';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';

@Component({
  standalone: true,
  selector: 'sesa-evolucion-enfermeria-page',
  imports: [CommonModule, FormsModule, SesaCardComponent],
  templateUrl: './evolucion-enfermeria.page.html',
  styleUrl: './evolucion-enfermeria.page.scss',
})
export class EvolucionEnfermeriaPageComponent {
  private readonly notaService = inject(NotaEnfermeriaService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(SesaToastService);

  readonly atencionIdInput = signal<string>('');
  readonly notas = signal<NotaEnfermeriaDto[]>([]);
  readonly cargando = signal(false);
  readonly guardando = signal(false);
  readonly nuevaNotaTexto = signal('');
  readonly error = signal<string | null>(null);

  cargarNotas(): void {
    const id = this.atencionIdInput().trim();
    const num = id ? parseInt(id, 10) : NaN;
    if (!id || isNaN(num) || num < 1) {
      this.error.set('Ingrese un ID de atención válido.');
      return;
    }
    this.error.set(null);
    this.cargando.set(true);
    this.notaService.listarPorAtencion(num).subscribe({
      next: (list) => {
        this.notas.set(Array.isArray(list) ? list : []);
        this.cargando.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.error || 'No se pudieron cargar las notas.');
        this.toast.error(this.error()!, 'Error');
        this.cargando.set(false);
      },
    });
  }

  registrarNota(): void {
    const id = this.atencionIdInput().trim();
    const atencionId = id ? parseInt(id, 10) : NaN;
    const nota = this.nuevaNotaTexto().trim();
    if (!nota || isNaN(atencionId) || atencionId < 1) {
      this.error.set('ID de atención y contenido de la nota son obligatorios.');
      return;
    }
    this.error.set(null);
    this.guardando.set(true);
    const dto: NotaEnfermeriaRequestDto = {
      atencionId,
      nota,
      profesionalId: this.auth.currentUser()?.personalId ?? undefined,
    };
    this.notaService.crear(dto).subscribe({
      next: (creada) => {
        this.notas.update((list) => [creada, ...list]);
        this.nuevaNotaTexto.set('');
        this.toast.success('Nota de enfermería registrada.', 'Registrada');
        this.guardando.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.error || 'No se pudo registrar la nota.');
        this.toast.error(this.error()!, 'Error');
        this.guardando.set(false);
      },
    });
  }

  formatFecha(fecha?: string): string {
    if (!fecha) return '—';
    const d = new Date(fecha);
    return isNaN(d.getTime()) ? fecha : d.toLocaleString('es-CO', { dateStyle: 'short', timeStyle: 'short' });
  }
}
