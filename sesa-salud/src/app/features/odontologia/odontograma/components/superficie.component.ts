/**
 * Una superficie dental (polígono SVG) con click, doble click y menú contextual.
 * Autor: Ing. J Sebastian Vargas S
 */

import {
  Component,
  input,
  output,
  ChangeDetectionStrategy,
} from '@angular/core';
import type { Superficie } from '../../odontologia.models';

@Component({
  selector: 'g[app-superficie]',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <polygon
      class="superficie-poly"
      [class.active]="active()"
      [attr.points]="points()"
      [attr.fill]="fill()"
      (click)="onClick($event)"
      (dblclick)="onDblClick($event)"
      (contextmenu)="onContextMenu($event)"
    />
  `,
  styles: [`
    /* [Superficie odontograma] */
    /* Autor: Ing. J Sebastian Vargas S */
    :host ::ng-deep .superficie-poly {
      stroke: var(--sesa-border);
      stroke-width: 0.7;
      stroke-linejoin: round;
      cursor: pointer;
      transition: stroke 0.12s, stroke-width 0.12s, fill-opacity 0.12s;
    }
    :host ::ng-deep .superficie-poly:hover {
      fill-opacity: 0.85;
      stroke: var(--sesa-secondary);
      stroke-width: 1.3;
    }
    :host ::ng-deep .superficie-poly.active {
      stroke: var(--sesa-secondary);
      stroke-width: 2;
    }
  `],
})
export class SuperficieComponent {
  readonly points = input.required<string>();
  readonly fill = input.required<string>();
  readonly active = input(false);
  readonly superficie = input.required<Superficie>();

  readonly superficieClick = output<Superficie>();
  readonly superficieDblClick = output<Superficie>();
  readonly superficieContextMenu = output<{ event: MouseEvent; superficie: Superficie }>();

  onClick(e: MouseEvent): void {
    e.stopPropagation();
    this.superficieClick.emit(this.superficie());
  }

  onDblClick(e: MouseEvent): void {
    e.stopPropagation();
    this.superficieDblClick.emit(this.superficie());
  }

  onContextMenu(e: MouseEvent): void {
    e.preventDefault();
    e.stopPropagation();
    this.superficieContextMenu.emit({ event: e, superficie: this.superficie() });
  }
}
