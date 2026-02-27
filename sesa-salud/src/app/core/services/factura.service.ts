import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface FacturaDto {
  id: number;
  numeroFactura?: string;
  pacienteId: number;
  pacienteNombre: string;
  pacienteDocumento?: string;
  pacienteTipoDocumento?: string;
  epsNombre?: string;
  epsCodigo?: string;
  ordenId?: number;
  valorTotal: number;
  estado?: string;
  descripcion?: string;
  fechaFactura?: string;
  createdAt?: string;
  // Campos normativos Decreto 4747/2007 + RIPS
  codigoCups?: string;
  descripcionCups?: string;
  tipoServicio?: string;
  responsablePago?: string;
  cuotaModeradora?: number;
  numeroAutorizacionEps?: string;
}

export interface FacturaRequestDto {
  numeroFactura?: string;
  pacienteId: number;
  ordenId?: number;
  valorTotal: number;
  estado?: string;
  descripcion?: string;
  fechaFactura?: string;
  // Campos normativos Decreto 4747/2007 + RIPS
  codigoCups?: string;
  descripcionCups?: string;
  tipoServicio?: string;
  responsablePago?: string;
  cuotaModeradora?: number;
  numeroAutorizacionEps?: string;
}

export interface ResumenFacturacion {
  totalFacturadoMes: number;
  cantidadMes: number;
  montoPendiente: number;
  cantidadPendiente: number;
  montoPagado: number;
  cantidadPagada: number;
  cantidadAnulada: number;
  cantidadRechazada: number;
}

export interface PageResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface FacturaFiltros {
  estado?: string;
  desde?: string;
  hasta?: string;
  pacienteId?: number;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class FacturaService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/facturas`;

  listAll(filtros: FacturaFiltros = {}): Observable<PageResult<FacturaDto>> {
    let params = new HttpParams()
      .set('page', filtros.page ?? 0)
      .set('size', filtros.size ?? 20);
    if (filtros.estado) params = params.set('estado', filtros.estado);
    if (filtros.desde) params = params.set('desde', filtros.desde);
    if (filtros.hasta) params = params.set('hasta', filtros.hasta);
    if (filtros.pacienteId) params = params.set('pacienteId', filtros.pacienteId);
    return this.http.get<PageResult<FacturaDto>>(this.apiUrl, { params });
  }

  listByPaciente(pacienteId: number, page = 0, size = 20): Observable<FacturaDto[]> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<FacturaDto[]>(`${this.apiUrl}/paciente/${pacienteId}`, { params });
  }

  resumen(): Observable<ResumenFacturacion> {
    return this.http.get<ResumenFacturacion>(`${this.apiUrl}/resumen`);
  }

  getById(id: number): Observable<FacturaDto> {
    return this.http.get<FacturaDto>(`${this.apiUrl}/${id}`);
  }

  create(request: FacturaRequestDto): Observable<FacturaDto> {
    return this.http.post<FacturaDto>(this.apiUrl, request);
  }

  update(id: number, request: FacturaRequestDto): Observable<FacturaDto> {
    return this.http.put<FacturaDto>(`${this.apiUrl}/${id}`, request);
  }

  cambiarEstado(id: number, estado: string): Observable<FacturaDto> {
    return this.http.patch<FacturaDto>(`${this.apiUrl}/${id}/estado`, { estado });
  }

  /** Descarga el archivo RIPS CSV directamente al navegador. */
  descargarRips(desde: string, hasta: string): void {
    const params = new HttpParams().set('desde', desde).set('hasta', hasta);
    const url = `${this.apiUrl}/rips?${params.toString()}`;
    const a = document.createElement('a');
    a.href = url;
    a.download = `RIPS_${desde}_${hasta}.csv`;
    a.click();
  }

  /** Exporta RIPS como texto (para preview). */
  exportRips(desde: string, hasta: string): Observable<string> {
    const params = new HttpParams().set('desde', desde).set('hasta', hasta);
    return this.http.get(`${this.apiUrl}/rips`, { params, responseType: 'text' }) as Observable<string>;
  }

  /** Exporta RIPS estructurado Res. 3374/2000 (archivos CT, US, AP, AC). */
  exportRipsEstructurado(desde: string, hasta: string): Observable<Record<string, string>> {
    const params = new HttpParams().set('desde', desde).set('hasta', hasta);
    return this.http.get<Record<string, string>>(`${this.apiUrl}/rips/estructurado`, { params });
  }

  /** Descarga un archivo RIPS estructurado específico (CT, US, AP o AC). */
  descargarArchivoRips(contenido: string, nombreArchivo: string): void {
    const blob = new Blob([contenido], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = nombreArchivo;
    a.click();
    URL.revokeObjectURL(url);
  }
}
