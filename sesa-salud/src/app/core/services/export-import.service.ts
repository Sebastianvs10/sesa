/**
 * Servicio global de exportación/importación de datos (Excel)
 * Autor: Ing. J Sebastian Vargas S
 */
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type ExportTipo = 'pacientes' | 'facturas';
export type ImportTipo = 'pacientes' | 'facturas';

export interface ImportResult {
  importados: number;
  omitidos: number;
  mensaje: string;
  errores: string[];
}

@Injectable({ providedIn: 'root' })
export class ExportImportService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/data-exchange`;

  /* ── Export ──────────────────────────────────────────────────────── */

  /** Exporta un listado de entidades como Excel (.xlsx). */
  exportar(tipo: ExportTipo, params: Record<string, string> = {}): Observable<Blob> {
    let httpParams = new HttpParams();
    for (const [k, v] of Object.entries(params)) {
      if (v) httpParams = httpParams.set(k, v);
    }
    return this.http.get(`${this.baseUrl}/export/${tipo}`, {
      params: httpParams,
      responseType: 'blob',
    });
  }

  /** Descarga la plantilla Excel vacía para importar. */
  descargarPlantilla(tipo: ExportTipo): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/template/${tipo}`, {
      responseType: 'blob',
    });
  }

  /* ── Import ──────────────────────────────────────────────────────── */

  /** Importa registros desde un archivo Excel. */
  importar(tipo: ImportTipo, file: File): Observable<ImportResult> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ImportResult>(`${this.baseUrl}/import/${tipo}`, form);
  }

  /* ── Trigger download ────────────────────────────────────────────── */

  /** Lanza la descarga del blob en el navegador. */
  triggerDownload(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  }
}
