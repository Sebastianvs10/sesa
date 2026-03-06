/**
 * Página pública de verificación de receta electrónica por token (QR o enlace).
 * Ruta: /verificar-receta?t=TOKEN
 * Autor: Ing. J Sebastian Vargas S
 */

import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  OnInit,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { RecetaElectronicaService, RecetaVerificacionResponseDto } from '../../core/services/receta-electronica.service';

@Component({
  selector: 'sesa-verificar-receta',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  template: `
    <div class="ver-rec">
      <div class="ver-rec__card">
        <div class="ver-rec__brand">
          <span class="ver-rec__logo">SESA</span>
          <span class="ver-rec__sub">Verificación de receta electrónica</span>
        </div>

        @if (cargando()) {
          <div class="ver-rec__loading">
            <div class="ver-rec__spinner"></div>
            <p>Verificando receta…</p>
          </div>
        }

        @if (!cargando() && resultado(); as r) {
          @if (r.valida && r.receta) {
            <div class="ver-rec__ok">
              <div class="ver-rec__ok-icon">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>
                </svg>
              </div>
              <h1 class="ver-rec__ok-title">Receta válida</h1>
              <p class="ver-rec__ok-text">La receta ha sido emitida por SESA Salud y puede ser utilizada en farmacia.</p>

              <div class="ver-rec__data">
                <div class="ver-rec__row">
                  <span class="ver-rec__label">Paciente</span>
                  <span class="ver-rec__val">{{ r.receta.pacienteNombre }}</span>
                </div>
                <div class="ver-rec__row">
                  <span class="ver-rec__label">Médico</span>
                  <span class="ver-rec__val">{{ r.receta.medicoNombre }}</span>
                </div>
                <div class="ver-rec__row">
                  <span class="ver-rec__label">Fecha de emisión</span>
                  <span class="ver-rec__val">{{ r.receta.fechaEmision }}</span>
                </div>
                @if (r.receta.diagnostico) {
                  <div class="ver-rec__row">
                    <span class="ver-rec__label">Diagnóstico</span>
                    <span class="ver-rec__val">{{ r.receta.diagnostico }}</span>
                  </div>
                }
                <div class="ver-rec__meds">
                  <span class="ver-rec__label">Medicamentos</span>
                  <ul class="ver-rec__list">
                    @for (m of r.receta.medicamentos; track m.medicamento) {
                      <li>
                        <strong>{{ m.medicamento }}</strong>
                        @if (m.dosis || m.frecuencia || m.duracion) {
                          <span class="ver-rec__dosis"> — {{ m.dosis }} {{ m.frecuencia }} {{ m.duracion }}</span>
                        }
                      </li>
                    }
                  </ul>
                </div>
              </div>
            </div>
          } @else {
            <div class="ver-rec__invalid">
              <div class="ver-rec__invalid-icon">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>
                </svg>
              </div>
              <h1 class="ver-rec__invalid-title">Receta no válida</h1>
              <p class="ver-rec__invalid-text">{{ r.mensaje || 'El enlace ha expirado o no corresponde a una receta registrada.' }}</p>
            </div>
          }
        }

        @if (!cargando() && sinToken()) {
          <div class="ver-rec__invalid">
            <p class="ver-rec__invalid-text">Falta el código de verificación. Escanee el QR de la receta o use el enlace proporcionado.</p>
          </div>
        }
      </div>
    </div>
  `,
  styleUrl: './verificar-receta.page.scss',
})
export class VerificarRecetaPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly recetaService = inject(RecetaElectronicaService);

  readonly cargando = signal(true);
  readonly resultado = signal<RecetaVerificacionResponseDto | null>(null);
  readonly sinToken = computed(() => !this.route.snapshot.queryParamMap.get('t') && !this.cargando());

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('t');
    if (!token) {
      this.cargando.set(false);
      return;
    }
    this.recetaService.verificar(token).subscribe({
      next: (r) => {
        this.resultado.set(r);
        this.cargando.set(false);
      },
      error: () => {
        this.resultado.set({ valida: false, mensaje: 'Error al verificar. Compruebe su conexión.' });
        this.cargando.set(false);
      },
    });
  }
}
