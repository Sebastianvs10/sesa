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

  /** Descarga el PDF de órdenes clínicas y resultados del paciente. */
  descargarOrdenesPaciente(pacienteId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/pdf/ordenes/paciente/${pacienteId}`, {
      responseType: 'blob',
    });
  }

  /** Descarga el PDF de una sola orden (con datos del paciente y resultado). */
  descargarOrdenIndividual(ordenId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/pdf/orden/${ordenId}`, {
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

  /**
   * Abre el PDF en una nueva ventana y dispara el diálogo de impresión.
   * Incluye datos del paciente y resultados en el mismo documento.
   */
  openForPrint(blob: Blob): void {
    const url = URL.createObjectURL(blob);
    const w = window.open(url, '_blank', 'noopener,noreferrer');
    if (w) {
      w.onload = () => {
        w.print();
        w.onafterprint = () => {
          w.close();
          URL.revokeObjectURL(url);
        };
      };
    } else {
      URL.revokeObjectURL(url);
    }
  }
}
