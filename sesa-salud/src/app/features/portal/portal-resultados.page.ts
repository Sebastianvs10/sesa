/**
 * S14: Resultados con interpretación en lenguaje sencillo y descarga PDF (portal del paciente).
 * Autor: Ing. J Sebastian Vargas S
 */

import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  PortalPacienteService,
  OrdenConResultadoPortalDto,
} from '../../core/services/portal-paciente.service';

@Component({
  selector: 'sesa-portal-resultados',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  templateUrl: './portal-resultados.page.html',
  styleUrl: './portal-resultados.page.scss',
})
export class PortalResultadosPageComponent implements OnInit {
  private readonly portal = inject(PortalPacienteService);

  readonly loading = signal(true);
  readonly error = signal(false);
  readonly ordenes = signal<OrdenConResultadoPortalDto[]>([]);
  readonly descargandoId = signal<number | null>(null);

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.error.set(false);
    this.portal.getOrdenesConResultados().subscribe({
      next: (list) => {
        this.ordenes.set(list ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set(true);
      },
    });
  }

  descargarPdf(orden: OrdenConResultadoPortalDto): void {
    const id = orden.ordenId;
    this.descargandoId.set(id);
    this.portal.getPdfOrden(id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `orden-${id}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
        this.descargandoId.set(null);
      },
      error: () => this.descargandoId.set(null),
    });
  }
}
