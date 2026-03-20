/**
 * Modal para emitir receta electrónica con QR verificable.
 * Se abre desde Consulta Médica (contexto cita) o desde Historia Clínica.
 * Autor: Ing. J Sebastian Vargas S
 */

import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  input,
  output,
  ViewChild,
  ElementRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FormulaMedicaDto } from '../../core/services/atencion.service';
import { RecetaElectronicaService, RecetaElectronicaDto } from '../../core/services/receta-electronica.service';
import { SesaQrComponent } from '../../shared/components/sesa-qr/sesa-qr.component';
import { ConsultaMedicaDto } from '../../core/services/cita.service';
import { AuthService } from '../../core/services/auth.service';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';

@Component({
  selector: 'sesa-receta-electronica-modal',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule, SesaQrComponent],
  template: `
    <div class="rem-backdrop" (click)="cerrar()" role="presentation">
      <div class="rem-modal" (click)="$event.stopPropagation()" role="dialog" aria-modal="true">
        <div class="rem-header">
          <h2 class="rem-title">Receta electrónica con QR verificable</h2>
          <button type="button" class="rem-close" (click)="cerrar()" aria-label="Cerrar">×</button>
        </div>

        @if (!recetaGenerada()) {
          <div class="rem-body">
            <p class="rem-desc">Los medicamentos se incluirán en la receta firmada. El paciente o la farmacia podrán verificar su autenticidad escaneando el QR.</p>
            <div class="rem-row rem-row--readonly">
              <span class="rem-label">Paciente</span>
              <span class="rem-val">{{ cita()?.pacienteNombreCompleto }}</span>
            </div>
            <div class="rem-row rem-row--readonly">
              <span class="rem-label">Médico</span>
              <span class="rem-val">{{ medicoNombre() }}</span>
            </div>

            <div class="rem-section">
              <h3 class="rem-section-title">Medicamentos</h3>
              @for (item of medicamentos(); track $index) {
                <div class="rem-med">
                  <input type="text" [(ngModel)]="item.medicamento" placeholder="Medicamento" class="rem-input" />
                  <input type="text" [(ngModel)]="item.dosis" placeholder="Dosis" class="rem-input rem-input--short" />
                  <input type="text" [(ngModel)]="item.frecuencia" placeholder="Frecuencia" class="rem-input rem-input--short" />
                  <input type="text" [(ngModel)]="item.duracion" placeholder="Duración" class="rem-input rem-input--short" />
                  <button type="button" class="rem-btn-remove" (click)="quitarMed($index)">Eliminar</button>
                </div>
              }
              <button type="button" class="rem-btn-add" (click)="agregarMed()">+ Añadir medicamento</button>
            </div>

            <div class="rem-row">
              <label class="rem-label" for="rem-obs">Observaciones (opcional)</label>
              <textarea id="rem-obs" [(ngModel)]="observaciones" placeholder="Indicaciones adicionales" class="rem-textarea" rows="2"></textarea>
            </div>
          </div>
          <div class="rem-footer">
            <button type="button" class="rem-btn rem-btn--ghost" (click)="cerrar()">Cancelar</button>
            <button type="button" class="rem-btn rem-btn--primary" (click)="generar()" [disabled]="guardando() || medicamentos().length === 0 || !medicamentos()[0].medicamento">
              {{ guardando() ? 'Generando…' : 'Generar receta con QR' }}
            </button>
          </div>
        } @else {
          <div class="rem-body rem-body--result" #resultSection>
            <div class="rem-result-header">
              <span class="rem-result-badge">Receta emitida</span>
              <p class="rem-result-date">{{ recetaGenerada()?.fechaEmision }}</p>
            </div>
            <div class="rem-result-grid">
              <div class="rem-result-qr">
                <sesa-qr [value]="recetaGenerada()?.urlVerificacion ?? ''" [size]="180" alt="QR verificación" />
                <p class="rem-result-qr-label">Escanear para verificar autenticidad</p>
              </div>
              <div class="rem-result-data">
                <div class="rem-row"><span class="rem-label">Paciente</span><span class="rem-val">{{ recetaGenerada()?.pacienteNombre }}</span></div>
                <div class="rem-row"><span class="rem-label">Médico</span><span class="rem-val">{{ recetaGenerada()?.medicoNombre }}</span></div>
                <ul class="rem-result-list">
                  @for (m of recetaGenerada()?.medicamentos ?? []; track m.medicamento) {
                    <li><strong>{{ m.medicamento }}</strong> {{ m.dosis }} {{ m.frecuencia }} {{ m.duracion }}</li>
                  }
                </ul>
              </div>
            </div>
            <div class="rem-result-actions">
              <button type="button" class="rem-btn rem-btn--outline" (click)="copiarEnlace()">Copiar enlace de verificación</button>
              <button type="button" class="rem-btn rem-btn--primary" (click)="imprimir()">Imprimir receta</button>
            </div>
          </div>
          <div class="rem-footer">
            <button type="button" class="rem-btn rem-btn--primary" (click)="cerrar()">Cerrar</button>
          </div>
        }
      </div>
    </div>
  `,
  styleUrl: './receta-electronica-modal.component.scss',
})
export class RecetaElectronicaModalComponent {
  cita = input<ConsultaMedicaDto | null>(null);
  atencionId = input<number | null>(null);
  closed = output<void>();

  @ViewChild('resultSection') resultSection?: ElementRef<HTMLElement>;

  private readonly recetaService = inject(RecetaElectronicaService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(SesaToastService);

  readonly medicamentos = signal<Partial<FormulaMedicaDto>[]>([{ medicamento: '', dosis: '', frecuencia: '', duracion: '' }]);
  readonly guardando = signal(false);
  readonly recetaGenerada = signal<RecetaElectronicaDto | null>(null);
  observaciones = '';

  readonly medicoNombre = () => this.auth.currentUser()?.nombreCompleto ?? 'Médico';

  agregarMed(): void {
    this.medicamentos.update(m => [...m, { medicamento: '', dosis: '', frecuencia: '', duracion: '' }]);
  }

  quitarMed(i: number): void {
    this.medicamentos.update(m => m.filter((_, idx) => idx !== i));
  }

  generar(): void {
    const c = this.cita();
    const meds = this.medicamentos().filter(m => m.medicamento?.trim());
    if (!meds.length || !c) return;

    const aid = this.atencionId();
    this.guardando.set(true);
    if (aid != null) {
      this.recetaService.crear(aid, this.observaciones || undefined).subscribe({
        next: (rec) => {
          rec.urlVerificacion = this.recetaService.getUrlVerificacion(rec.tokenVerificacion);
          this.recetaGenerada.set(rec);
          this.guardando.set(false);
        },
        error: () => {
          this.guardando.set(false);
          this.toast.error('No se pudo generar la receta. Verifique que el backend exponga POST /api/recetas.');
        },
      });
      return;
    }
    this.recetaService.crearConFormulas(
      c.pacienteId,
      undefined,
      meds as FormulaMedicaDto[],
      this.medicoNombre(),
      c.pacienteNombreCompleto,
      undefined,
      this.observaciones || undefined
    ).subscribe({
      next: (rec) => {
        rec.urlVerificacion = this.recetaService.getUrlVerificacion(rec.tokenVerificacion);
        this.recetaGenerada.set(rec);
        this.guardando.set(false);
      },
      error: () => {
        this.guardando.set(false);
        this.toast.error('No se pudo generar la receta. Implemente POST /api/recetas/crear-con-formulas en el backend.');
      },
    });
  }

  copiarEnlace(): void {
    const url = this.recetaGenerada()?.urlVerificacion;
    if (url && navigator.clipboard) navigator.clipboard.writeText(url);
  }

  imprimir(): void {
    if (this.resultSection) window.print();
  }

  cerrar(): void {
    this.closed.emit();
  }
}
