/**
 * Facturación — Módulo de cuentas médicas Colombia (RIPS, glosas, estados).
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { SesaSkeletonComponent } from '../../shared/components/sesa-skeleton/sesa-skeleton.component';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';
import { SesaConfirmDialogService } from '../../shared/components/sesa-confirm-dialog/sesa-confirm-dialog.component';
import {
  AlertaFacturacionDto,
  AlertasFacturacionDto,
  BandejaFacturadorDto,
  ChecklistRadicacionDto,
  FacturaDto,
  FacturaFiltros,
  FacturaItemRequestDto,
  FacturaRequestDto,
  FacturaService,
  FacturaTimelineEventDto,
  OrdenPendienteFacturaDto,
  ResumenFacturacion,
  TareaFacturadorDto,
} from '../../core/services/factura.service';
import { PacienteDto, PacienteService } from '../../core/services/paciente.service';
import { CupCatalogoDto, CupsCatalogoService } from '../../core/services/cups-catalogo.service';
import { RadicacionRequestDto, RadicacionService } from '../../core/services/radicacion.service';

type EstadoFactura = 'PENDIENTE' | 'EN_PROCESO' | 'PAGADA' | 'RECHAZADA' | 'ANULADA';
type PanelMode = 'crear' | 'rips' | 'radicacion' | 'trazabilidad';

@Component({
  standalone: true,
  selector: 'sesa-facturacion-page',
  imports: [CommonModule, FormsModule, RouterLink, SesaCardComponent, SesaSkeletonComponent, CurrencyPipe, DatePipe],
  templateUrl: './facturacion.page.html',
  styleUrl: './facturacion.page.scss',
})
export class FacturacionPageComponent implements OnInit {
  private readonly facturaService = inject(FacturaService);
  private readonly pacienteService = inject(PacienteService);
  private readonly cupsCatalogoService = inject(CupsCatalogoService);
  private readonly radicacionService = inject(RadicacionService);
  private readonly toast = inject(SesaToastService);
  private readonly confirm = inject(SesaConfirmDialogService);

  /* ── Estado principal ─────────────────────────────────────── */
  pacientes = signal<PacienteDto[]>([]);
  facturas = signal<FacturaDto[]>([]);
  resumen = signal<ResumenFacturacion | null>(null);

  loading = signal(true);
  loadingResumen = signal(true);
  saving = signal(false);

  totalElements = signal(0);
  totalPages = signal(0);
  currentPage = signal(0);
  readonly pageSize = 20;

  /** Órdenes clínicas sin factura (medicamentos, laboratorio, procedimientos). */
  ordenesPendientes = signal<OrdenPendienteFacturaDto[]>([]);
  loadingOrdenesPendientes = signal(false);
  totalOrdenesPendientes = signal(0);
  /** IDs de órdenes seleccionadas para facturación por lote. */
  ordenesSeleccionadas = signal<Set<number>>(new Set());

  /** Alertas dashboard predictivo (facturas por vencer, vencidas, glosas). */
  alertas = signal<AlertasFacturacionDto | null>(null);
  loadingAlertas = signal(false);
  /** Bandeja del facturador (tareas: radicar, responder glosas). */
  bandeja = signal<BandejaFacturadorDto | null>(null);
  loadingBandeja = signal(false);
  /** Checklist pre-radicación al abrir panel radicar. */
  checklistRadicacion = signal<ChecklistRadicacionDto | null>(null);
  loadingChecklist = signal(false);
  /** Facturación por lote en curso. */
  guardandoLote = signal(false);

  /* ── Filtros ──────────────────────────────────────────────── */
  filtroEstado = '';
  filtroDesde = '';
  filtroHasta = '';
  filtroPacienteId: number | null = null;

  /* ── Panel lateral ────────────────────────────────────────── */
  panelOpen = signal(false);
  panelMode = signal<PanelMode>('crear');

  /** Orden desde la que se abrió el panel (Generar factura). Null si es factura manual. */
  ordenOrigen = signal<OrdenPendienteFacturaDto | null>(null);

  /** Factura seleccionada para registrar radicación. */
  facturaParaRadicacion = signal<FacturaDto | null>(null);
  formRadicacion: RadicacionRequestDto = { facturaId: 0, estado: 'RADICADA' };
  savingRadicacion = signal(false);

  /** Factura en panel de trazabilidad (timeline). */
  facturaTrazabilidad = signal<FacturaDto | null>(null);
  timeline = signal<FacturaTimelineEventDto[]>([]);
  loadingTimeline = signal(false);

  /** Libro de facturación: export CSV. */
  libroDesde = '';
  libroHasta = '';
  libroEstado = '';
  descargandoLibro = signal(false);

  /* ── Formulario nueva factura ─────────────────────────────── */
  form: FacturaRequestDto = this.emptyForm();

  /* ── RIPS ─────────────────────────────────────────────────── */
  ripsDesde = '';
  ripsHasta = '';
  generandoRips = signal(false);

  /** ID de factura en proceso de emisión electrónica */
  emitirElectronicaId = signal<number | null>(null);

  /** Catálogo CUPS (Colombia) desde API */
  catalogoCups = signal<CupCatalogoDto[]>([]);
  loadingCups = signal(false);
  filtroCups = signal('');

  /* ── Computed ─────────────────────────────────────────────── */
  readonly estados: EstadoFactura[] = ['PENDIENTE', 'EN_PROCESO', 'PAGADA', 'RECHAZADA', 'ANULADA'];

  readonly paginas = computed(() => {
    const n = Math.min(this.totalPages(), 10);
    return Array.from({ length: n }, (_, i) => i);
  });

  /** CUPS filtrados por búsqueda (código o descripción) */
  readonly catalogoCupsFiltrados = computed(() => {
    const lista = this.catalogoCups();
    const q = (this.filtroCups() || '').trim().toLowerCase();
    if (!q) return lista;
    return lista.filter(
      (c) =>
        c.codigo.toLowerCase().includes(q) ||
        (c.descripcion && c.descripcion.toLowerCase().includes(q))
    );
  });

  ngOnInit(): void {
    this.cargarPacientes();
    this.cargarResumen();
    this.cargarCatalogoCups();
    this.cargarOrdenesPendientes();
    this.cargarAlertas();
    this.cargarBandeja();
    this.buscar();
    const hoy = new Date().toISOString().slice(0, 10);
    const primerDiaMes = new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().slice(0, 10);
    if (!this.libroDesde) this.libroDesde = primerDiaMes;
    if (!this.libroHasta) this.libroHasta = hoy;
  }

  /* ── Carga ────────────────────────────────────────────────── */
  private cargarPacientes(): void {
    this.pacienteService.list(0, 500).subscribe({
      next: (res) => this.pacientes.set(res.content ?? []),
    });
  }

  cargarCatalogoCups(): void {
    this.loadingCups.set(true);
    this.cupsCatalogoService.listar().subscribe({
      next: (list) => {
        this.catalogoCups.set(list ?? []);
        this.loadingCups.set(false);
      },
      error: () => this.loadingCups.set(false),
    });
  }

  cargarResumen(): void {
    this.loadingResumen.set(true);
    this.facturaService.resumen().subscribe({
      next: (r) => {
        this.resumen.set(r);
        this.loadingResumen.set(false);
      },
      error: () => this.loadingResumen.set(false),
    });
  }

  cargarOrdenesPendientes(): void {
    this.loadingOrdenesPendientes.set(true);
    this.facturaService.ordenesPendientesDeFacturar(0, 50).subscribe({
      next: (res) => {
        this.ordenesPendientes.set(res.content ?? []);
        this.totalOrdenesPendientes.set(res.totalElements ?? 0);
        this.loadingOrdenesPendientes.set(false);
      },
      error: () => this.loadingOrdenesPendientes.set(false),
    });
  }

  cargarAlertas(): void {
    this.loadingAlertas.set(true);
    this.facturaService.getAlertas().subscribe({
      next: (r) => {
        this.alertas.set(r);
        this.loadingAlertas.set(false);
      },
      error: () => this.loadingAlertas.set(false),
    });
  }

  cargarBandeja(): void {
    this.loadingBandeja.set(true);
    this.facturaService.getBandejaFacturador().subscribe({
      next: (r) => {
        this.bandeja.set(r);
        this.loadingBandeja.set(false);
      },
      error: () => this.loadingBandeja.set(false),
    });
  }

  buscar(page = 0): void {
    this.loading.set(true);
    this.currentPage.set(page);
    const filtros: FacturaFiltros = {
      page,
      size: this.pageSize,
    };
    if (this.filtroEstado) filtros.estado = this.filtroEstado;
    if (this.filtroDesde) filtros.desde = this.filtroDesde;
    if (this.filtroHasta) filtros.hasta = this.filtroHasta;
    if (this.filtroPacienteId) filtros.pacienteId = this.filtroPacienteId;

    this.facturaService.listAll(filtros).subscribe({
      next: (res) => {
        this.facturas.set(res.content ?? []);
        this.totalElements.set(res.totalElements ?? 0);
        this.totalPages.set(res.totalPages ?? 0);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        const msg = (err as { error?: { error?: string } })?.error?.error ?? 'Error al cargar facturas';
        this.toast.error(msg, 'Error');
        this.loading.set(false);
      },
    });
  }

  limpiarFiltros(): void {
    this.filtroEstado = '';
    this.filtroDesde = '';
    this.filtroHasta = '';
    this.filtroPacienteId = null;
    this.buscar();
  }

  /* ── Panel ────────────────────────────────────────────────── */
  abrirPanelCrear(): void {
    this.ordenOrigen.set(null);
    this.form = this.emptyForm();
    this.panelMode.set('crear');
    this.panelOpen.set(true);
  }

  /** Abre el panel de nueva factura con datos prellenados desde una orden pendiente (flujo IPS: orden → cuenta médica). */
  abrirPanelCrearDesdeOrden(orden: OrdenPendienteFacturaDto): void {
    this.ordenOrigen.set(orden);
    const tipoRips = this.mapearTipoOrdenATipoRips(orden.tipo);
    const valorEst = orden.valorEstimado ?? 0;
    this.form = {
      ...this.emptyForm(),
      pacienteId: orden.pacienteId,
      ordenId: orden.id,
      valorTotal: valorEst,
      descripcion: orden.detalle
        ? `${orden.tipo}: ${orden.detalle}`
        : `Orden #${orden.id} - ${orden.tipo}`,
      codigoCups: '',
      descripcionCups: orden.detalle ?? orden.tipo,
      tipoServicio: tipoRips,
      items: [{
        itemIndex: 0,
        codigoCups: '',
        descripcionCups: orden.detalle ?? orden.tipo,
        tipoServicio: tipoRips,
        cantidad: 1,
        valorUnitario: valorEst,
        valorTotal: valorEst,
      }],
    };
    this.panelMode.set('crear');
    this.panelOpen.set(true);
  }

  /** Mapea tipo de orden clínica al tipo de servicio RIPS (archivos AC, AP, AM, etc.). */
  private mapearTipoOrdenATipoRips(tipo: string): string {
    const t = (tipo || '').toUpperCase();
    if (t === 'LABORATORIO') return 'LABORATORIO';
    if (t === 'MEDICAMENTO') return 'MEDICAMENTO';
    if (t === 'COMPUESTA') return 'COMPUESTA';
    if (t === 'PROCEDIMIENTO' || t === 'IMAGEN') return 'PROCEDIMIENTO';
    if (t === 'CONSULTA') return 'CONSULTA_EXTERNA';
    return t || 'PROCEDIMIENTO';
  }

  abrirPanelRips(): void {
    this.panelMode.set('rips');
    this.panelOpen.set(true);
  }

  abrirPanelRadicacion(factura: FacturaDto): void {
    this.facturaParaRadicacion.set(factura);
    this.checklistRadicacion.set(null);
    const hoy = new Date().toISOString().slice(0, 10);
    this.formRadicacion = {
      facturaId: factura.id,
      fechaRadicacion: hoy,
      numeroRadicado: '',
      epsCodigo: factura.epsCodigo ?? '',
      epsNombre: factura.epsNombre ?? '',
      estado: 'RADICADA',
      cuv: '',
      observaciones: '',
    };
    this.panelMode.set('radicacion');
    this.panelOpen.set(true);
    this.loadingChecklist.set(true);
    this.facturaService.getChecklistRadicacion(factura.id).subscribe({
      next: (c) => {
        this.checklistRadicacion.set(c);
        this.loadingChecklist.set(false);
      },
      error: () => this.loadingChecklist.set(false),
    });
  }

  /** True si el checklist indica errores y no se debe permitir registrar radicación. */
  radicacionBloqueadaPorChecklist(): boolean {
    const chk = this.checklistRadicacion();
    return !!chk && !chk.listo && !!chk.errores && chk.errores.length > 0;
  }

  guardarRadicacion(): void {
    if (!this.formRadicacion.facturaId) return;
    this.savingRadicacion.set(true);
    const payload = {
      ...this.formRadicacion,
      fechaRadicacion: this.formRadicacion.fechaRadicacion
        ? new Date(this.formRadicacion.fechaRadicacion + 'T12:00:00Z').toISOString()
        : new Date().toISOString(),
    };
    this.radicacionService.create(payload).subscribe({
      next: () => {
        this.toast.success('Radicación registrada.', 'Éxito');
        this.savingRadicacion.set(false);
        this.cerrarPanel();
        this.buscar(this.currentPage());
        this.cargarResumen();
      },
      error: (err: unknown) => {
        const msg = (err as { error?: { error?: string } })?.error?.error ?? 'No se pudo registrar la radicación';
        this.toast.error(msg, 'Error');
        this.savingRadicacion.set(false);
      },
    });
  }

  cerrarPanel(): void {
    this.ordenOrigen.set(null);
    this.facturaParaRadicacion.set(null);
    this.checklistRadicacion.set(null);
    this.facturaTrazabilidad.set(null);
    this.timeline.set([]);
    this.panelOpen.set(false);
  }

  /** Abre el panel de trazabilidad (timeline) de una factura. */
  abrirTrazabilidad(factura: FacturaDto): void {
    this.facturaTrazabilidad.set(factura);
    this.timeline.set([]);
    this.panelMode.set('trazabilidad');
    this.panelOpen.set(true);
    this.loadingTimeline.set(true);
    this.facturaService.getTimeline(factura.id).subscribe({
      next: (events) => {
        this.timeline.set(events ?? []);
        this.loadingTimeline.set(false);
      },
      error: () => this.loadingTimeline.set(false),
    });
  }

  /** Descarga el libro de facturación en CSV. */
  descargarLibro(): void {
    const desde = this.libroDesde || new Date().toISOString().slice(0, 10);
    const hasta = this.libroHasta || new Date().toISOString().slice(0, 10);
    if (!this.libroDesde || !this.libroHasta) {
      this.toast.warning('Indique rango de fechas (desde y hasta).', 'Libro de facturación');
      return;
    }
    this.descargandoLibro.set(true);
    this.facturaService.descargarLibro(desde, hasta, this.libroEstado || undefined).subscribe({
      next: (blob) => {
        this.facturaService.guardarDescarga(blob, `libro_facturacion_${desde}_${hasta}.csv`);
        this.toast.success('Descarga iniciada.', 'Libro de facturación');
        this.descargandoLibro.set(false);
      },
      error: () => {
        this.toast.error('Error al generar el libro.', 'Libro de facturación');
        this.descargandoLibro.set(false);
      },
    });
  }

  /** Clase CSS por tipo de evento del timeline. */
  timelineEventClass(tipo: string): string {
    const m: Record<string, string> = {
      CREADA: 'fac-timeline--creada',
      EMITIDA_FEV: 'fac-timeline--fev',
      RADICADA: 'fac-timeline--radicada',
      GLOSA: 'fac-timeline--glosa',
      PAGADA: 'fac-timeline--pagada',
      RECHAZADA: 'fac-timeline--rechazada',
      ANULADA: 'fac-timeline--anulada',
    };
    return m[tipo] ?? '';
  }

  /** Marca o desmarca una orden para facturación por lote. */
  toggleOrdenSeleccionada(ordenId: number): void {
    const set = new Set(this.ordenesSeleccionadas());
    if (set.has(ordenId)) set.delete(ordenId);
    else set.add(ordenId);
    this.ordenesSeleccionadas.set(set);
  }

  /** Selecciona o deselecciona todas las órdenes visibles. */
  toggleTodasOrdenes(checked: boolean): void {
    if (checked) {
      const ids = this.ordenesPendientes().map((o) => o.id);
      this.ordenesSeleccionadas.set(new Set(ids));
    } else {
      this.ordenesSeleccionadas.set(new Set());
    }
  }

  estaOrdenSeleccionada(ordenId: number): boolean {
    return this.ordenesSeleccionadas().has(ordenId);
  }

  /** Abre el panel de radicación cargando la factura por ID (desde alertas/bandeja). */
  abrirPanelRadicacionPorId(facturaId: number): void {
    const enLista = this.facturas().find((f) => f.id === facturaId);
    if (enLista) {
      this.abrirPanelRadicacion(enLista);
      return;
    }
    this.facturaService.getById(facturaId).subscribe({
      next: (f) => this.abrirPanelRadicacion(f),
      error: (err: unknown) => {
        const msg = (err as { error?: { message?: string } })?.error?.message ?? 'No se pudo cargar la factura';
        this.toast.error(msg, 'Error');
      },
    });
  }

  /** Facturación por lote: agrupa por paciente y crea una factura por paciente. */
  facturarLote(): void {
    const ids = Array.from(this.ordenesSeleccionadas());
    if (ids.length === 0) {
      this.toast.warning('Seleccione al menos una orden.', 'Facturación por lote');
      return;
    }
    this.guardandoLote.set(true);
    this.facturaService.createFromLote(ids).subscribe({
      next: (res) => {
        this.guardandoLote.set(false);
        this.ordenesSeleccionadas.set(new Set());
        if (res.facturasCreadas?.length) {
          this.toast.success(
            `Se crearon ${res.totalCreadas} factura(s).`,
            'Facturación por lote'
          );
          this.cargarOrdenesPendientes();
          this.buscar(this.currentPage());
          this.cargarResumen();
          this.cargarAlertas();
          this.cargarBandeja();
        }
        if (res.errores?.length) {
          res.errores.forEach((e) => this.toast.warning(e, 'Aviso'));
        }
        if (!res.facturasCreadas?.length && !res.errores?.length) {
          this.toast.info(res.errores?.[0] ?? 'Ninguna orden pendiente de facturar.', 'Facturación por lote');
        }
      },
      error: (err: unknown) => {
        this.guardandoLote.set(false);
        const msg = (err as { error?: { message?: string } })?.error?.message ?? 'Error al facturar por lote';
        this.toast.error(msg, 'Error');
      },
    });
  }

  /* ── CRUD ─────────────────────────────────────────────────── */
  guardarFactura(): void {
    if (!this.form.pacienteId) {
      this.toast.error('Paciente es obligatorio.', 'Validación');
      return;
    }
    if (this.form.items?.length) {
      this.actualizarTotalDesdeItems();
      if (this.form.valorTotal <= 0) {
        this.toast.error('Revise los valores de los ítems.', 'Validación');
        return;
      }
    } else if (!this.form.valorTotal || this.form.valorTotal <= 0) {
      this.toast.error('Valor total es obligatorio.', 'Validación');
      return;
    }
    this.saving.set(true);
    const payload: FacturaRequestDto = { ...this.form };
    if (payload.items?.length === 0) payload.items = undefined;
    this.facturaService.create(payload).subscribe({
      next: () => {
        this.toast.success('Factura creada correctamente.', 'Éxito');
        this.saving.set(false);
        this.cerrarPanel();
        this.buscar(0);
        this.cargarResumen();
        this.cargarOrdenesPendientes();
      },
      error: (err: unknown) => {
        const msg = (err as { error?: { error?: string } })?.error?.error ?? 'No se pudo crear la factura';
        this.toast.error(msg, 'Error');
        this.saving.set(false);
      },
    });
  }

  /** Emitir factura electrónica DIAN/FEV. */
  emitirElectronica(id: number): void {
    this.emitirElectronicaId.set(id);
    this.facturaService.emitirElectronica(id).subscribe({
      next: (updated) => {
        this.facturas.update((list) =>
          list.map((f) => (f.id === updated.id ? updated : f))
        );
        this.toast.success('Factura electrónica emitida.', 'FEV');
        this.emitirElectronicaId.set(null);
        this.cargarResumen();
      },
      error: (err: unknown) => {
        const msg = (err as { error?: { error?: string } })?.error?.error ?? 'No se pudo emitir';
        this.toast.error(msg, 'Error FEV');
        this.emitirElectronicaId.set(null);
      },
    });
  }

  async cambiarEstado(factura: FacturaDto, estado: EstadoFactura): Promise<void> {
    const etiquetas: Record<EstadoFactura, string> = {
      PENDIENTE: 'marcar como Pendiente',
      EN_PROCESO: 'poner En Proceso (glosa)',
      PAGADA: 'marcar como Pagada',
      RECHAZADA: 'Rechazar',
      ANULADA: 'Anular',
    };
    const tipo = estado === 'ANULADA' ? 'danger' : 'warning';
    const confirmado = await this.confirm.confirm({
      title: `Confirmar cambio de estado`,
      message: `¿Deseas ${etiquetas[estado]} la factura #${factura.numeroFactura ?? factura.id}?`,
      confirmLabel: etiquetas[estado],
      type: tipo,
    });
    if (!confirmado) return;

    this.facturaService.cambiarEstado(factura.id, estado).subscribe({
      next: (updated) => {
        this.facturas.update((list) =>
          list.map((f) => (f.id === updated.id ? updated : f))
        );
        this.toast.success(`Estado cambiado a ${estado}.`, 'Actualizado');
        this.cargarResumen();
      },
      error: (err: unknown) => {
        const msg = (err as { error?: { error?: string } })?.error?.error ?? 'No se pudo cambiar el estado';
        this.toast.error(msg, 'Error');
      },
    });
  }

  ripsArchivos: Record<string, string> | null = null;

  /* ── RIPS CSV genérico ─────────────────────────────────── */
  descargarRips(): void {
    if (!this.ripsDesde || !this.ripsHasta) {
      this.toast.error('Selecciona el rango de fechas para el RIPS.', 'Validación');
      return;
    }
    this.generandoRips.set(true);
    this.facturaService.exportRips(this.ripsDesde, this.ripsHasta).subscribe({
      next: (csv) => {
        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `RIPS_${this.ripsDesde}_${this.ripsHasta}.csv`;
        a.click();
        URL.revokeObjectURL(url);
        this.toast.success('Archivo RIPS descargado correctamente.', 'Exportación RIPS');
        this.generandoRips.set(false);
      },
      error: (err: unknown) => {
        const msg = (err as { error?: { error?: string } })?.error?.error ?? 'Error al exportar RIPS';
        this.toast.error(msg, 'Error RIPS');
        this.generandoRips.set(false);
      },
    });
  }

  /** Generación automática RIPS (mes anterior por defecto). */
  generarRipsAutomatico(): void {
    this.generandoRips.set(true);
    const lastMonth = new Date();
    lastMonth.setMonth(lastMonth.getMonth() - 1);
    const desde = lastMonth.toISOString().slice(0, 10);
    lastMonth.setMonth(lastMonth.getMonth() + 1);
    lastMonth.setDate(0);
    const hasta = lastMonth.toISOString().slice(0, 10);
    this.facturaService.generarRipsAutomatico({ desde, hasta }).subscribe({
      next: (archivos) => {
        this.ripsDesde = desde;
        this.ripsHasta = hasta;
        this.ripsArchivos = archivos;
        this.generandoRips.set(false);
        this.toast.success('RIPS automático generado (mes anterior). Descarga cada archivo.', 'RIPS automático');
      },
      error: (err: unknown) => {
        const msg = (err as { error?: { error?: string } })?.error?.error ?? 'Error al generar RIPS automático';
        this.toast.error(msg, 'Error RIPS');
        this.generandoRips.set(false);
      },
    });
  }

  /* ── RIPS estructurado Res. 3374/2000 ───────────────────── */
  generarRipsEstructurado(): void {
    if (!this.ripsDesde || !this.ripsHasta) {
      this.toast.error('Selecciona el rango de fechas.', 'Validación');
      return;
    }
    this.generandoRips.set(true);
    this.facturaService.exportRipsEstructurado(this.ripsDesde, this.ripsHasta).subscribe({
      next: (archivos) => {
        this.ripsArchivos = archivos;
        this.generandoRips.set(false);
        this.toast.success('RIPS estructurado generado. Descarga cada archivo.', 'RIPS Res. 3374/2000');
      },
      error: (err: unknown) => {
        const msg = (err as { error?: { error?: string } })?.error?.error ?? 'Error al generar RIPS';
        this.toast.error(msg, 'Error RIPS');
        this.generandoRips.set(false);
      },
    });
  }

  descargarArchivoRips(tipo: string): void {
    if (!this.ripsArchivos?.[tipo]) return;
    const nombre = `RIPS_${tipo}_${this.ripsDesde}_${this.ripsHasta}.txt`;
    this.facturaService.descargarArchivoRips(this.ripsArchivos[tipo], nombre);
  }

  /* ── Helpers UI ───────────────────────────────────────────── */
  estadoLabel(estado: string | undefined): string {
    const map: Record<string, string> = {
      PENDIENTE: 'Pendiente',
      EN_PROCESO: 'En proceso',
      PAGADA: 'Pagada',
      RECHAZADA: 'Rechazada',
      ANULADA: 'Anulada',
    };
    return map[estado ?? ''] ?? (estado ?? '—');
  }

  estadoClass(estado: string | undefined): string {
    const map: Record<string, string> = {
      PENDIENTE: 'badge-warning',
      EN_PROCESO: 'badge-info',
      PAGADA: 'badge-success',
      RECHAZADA: 'badge-danger',
      ANULADA: 'badge-muted',
    };
    return map[estado ?? ''] ?? '';
  }

  dianEstadoLabel(estado: string | undefined): string {
    const map: Record<string, string> = {
      ACEPTADA: 'Aceptada',
      PENDIENTE_DIAN: 'Pendiente',
      NO_ACTIVA: 'No activa',
      RECHAZADA: 'Rechazada',
    };
    return map[estado ?? ''] ?? (estado ?? '—');
  }

  private emptyForm(): FacturaRequestDto {
    return {
      numeroFactura: '',
      pacienteId: 0,
      valorTotal: 0,
      estado: 'PENDIENTE',
      descripcion: '',
      codigoCups: '',
      descripcionCups: '',
      tipoServicio: '',
      responsablePago: '',
      cuotaModeradora: 0,
      numeroAutorizacionEps: '',
      items: [],
    };
  }

  /** Añade una línea al detalle de la factura (cuenta médica multiclínea). */
  agregarItemFactura(): void {
    if (!this.form.items) this.form.items = [];
    this.form.items.push({
      itemIndex: this.form.items.length,
      codigoCups: this.form.codigoCups || '',
      descripcionCups: this.form.descripcionCups || '',
      tipoServicio: this.form.tipoServicio || '',
      cantidad: 1,
      valorUnitario: 0,
      valorTotal: 0,
    });
    this.actualizarTotalDesdeItems();
  }

  quitarItemFactura(index: number): void {
    if (!this.form.items) return;
    this.form.items.splice(index, 1);
    this.form.items.forEach((it, i) => (it.itemIndex = i));
    this.actualizarTotalDesdeItems();
  }

  /** Recalcula valorTotal desde ítems y actualiza form.valorTotal. */
  actualizarTotalDesdeItems(): void {
    if (!this.form.items?.length) return;
    this.form.items.forEach((it) => {
      const cant = it.cantidad ?? 1;
      const vU = it.valorUnitario ?? 0;
      it.valorTotal = cant * vU;
    });
    this.form.valorTotal = this.form.items.reduce((sum, it) => sum + (it.valorTotal ?? 0), 0);
  }

  /** Aplica el CUPS actual de cabecera al último ítem o agrega uno nuevo. */
  aplicarCupsAItem(): void {
    if (!this.form.codigoCups?.trim()) {
      this.toast.warning('Seleccione un CUPS en cabecera.', 'CUPS');
      return;
    }
    const item = this.catalogoCups().find((c) => c.codigo === this.form.codigoCups);
    const valorU = item?.precioSugerido ?? 0;
    if (!this.form.items?.length) this.agregarItemFactura();
    const items = this.form.items;
    const last = items?.length ? items[items.length - 1] : null;
    if (!last) return;
    last.codigoCups = this.form.codigoCups;
    last.descripcionCups = this.form.descripcionCups || item?.descripcion || '';
    last.tipoServicio = this.form.tipoServicio || item?.tipoServicio || 'PROCEDIMIENTO';
    last.valorUnitario = valorU;
    last.cantidad = last.cantidad ?? 1;
    last.valorTotal = last.cantidad * last.valorUnitario;
    this.actualizarTotalDesdeItems();
  }

  seleccionarCups(codigo: string): void {
    const item = this.catalogoCups().find((c) => c.codigo === codigo);
    if (item) {
      this.form.codigoCups = item.codigo;
      this.form.descripcionCups = item.descripcion;
      if (item.precioSugerido != null && item.precioSugerido > 0) {
        this.form.valorTotal = item.precioSugerido;
      }
    }
  }
}
