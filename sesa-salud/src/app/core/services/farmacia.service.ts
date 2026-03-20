import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface FarmaciaMedicamentoDto {
  id: number;
  nombre: string;
  lote?: string;
  codigoBarras?: string;
  fechaVencimiento?: string;
  cantidad: number;
  precio?: number;
  stockMinimo?: number;
  activo: boolean;
}

export interface FarmaciaMedicamentoRequestDto {
  nombre: string;
  lote?: string;
  codigoBarras?: string;
  fechaVencimiento?: string;
  cantidad?: number;
  precio?: number;
  stockMinimo?: number;
  activo?: boolean;
}

/** Respuesta paginada Spring Data */
export interface PageFarmaciaMedicamentos {
  content: FarmaciaMedicamentoDto[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface FarmaciaIndicadoresDto {
  totalSkusActivos: number;
  stockBajo: number;
  proximosAVencer30Dias: number;
}

export interface FarmaciaDispensacionDto {
  id: number;
  medicamentoId: number;
  medicamentoNombre: string;
  pacienteId: number;
  pacienteNombre: string;
  cantidad: number;
  fechaDispensacion?: string;
  entregadoPor?: string;
}

export interface FarmaciaDispensacionRequestDto {
  medicamentoId: number;
  pacienteId: number;
  cantidad: number;
  entregadoPor?: string;
}

export interface OrdenFarmaciaPendienteDto {
  id: number;
  pacienteId: number;
  pacienteNombre: string;
  pacienteDocumento?: string;
  tipoDocumentoPaciente?: string;
  alergiasPaciente?: string;
  detalle?: string;
  cantidadPrescrita?: number;
  unidadMedida?: string;
  frecuencia?: string;
  duracionDias?: number;
  fechaOrden?: string;
  medicoNombre?: string;
  estadoDispensacionFarmacia: string;
  items?: OrdenFarmaciaPendienteItemDto[];
}

export interface OrdenFarmaciaPendienteItemDto {
  id?: number;
  detalle?: string;
  cantidadPrescrita?: number;
  unidadMedida?: string;
  frecuencia?: string;
  duracionDias?: number;
}

export interface LineaDispensacionDto {
  medicamentoId: number;
  lote?: string;
  cantidad: number;
}

export interface DispensarOrdenRequestDto {
  ordenId: number;
  lineas: LineaDispensacionDto[];
}

export interface ListMedicamentosParams {
  page?: number;
  size?: number;
  q?: string;
  soloStock?: boolean;
}

/** Órdenes farmacia pendientes — respuesta paginada Spring Data */
export interface PageOrdenesFarmaciaPendientes {
  content: OrdenFarmaciaPendienteDto[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface ListOrdenesPendientesParams {
  page?: number;
  size?: number;
  q?: string;
}

@Injectable({ providedIn: 'root' })
export class FarmaciaService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/farmacia`;

  listMedicamentos(params: ListMedicamentosParams = {}): Observable<PageFarmaciaMedicamentos> {
    let p = new HttpParams()
      .set('page', String(params.page ?? 0))
      .set('size', String(params.size ?? 25));
    if (params.q?.trim()) p = p.set('q', params.q.trim());
    if (params.soloStock === true) p = p.set('soloStock', 'true');
    return this.http.get<PageFarmaciaMedicamentos>(`${this.apiUrl}/medicamentos`, { params: p });
  }

  indicadoresInventario(): Observable<FarmaciaIndicadoresDto> {
    return this.http.get<FarmaciaIndicadoresDto>(`${this.apiUrl}/medicamentos/indicadores`);
  }

  /** Búsqueda exacta por código leído del escáner */
  medicamentoPorCodigo(codigo: string): Observable<FarmaciaMedicamentoDto | null> {
    const p = new HttpParams().set('codigo', codigo.trim());
    return this.http
      .get<FarmaciaMedicamentoDto>(`${this.apiUrl}/medicamentos/por-codigo`, { params: p })
      .pipe(
        catchError((err) => {
          if (err?.status === 404) return of(null);
          throw err;
        })
      );
  }

  createMedicamento(request: FarmaciaMedicamentoRequestDto): Observable<FarmaciaMedicamentoDto> {
    return this.http.post<FarmaciaMedicamentoDto>(`${this.apiUrl}/medicamentos`, request);
  }

  dispensar(request: FarmaciaDispensacionRequestDto): Observable<FarmaciaDispensacionDto> {
    return this.http.post<FarmaciaDispensacionDto>(`${this.apiUrl}/dispensar`, request);
  }

  listDispensacionesByPaciente(pacienteId: number): Observable<FarmaciaDispensacionDto[]> {
    const params = new HttpParams().set('page', '0').set('size', '50');
    return this.http.get<FarmaciaDispensacionDto[]>(`${this.apiUrl}/dispensaciones/paciente/${pacienteId}`, { params });
  }

  listOrdenesPendientes(params: ListOrdenesPendientesParams = {}): Observable<PageOrdenesFarmaciaPendientes> {
    let p = new HttpParams()
      .set('page', String(params.page ?? 0))
      .set('size', String(params.size ?? 20));
    if (params.q?.trim()) p = p.set('q', params.q.trim());
    return this.http.get<PageOrdenesFarmaciaPendientes>(`${this.apiUrl}/ordenes-pendientes`, { params: p });
  }

  dispensarOrden(request: DispensarOrdenRequestDto): Observable<FarmaciaDispensacionDto[]> {
    return this.http.post<FarmaciaDispensacionDto[]>(`${this.apiUrl}/dispensar-orden`, request);
  }
}
