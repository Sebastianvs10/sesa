import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

export interface SesaTableColumn {
  key: string;
  label: string;
  width?: string;
}

@Component({
  standalone: true,
  selector: 'sesa-data-table',
  imports: [CommonModule],
  templateUrl: './sesa-data-table.component.html',
  styleUrl: './sesa-data-table.component.scss',
})
export class SesaDataTableComponent {
  @Input() columns: SesaTableColumn[] = [];
  @Input() data: Record<string, unknown>[] = [];
  @Input() emptyMessage = 'Sin registros';
}

