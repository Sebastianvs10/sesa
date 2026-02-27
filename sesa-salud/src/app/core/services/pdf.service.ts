/**
 * Servicio global para descarga de documentos PDF
 * Autor: Ing. J Sebastian Vargas S
 */
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PdfService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  /** Descarga el PDF de historia clínica completa por ID de historia. */
  descargarHistoriaClinica(historiaId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/pdf/historia/${historiaId}`, {
      responseType: 'blob',
    });
  }

  /** Descarga el PDF de historia clínica buscando por ID de paciente. */
  descargarHistoriaClinicaPorPaciente(pacienteId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/pdf/historia/paciente/${pacienteId}`, {
      responseType: 'blob',
    });
  }

  /**
   * Dispara la descarga del blob en el navegador.
   * @param blob    Blob recibido del servidor
   * @param filename Nombre del archivo con extensión
   */
  triggerDownload(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  }
}
