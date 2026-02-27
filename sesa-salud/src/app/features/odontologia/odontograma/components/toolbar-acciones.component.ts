/**
 * Toolbar de acciones de tratamiento (Caries, Restauración, Corona, etc.).
 * Al seleccionar una acción, el click en superficie aplica ese tratamiento.
 * Autor: Ing. J Sebastian Vargas S
 */

import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OdontogramaService, ACCIONES_TOOLBAR } from '../services/odontograma.service';
import { ESTADO_COLOR, ESTADO_LABEL } from '../../odontologia.models';

@Component({
  selector: 'app-toolbar-acciones',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toolbar-acciones">
      <span class="toolbar-label">Aplicar:</span>
      @for (acc of acciones; track acc.estado) {
        <button type="button"
          class="toolbar-btn"
          [class.active]="svc.tratamientoSeleccionado() === acc.estado"
          [style.--btn-color]="ESTADO_COLOR[acc.estado]"
          (click)="toggle(acc.estado)"
          [title]="acc.label + ' — Click en superficie para aplicar'">
          <span class="toolbar-btn-dot" [style.background]="ESTADO_COLOR[acc.estado] === 'transparent' ? 'var(--sesa-border)' : ESTADO_COLOR[acc.estado]"></span>
          {{ acc.label }}
        </button>
      }
      @if (svc.tieneTratamientoSeleccionado()) {
        <button type="button" class="toolbar-btn toolbar-clear" (click)="svc.setTratamientoSeleccionado(null)">
          Deseleccionar
        </button>
      }
    </div>
  `,
  styles: [`
    /* [Toolbar acciones odontograma] */
    /* Autor: Ing. J Sebastian Vargas S */
    .toolbar-acciones {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;
    }
    .toolbar-label {
      font-size: 0.8rem;
      font-weight: 600;
      color: var(--sesa-text-secondary);
      margin-right: 4px;
    }
    .toolbar-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 6px 12px;
      border: 1px solid var(--sesa-border);
      background: var(--sesa-bg);
      color: var(--sesa-text-primary);
      border-radius: var(--sesa-radius-sm);
      font-size: 0.78rem;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.15s ease;
    }
    .toolbar-btn:hover {
      background: var(--sesa-surface-hover);
      border-color: var(--sesa-border);
    }
    .toolbar-btn.active {
      border-color: var(--btn-color, var(--sesa-secondary));
      background: color-mix(in srgb, var(--btn-color, var(--sesa-secondary)) 15%, transparent);
      color: var(--sesa-text-primary);
    }
    .toolbar-btn-dot {
      width: 10px;
      height: 10px;
      border-radius: 50%;
      flex-shrink: 0;
    }
    .toolbar-clear {
      margin-left: 8px;
      border-style: dashed;
    }
  `],
})
export class ToolbarAccionesComponent {
  readonly svc = inject(OdontogramaService);
  readonly acciones = ACCIONES_TOOLBAR;
  readonly ESTADO_COLOR = ESTADO_COLOR;
  readonly ESTADO_LABEL = ESTADO_LABEL;

  toggle(estado: typeof ACCIONES_TOOLBAR[number]['estado']): void {
    const current = this.svc.tratamientoSeleccionado();
    this.svc.setTratamientoSeleccionado(current === estado ? null : estado);
  }
}
