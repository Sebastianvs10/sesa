/**
 * Selector de vista del odontograma (frontal, oclusal, lateral, arcadas).
 * Autor: Ing. J Sebastian Vargas S
 */

import { Component, inject, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OdontogramaService, TipoVista, VISTAS_LABEL } from '../services/odontograma.service';

@Component({
  selector: 'app-vista-selector',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="vista-selector">
      @for (v of vistas; track v) {
        <button type="button"
          class="vista-btn"
          [class.active]="svc.vista() === v"
          (click)="seleccionar(v)"
          [title]="VISTAS_LABEL[v]">
          {{ VISTAS_LABEL[v] }}
        </button>
      }
    </div>
  `,
  styles: [`
    /* [Vista selector odontograma] */
    /* Autor: Ing. J Sebastian Vargas S */
    .vista-selector {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
    }
    .vista-btn {
      padding: 8px 14px;
      border: 1px solid var(--sesa-border);
      background: var(--sesa-bg);
      color: var(--sesa-text-secondary);
      border-radius: var(--sesa-radius-sm);
      font-size: 0.8rem;
      font-weight: 500;
      cursor: pointer;
      transition: background var(--sesa-transition), border-color var(--sesa-transition), color var(--sesa-transition);
    }
    .vista-btn:hover {
      background: var(--sesa-surface-hover);
      border-color: var(--sesa-secondary);
      color: var(--sesa-text-primary);
    }
    .vista-btn.active {
      background: var(--sesa-secondary);
      border-color: var(--sesa-secondary);
      color: #fff;
    }
  `],
})
export class VistaSelectorComponent {
  readonly svc = inject(OdontogramaService);
  readonly vistaChange = output<TipoVista>();

  readonly VISTAS_LABEL = VISTAS_LABEL;
  readonly vistas: TipoVista[] = [
    'frontal',
    'oclusal',
    'lateral_derecha',
    'lateral_izquierda',
    'arcada_superior',
    'arcada_inferior',
  ];

  seleccionar(v: TipoVista): void {
    this.svc.setVista(v);
    this.vistaChange.emit(v);
  }
}
