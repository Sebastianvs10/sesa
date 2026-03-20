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
  consultaId?: number;
  valorTotal: number;
  estado?: string;
  descripcion?: string;
  fechaFactura?: string;
  createdAt?: string;
  codigoCups?: string;
  descripcionCups?: string;
  tipoServicio?: string;
  responsablePago?: string;
  cuotaModeradora?: number;
  numeroAutorizacionEps?: string;
  /** Facturación electrónica DIAN / FEV */
  dianEstado?: string;
  dianCufe?: string;
  dianQrUrl?: string;
  /** Días hábiles restantes para radicación (22 d normativa) */
  diasParaRadicacion?: number;
  vencidaRadicacion?: boolean;
  /** Detalle multiclínea (cuenta médica). */
  items?: FacturaItemDto[];
}

export interface FacturaItemDto {
  id?: number;
  itemIndex?: number;
  codigoCups?: string;
  descripcionCups?: string;
  tipoServicio?: string;
  cantidad?: number;
  valorUnitario?: number;
  valorTotal?: number;
}

export interface FacturaItemRequestDto {
  itemIndex?: number;
  codigoCups?: string;
  descripcionCups?: string;
  tipoServicio?: string;
  cantidad?: number;
  valorUnitario?: number;
  valorTotal?: number;
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
  /** Detalle multiclínea (opcional). Si se envía, valorTotal se calcula como suma de ítems. */
  items?: FacturaItemRequestDto[];
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
  /** Facturas que superaron 22 días hábiles sin radicar */
  cantidadVencidaRadicacion?: number;
  montoVencidoRadicacion?: number;
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

export interface FacturaDetalleCompleto {
  factura: FacturaDto;
  ordenId?: number;
  consultaId?: number;
  fechaConsultaIso?: string;
  codigoCie10Consulta?: string;
  tipoOrden?: string;
  valorEstimadoOrden?: number;
}

/** Orden clínica sin factura asociada (medicamentos, laboratorio, procedimientos). */
export interface OrdenPendienteFacturaDto {
  id: number;
  pacienteId: number;
  pacienteNombre: string;
  pacienteDocumento?: string;
  tipo: string;
  detalle?: string;
  valorEstimado?: number;
  fechaOrden?: string;
  consultaId?: number;
  estado?: string;
  estadoDispensacionFarmacia?: string;
}

/** Tipo de alerta del dashboard predictivo. */
export type TipoAlertaFacturacion = 'POR_VENCER_RADICACION' | 'VENCIDA_RADICACION' | 'GLOSA_PENDIENTE' | 'ORDEN_SIN_FACTURA';

export interface AlertaFacturacionDto {
  tipo: TipoAlertaFacturacion;
  titulo: string;
  mensaje: string;
  facturaId?: number;
  numeroFactura?: string;
  glosaId?: number;
  ordenId?: number;
  diasRestantes?: number;
  monto?: number;
  epsNombre?: string;
}

export interface AlertasFacturacionDto {
  alertas: AlertaFacturacionDto[];
  totalPorVencer: number;
  totalVencidas: number;
  totalGlosasPendientes: number;
}

export interface ChecklistRadicacionDto {
  listo: boolean;
  errores: string[];
  advertencias: string[];
  resumen: string;
}

export type TipoTareaFacturador = 'RADICAR' | 'RESPONDER_GLOSA';

export interface TareaFacturadorDto {
  tipo: TipoTareaFacturador;
  id: number;
  referencia: string;
  descripcion: string;
  diasRestantes?: number;
  vencida?: boolean;
  monto?: number;
}

export interface BandejaFacturadorDto {
  tareas: TareaFacturadorDto[];
  totalFacturasPorRadicar: number;
  totalGlosasPendientes: number;
}

export interface FacturaLoteResultDto {
  facturasCreadas: FacturaDto[];
  errores: string[];
  totalProcesadas: number;
  totalCreadas: number;
}

/** Evento de la línea de tiempo de trazabilidad de una factura. */
export type TipoEventoTimeline = 'CREADA' | 'EMITIDA_FEV' | 'RADICADA' | 'GLOSA' | 'PAGADA' | 'RECHAZADA' | 'ANULADA';

export interface FacturaTimelineEventDto {
  tipo: TipoEventoTimeline;
  fecha?: string;
  titulo: string;
  descripcion?: string;
  referencia?: string;
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

  /** Órdenes clínicas sin factura (pendientes de facturar). */
  ordenesPendientesDeFacturar(page = 0, size = 50): Observable<PageResult<OrdenPendienteFacturaDto>> {
    const params = new HttpParams().set('page', page).set('size', size).set('sort', 'createdAt,desc');
    return this.http.get<PageResult<OrdenPendienteFacturaDto>>(`${this.apiUrl}/ordenes-pendientes`, { params });
  }

  getById(id: number): Observable<FacturaDto> {
    return this.http.get<FacturaDto>(`${this.apiUrl}/${id}`);
  }

  /** Detalle con trazabilidad a orden y consulta (HC). */
  getDetalleCompleto(id: number): Observable<FacturaDetalleCompleto> {
    return this.http.get<FacturaDetalleCompleto>(`${this.apiUrl}/${id}/detalle-completo`);
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

  /** Emitir factura electrónica DIAN/FEV (si la empresa tiene config activa). */
  emitirElectronica(id: number): Observable<FacturaDto> {
    return this.http.post<FacturaDto>(`${this.apiUrl}/${id}/emitir-electronica`, null);
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

  /** Generación automática de RIPS (por defecto mes anterior). Opcional: { desde, hasta } en formato yyyy-MM-dd. */
  generarRipsAutomatico(body?: { desde?: string; hasta?: string }): Observable<Record<string, string>> {
    return this.http.post<Record<string, string>>(`${this.apiUrl}/rips/generar-automatico`, body ?? {});
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

  /** Alertas para dashboard predictivo (facturas por vencer/vencidas, glosas pendientes). */
  getAlertas(): Observable<AlertasFacturacionDto> {
    return this.http.get<AlertasFacturacionDto>(`${this.apiUrl}/alertas`);
  }

  /** Checklist pre-radicación para una factura. */
  getChecklistRadicacion(facturaId: number): Observable<ChecklistRadicacionDto> {
    return this.http.get<ChecklistRadicacionDto>(`${this.apiUrl}/${facturaId}/checklist-radicacion`);
  }

  /** Bandeja del facturador: tareas (radicar, responder glosas). */
  getBandejaFacturador(): Observable<BandejaFacturadorDto> {
    return this.http.get<BandejaFacturadorDto>(`${this.apiUrl}/bandeja-facturador`);
  }

  /** Facturación por lote: agrupa órdenes por paciente y crea una factura por paciente. */
  createFromLote(ordenIds: number[]): Observable<FacturaLoteResultDto> {
    return this.http.post<FacturaLoteResultDto>(`${this.apiUrl}/lote`, { ordenIds });
  }

  /** Línea de tiempo de la factura (creada → FEV → radicada → glosa → estado). */
  getTimeline(facturaId: number): Observable<FacturaTimelineEventDto[]> {
    return this.http.get<FacturaTimelineEventDto[]>(`${this.apiUrl}/${facturaId}/timeline`);
  }

  /** Descarga el libro de facturación en CSV para el período indicado. */
  descargarLibro(desde: string, hasta: string, estado?: string): Observable<Blob> {
    let params = new HttpParams().set('desde', desde).set('hasta', hasta);
    if (estado) params = params.set('estado', estado);
    return this.http.get(`${this.apiUrl}/reporte-libro/export`, {
      params,
      responseType: 'blob',
    });
  }

  /** Guarda un blob como archivo descargado (para libro de facturación). */
  guardarDescarga(blob: Blob, nombreArchivo: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = nombreArchivo;
    a.click();
    URL.revokeObjectURL(url);
  }
}
