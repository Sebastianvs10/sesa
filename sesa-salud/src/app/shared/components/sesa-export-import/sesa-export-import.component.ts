/**
 * Componente global reutilizable de Exportar/Importar datos (Excel)
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  ExportImportService,
  ExportTipo,
  ImportResult,
} from '../../../core/services/export-import.service';
import { SesaToastService } from '../sesa-toast/sesa-toast.component';

export interface ExportConfig {
  /** Entidad a exportar/importar: 'pacientes' | 'facturas' */
  tipo: ExportTipo;
  /** Etiqueta visible al usuario */
  label: string;
  /** Parámetros adicionales para el export (ej: { desde, hasta }) */
  params?: Record<string, string>;
  /** Si permite filtro de fechas */
  conFiltroFechas?: boolean;
}

@Component({
  standalone: true,
  selector: 'sesa-export-import',
  imports: [CommonModule, FormsModule],
  templateUrl: './sesa-export-import.component.html',
  styleUrl: './sesa-export-import.component.scss',
})
export class SesaExportImportComponent implements OnInit {
  private readonly svc = inject(ExportImportService);
  private readonly toast = inject(SesaToastService);

  @Input() configs: ExportConfig[] = [];
  /** Emite cuando se completa una importación exitosa */
  @Output() importCompleted = new EventEmitter<ImportResult>();

  selectedConfig: ExportConfig | null = null;
  activeTab: 'export' | 'import' = 'export';

  /* Filtros de fecha para export */
  exportDesde = '';
  exportHasta = '';

  /* Import */
  selectedFile: File | null = null;
  selectedImportConfig: ExportConfig | null = null;
  dragOver = signal(false);

  /* Estado */
  exporting = signal(false);
  importing = signal(false);
  importResult = signal<ImportResult | null>(null);

  ngOnInit(): void {
    if (this.configs.length > 0) {
      this.selectedConfig = this.configs[0];
      this.selectedImportConfig = this.configs[0];
    }
  }

  /* ── Export ────────────────────────────────────────────────── */

  exportar(): void {
    if (!this.selectedConfig) return;
    this.exporting.set(true);

    const params: Record<string, string> = { ...(this.selectedConfig.params ?? {}) };
    if (this.selectedConfig.conFiltroFechas) {
      if (this.exportDesde) params['desde'] = this.exportDesde;
      if (this.exportHasta) params['hasta'] = this.exportHasta;
    }

    this.svc.exportar(this.selectedConfig.tipo, params).subscribe({
      next: (blob) => {
        const fecha = new Date().toISOString().slice(0, 10);
        this.svc.triggerDownload(blob, `${this.selectedConfig!.tipo}_${fecha}.xlsx`);
        this.toast.success(`${this.selectedConfig!.label} exportado correctamente.`, 'Exportación Excel');
        this.exporting.set(false);
      },
      error: (err: unknown) => {
        const msg = (err as { error?: { error?: string } })?.error?.error ?? 'Error al exportar';
        this.toast.error(msg, 'Error');
        this.exporting.set(false);
      },
    });
  }

  descargarPlantilla(): void {
    if (!this.selectedImportConfig) return;
    this.svc.descargarPlantilla(this.selectedImportConfig.tipo).subscribe({
      next: (blob) => {
        this.svc.triggerDownload(blob, `plantilla_${this.selectedImportConfig!.tipo}.xlsx`);
        this.toast.success('Plantilla descargada.', 'Plantilla');
      },
      error: () => this.toast.error('No se pudo descargar la plantilla.', 'Error'),
    });
  }

  /* ── Import ────────────────────────────────────────────────── */

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
    this.importResult.set(null);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(true);
  }

  onDragLeave(): void {
    this.dragOver.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(false);
    const file = event.dataTransfer?.files?.[0];
    if (file && (file.name.endsWith('.xlsx') || file.name.endsWith('.xls'))) {
      this.selectedFile = file;
      this.importResult.set(null);
    } else {
      this.toast.error('Solo se aceptan archivos .xlsx o .xls', 'Formato no válido');
    }
  }

  importar(): void {
    if (!this.selectedFile || !this.selectedImportConfig) return;
    this.importing.set(true);
    this.importResult.set(null);

    this.svc.importar(this.selectedImportConfig.tipo, this.selectedFile).subscribe({
      next: (result) => {
        this.importResult.set(result);
        this.importing.set(false);
        if (result.importados > 0) {
          this.toast.success(result.mensaje, 'Importación completada');
          this.importCompleted.emit(result);
        } else {
          this.toast.error(result.mensaje || 'No se importaron registros.', 'Sin datos');
        }
        this.selectedFile = null;
      },
      error: (err: unknown) => {
        const msg = (err as { error?: { error?: string } })?.error?.error ?? 'Error al importar';
        this.toast.error(msg, 'Error de importación');
        this.importing.set(false);
      },
    });
  }

  removeFile(): void {
    this.selectedFile = null;
    this.importResult.set(null);
  }
}
