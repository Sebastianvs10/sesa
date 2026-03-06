/**
 * Facturación — Módulo de cuentas médicas Colombia (RIPS, glosas, estados).
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { SesaSkeletonComponent } from '../../shared/components/sesa-skeleton/sesa-skeleton.component';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';
import { SesaConfirmDialogService } from '../../shared/components/sesa-confirm-dialog/sesa-confirm-dialog.component';
import {
  FacturaDto,
  FacturaFiltros,
  FacturaRequestDto,
  FacturaService,
  ResumenFacturacion,
} from '../../core/services/factura.service';
import { PacienteDto, PacienteService } from '../../core/services/paciente.service';

type EstadoFactura = 'PENDIENTE' | 'EN_PROCESO' | 'PAGADA' | 'RECHAZADA' | 'ANULADA';
type PanelMode = 'crear' | 'rips';

@Component({
  standalone: true,
  selector: 'sesa-facturacion-page',
  imports: [CommonModule, FormsModule, SesaCardComponent, SesaSkeletonComponent, CurrencyPipe, DatePipe],
  templateUrl: './facturacion.page.html',
  styleUrl: './facturacion.page.scss',
})
export class FacturacionPageComponent implements OnInit {
  private readonly facturaService = inject(FacturaService);
  private readonly pacienteService = inject(PacienteService);
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

  /* ── Filtros ──────────────────────────────────────────────── */
  filtroEstado = '';
  filtroDesde = '';
  filtroHasta = '';
  filtroPacienteId: number | null = null;

  /* ── Panel lateral ────────────────────────────────────────── */
  panelOpen = signal(false);
  panelMode = signal<PanelMode>('crear');

  /* ── Formulario nueva factura ─────────────────────────────── */
  form: FacturaRequestDto = this.emptyForm();

  /* ── RIPS ─────────────────────────────────────────────────── */
  ripsDesde = '';
  ripsHasta = '';
  generandoRips = signal(false);

  /* ── Computed ─────────────────────────────────────────────── */
  readonly estados: EstadoFactura[] = ['PENDIENTE', 'EN_PROCESO', 'PAGADA', 'RECHAZADA', 'ANULADA'];

  readonly paginas = computed(() => {
    const n = Math.min(this.totalPages(), 10);
    return Array.from({ length: n }, (_, i) => i);
  });

  ngOnInit(): void {
    this.cargarPacientes();
    this.cargarResumen();
    this.buscar();
  }

  /* ── Carga ────────────────────────────────────────────────── */
  private cargarPacientes(): void {
    this.pacienteService.list(0, 500).subscribe({
      next: (res) => this.pacientes.set(res.content ?? []),
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
    this.form = this.emptyForm();
    this.panelMode.set('crear');
    this.panelOpen.set(true);
  }

  abrirPanelRips(): void {
    this.panelMode.set('rips');
    this.panelOpen.set(true);
  }

  cerrarPanel(): void {
    this.panelOpen.set(false);
  }

  /* ── CRUD ─────────────────────────────────────────────────── */
  guardarFactura(): void {
    if (!this.form.pacienteId || !this.form.valorTotal) {
      this.toast.error('Paciente y valor total son obligatorios.', 'Validación');
      return;
    }
    this.saving.set(true);
    this.facturaService.create(this.form).subscribe({
      next: () => {
        this.toast.success('Factura creada correctamente.', 'Éxito');
        this.saving.set(false);
        this.cerrarPanel();
        this.buscar(0);
        this.cargarResumen();
      },
      error: (err: unknown) => {
        const msg = (err as { error?: { error?: string } })?.error?.error ?? 'No se pudo crear la factura';
        this.toast.error(msg, 'Error');
        this.saving.set(false);
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
    };
  }

  // Catálogo simplificado CUPS de los procedimientos más comunes
  readonly catalogoCups = [
    { codigo: '890201', descripcion: 'Consulta médica general' },
    { codigo: '890202', descripcion: 'Consulta médica especializada' },
    { codigo: '890203', descripcion: 'Consulta de urgencias' },
    { codigo: '890208', descripcion: 'Consulta odontológica general' },
    { codigo: '890209', descripcion: 'Consulta odontológica especializada' },
    { codigo: '903801', descripcion: 'Hemograma completo' },
    { codigo: '903802', descripcion: 'Glicemia' },
    { codigo: '903804', descripcion: 'Perfil lipídico' },
    { codigo: '881601', descripcion: 'Radiografía de tórax PA' },
    { codigo: '860001', descripcion: 'Electrocardiograma' },
  ];

  seleccionarCups(codigo: string): void {
    const item = this.catalogoCups.find(c => c.codigo === codigo);
    if (item) {
      this.form.codigoCups = item.codigo;
      this.form.descripcionCups = item.descripcion;
    }
  }
}
