/**
 * Tarjeta reutilizable premium — variantes, estado de carga, slots de header/footer.
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { SesaSkeletonComponent } from '../sesa-skeleton/sesa-skeleton.component';

export type CardVariant = 'default' | 'elevated' | 'outlined' | 'flat';

@Component({
  standalone: true,
  selector: 'sesa-card',
  imports: [CommonModule, SesaSkeletonComponent],
  templateUrl: './sesa-card.component.html',
  styleUrl: './sesa-card.component.scss',
})
export class SesaCardComponent {
  @Input() title = '';
  @Input() subtitle = '';
  @Input() alignHeader: 'start' | 'between' = 'between';
  /** Variante visual de la tarjeta */
  @Input() variant: CardVariant = 'default';
  /** Muestra skeleton de carga en lugar del contenido */
  @Input() loading = false;
  /** Número de líneas skeleton a mostrar cuando loading=true */
  @Input() skeletonLines = 3;

  get skeletonLinesArray(): number[] {
    return Array.from({ length: this.skeletonLines }, (_, i) => i);
  }
}
