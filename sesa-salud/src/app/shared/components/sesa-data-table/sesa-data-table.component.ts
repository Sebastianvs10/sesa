/**
 * Tabla de datos premium — skeleton, empty state, acciones por fila, hover.
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule } from '@angular/common';
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { SesaSkeletonComponent } from '../sesa-skeleton/sesa-skeleton.component';

export interface SesaTableColumn {
  key: string;
  label: string;
  width?: string;
  /** Si true, la celda puede contener HTML (usar con precaución) */
  html?: boolean;
}

@Component({
  standalone: true,
  selector: 'sesa-data-table',
  imports: [CommonModule, SesaSkeletonComponent],
  templateUrl: './sesa-data-table.component.html',
  styleUrl: './sesa-data-table.component.scss',
})
export class SesaDataTableComponent {
  @Input() columns: SesaTableColumn[] = [];
  @Input() data: Record<string, unknown>[] = [];
  @Input() emptyMessage = 'Sin registros';
  @Input() emptyIcon = '';
  /** Muestra filas skeleton durante la carga */
  @Input() loading = false;
  /** Número de filas skeleton a mostrar */
  @Input() skeletonRows = 5;
  /** Si true, muestra columna de acciones al final */
  @Input() hasActions = false;

  get skeletonRowsArray(): number[] {
    return Array.from({ length: this.skeletonRows }, (_, i) => i);
  }

  getCellValue(row: Record<string, unknown>, key: string): string {
    const val = row[key];
    return val !== null && val !== undefined ? String(val) : '';
  }
}
