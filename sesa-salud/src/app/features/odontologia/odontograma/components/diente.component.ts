/**
 * Componente de una pieza dental (grupo SVG) con 5 superficies interactivas.
 * Autor: Ing. J Sebastian Vargas S
 */

import {
  Component,
  input,
  output,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import type { Superficie } from '../../odontologia.models';
import { SuperficieComponent } from './superficie.component';

export interface PiezaUI {
  fdi: number;
  x: number;
  y: number;
  label: string;
  esSuperior: boolean;
  esAnterior: boolean;
  hw: number;
  hh: number;
  rx: number;
}

export interface DienteViewModel {
  pieza: PiezaUI;
  points: Record<string, string>;
  colores: Record<string, string>;
  ausente: boolean;
  popupFdi: number | null;
  popupSuperficie: Superficie | null;
}

const SUPERFICIES: Superficie[] = ['VESTIBULAR', 'LINGUAL', 'MESIAL', 'DISTAL', 'OCLUSAL'];

@Component({
  selector: 'g[app-diente]',
  standalone: true,
  imports: [CommonModule, SuperficieComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (vm(); as d) {
      <!-- Badge FDI -->
      <rect
        [attr.x]="-14" [attr.width]="28"
        [attr.y]="d.pieza.esSuperior ? (-d.pieza.hh - 22) : (d.pieza.hh + 8)"
        height="14" rx="4" class="numero-badge"/>
      <text
        class="pieza-numero"
        x="0"
        [attr.y]="d.pieza.esSuperior ? (-d.pieza.hh - 12) : (d.pieza.hh + 18)"
      >{{ d.pieza.label }}</text>

      @if (d.ausente) {
        <rect [attr.x]="-d.pieza.hw" [attr.y]="-d.pieza.hh"
              [attr.width]="d.pieza.hw * 2" [attr.height]="d.pieza.hh * 2"
              [attr.rx]="d.pieza.rx" class="ausente-bg"/>
        <line [attr.x1]="-d.pieza.hw+4" [attr.y1]="-d.pieza.hh+4"
              [attr.x2]="d.pieza.hw-4" [attr.y2]="d.pieza.hh-4" class="ausente-x"/>
        <line [attr.x1]="d.pieza.hw-4" [attr.y1]="-d.pieza.hh+4"
              [attr.x2]="-d.pieza.hw+4" [attr.y2]="d.pieza.hh-4" class="ausente-x"/>
      } @else {
        <rect [attr.x]="-d.pieza.hw" [attr.y]="-d.pieza.hh"
              [attr.width]="d.pieza.hw * 2" [attr.height]="d.pieza.hh * 2"
              [attr.rx]="d.pieza.rx" class="tooth-base"/>
        @for (sup of superficies; track sup) {
          <g app-superficie
            [points]="d.points[sup]"
            [fill]="d.colores[sup] ?? 'transparent'"
            [active]="d.popupFdi === d.pieza.fdi && d.popupSuperficie === sup"
            [superficie]="sup"
            (superficieClick)="superficieClick.emit(sup)"
            (superficieDblClick)="superficieDblClick.emit(sup)"
            (superficieContextMenu)="superficieContextMenu.emit($event)"
          />
        }
        <rect [attr.x]="-d.pieza.hw" [attr.y]="-d.pieza.hh"
              [attr.width]="d.pieza.hw * 2" [attr.height]="d.pieza.hh * 2"
              [attr.rx]="d.pieza.rx" class="pieza-borde" style="pointer-events:none"/>
      }
    }
  `,
  styles: [`
    /* [Diente odontograma] */
    /* Autor: Ing. J Sebastian Vargas S */
    :host {
      cursor: pointer;
      transition: filter 0.18s ease;
    }
    :host:hover:not(.pieza-ausente) ::ng-deep .superficie-poly {
      stroke: var(--sesa-secondary);
    }
    :host ::ng-deep .numero-badge {
      fill: var(--sesa-surface);
      stroke: var(--sesa-border);
      stroke-width: 0.8;
    }
    :host ::ng-deep .pieza-numero {
      font-size: 8.5px;
      fill: var(--sesa-text-secondary);
      text-anchor: middle;
      font-weight: 700;
      pointer-events: none;
    }
    :host ::ng-deep .tooth-base { fill: var(--sesa-surface); stroke: none; pointer-events: none; }
    :host ::ng-deep .pieza-borde {
      fill: none;
      stroke: var(--sesa-border);
      stroke-width: 1.5;
      opacity: 0.7;
    }
    :host ::ng-deep .ausente-bg {
      fill: var(--sesa-bg);
      stroke: var(--sesa-border);
      stroke-width: 1;
      stroke-dasharray: 3,2;
    }
    :host ::ng-deep .ausente-x {
      stroke: var(--sesa-error);
      stroke-width: 2;
      stroke-linecap: round;
      pointer-events: none;
    }
  `],
})
export class DienteComponent {
  readonly vm = input.required<DienteViewModel | null>();
  readonly superficieClick = output<Superficie>();
  readonly superficieDblClick = output<Superficie>();
  readonly superficieContextMenu = output<{ event: MouseEvent; superficie: Superficie }>();

  readonly superficies = SUPERFICIES;
}
