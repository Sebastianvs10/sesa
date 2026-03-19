/**
 * S10: Cuestionario pre-consulta (ePRO) — formulario para el paciente.
 * Autor: Ing. J Sebastian Vargas S
 */
import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PortalPacienteService } from '../../core/services/portal-paciente.service';

@Component({
  standalone: true,
  selector: 'sesa-portal-cuestionario-cita',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './portal-cuestionario-cita.page.html',
  styleUrl: './portal-cuestionario-cita.page.scss',
})
export class PortalCuestionarioCitaPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly portalService = inject(PortalPacienteService);

  citaId = signal<number | null>(null);
  enviando = signal(false);
  enviado = signal(false);
  error = signal<string | null>(null);

  motivoPalabras = '';
  dolorEva: number | null = null;
  ansiedadEva: number | null = null;
  medicamentosActuales = '';
  alergiasReferidas = '';

  readonly rangoEva = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10];

  constructor() {
    const id = this.route.snapshot.paramMap.get('citaId');
    if (id) this.citaId.set(parseInt(id, 10));
  }

  enviar(): void {
    const id = this.citaId();
    if (id == null) {
      this.error.set('Cita no válida.');
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    this.portalService.enviarCuestionarioPreconsulta(id, {
      motivoPalabras: this.motivoPalabras.trim() || undefined,
      dolorEva: this.dolorEva ?? undefined,
      ansiedadEva: this.ansiedadEva ?? undefined,
      medicamentosActuales: this.medicamentosActuales.trim() || undefined,
      alergiasReferidas: this.alergiasReferidas.trim() || undefined,
    }).subscribe({
      next: () => {
        this.enviado.set(true);
        this.enviando.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message || err?.message || 'Error al enviar.');
        this.enviando.set(false);
      },
    });
  }

  volver(): void {
    this.router.navigate(['/portal/inicio']);
  }
}
