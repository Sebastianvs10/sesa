/**
 * Farmacia — Órdenes médicas (HC), inventario y dispensación.
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';
import {
  FarmaciaService,
  FarmaciaMedicamentoDto,
  FarmaciaDispensacionDto,
  FarmaciaIndicadoresDto,
  OrdenFarmaciaPendienteDto,
  OrdenFarmaciaPendienteItemDto,
  LineaDispensacionDto,
} from '../../core/services/farmacia.service';
import { PacienteDto, PacienteService } from '../../core/services/paciente.service';

type TabFarmacia = 'ordenes' | 'inventario' | 'dispensar';

@Component({
  standalone: true,
  selector: 'sesa-farmacia-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule],
  templateUrl: './farmacia.page.html',
  styleUrl: './farmacia.page.scss',
})
export class FarmaciaPageComponent implements OnInit, OnDestroy {
  private readonly farmaciaService = inject(FarmaciaService);
  private readonly pacienteService = inject(PacienteService);
  private readonly toast = inject(SesaToastService);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly tabActiva = signal<TabFarmacia>('ordenes');
  /** Página actual de órdenes (servidor) */
  readonly ordenesRows = signal<OrdenFarmaciaPendienteDto[]>([]);
  readonly ordenesPageNumber = signal(0);
  readonly ordenesTotalElements = signal(0);
  readonly ordenesTotalPages = signal(0);
  readonly ordenesPageSize = 20;
  /** Total de órdenes pendientes sin filtro de búsqueda (KPI y badge) */
  readonly ordenesPendientesGlobalTotal = signal(0);
  readonly dispensaciones = signal<FarmaciaDispensacionDto[]>([]);
  readonly cargandoOrdenes = signal(false);
  readonly guardando = signal(false);
  readonly error = signal<string | null>(null);

  /** Inventario paginado (servidor) */
  readonly invRows = signal<FarmaciaMedicamentoDto[]>([]);
  readonly invLoading = signal(false);
  readonly invPageNumber = signal(0);
  readonly invTotalElements = signal(0);
  readonly invTotalPages = signal(0);
  readonly invPageSize = 25;

  readonly indicadores = signal<FarmaciaIndicadoresDto | null>(null);

  /** Catálogo con stock para panel dispensar / matching (hasta 500 SKUs) */
  readonly catalogoStock = signal<FarmaciaMedicamentoDto[]>([]);

  /** Resultados búsqueda dispensación directa (servidor) */
  readonly medsDispResults = signal<FarmaciaMedicamentoDto[]>([]);

  readonly qOrdenes = signal('');
  readonly qInventario = signal('');
  readonly dispMedQuery = signal('');
  readonly dispPacQuery = signal('');
  readonly showDropMed = signal(false);
  readonly showDropPac = signal(false);

  /** Pacientes en combo dispensación directa (búsqueda paginada en servidor) */
  readonly pacDropdownRows = signal<PacienteDto[]>([]);
  readonly pacDropdownLoading = signal(false);
  readonly dispPacResumen = signal('');

  readonly ordenSeleccionada = signal<OrdenFarmaciaPendienteDto | null>(null);
  readonly lineasDispensacion = signal<LineaDispensacionDto[]>([]);

  medForm = {
    nombre: '',
    lote: '',
    codigoBarras: '',
    fechaVencimiento: '',
    cantidad: '' as string,
    precio: '' as string,
    stockMinimo: '' as string,
  };

  dispForm = {
    medicamentoId: null as number | null,
    pacienteId: null as number | null,
    cantidad: '' as string,
    entregadoPor: '',
  };

  private invSearchTimer?: ReturnType<typeof setTimeout>;
  private medDispSearchTimer?: ReturnType<typeof setTimeout>;
  private ordSearchTimer?: ReturnType<typeof setTimeout>;
  private pacSearchTimer?: ReturnType<typeof setTimeout>;

  readonly medicamentosConStock = computed(() =>
    this.catalogoStock().filter((m) => m.cantidad > 0)
  );

  readonly kpiOrdenesPendientes = computed(() => this.ordenesPendientesGlobalTotal());
  readonly kpiStockBajo = computed(() => this.indicadores()?.stockBajo ?? 0);
  readonly kpiProxVencer = computed(() => this.indicadores()?.proximosAVencer30Dias ?? 0);
  readonly kpiSkus = computed(() => this.indicadores()?.totalSkusActivos ?? 0);

  ngOnInit(): void {
    this.cargarIndicadores();
    this.cargarInventarioPagina(0);
    this.cargarOrdenesPagina(0);
  }

  ngOnDestroy(): void {
    clearTimeout(this.invSearchTimer);
    clearTimeout(this.medDispSearchTimer);
    clearTimeout(this.ordSearchTimer);
    clearTimeout(this.pacSearchTimer);
  }

  setTab(tab: TabFarmacia): void {
    this.tabActiva.set(tab);
    if (tab === 'ordenes') this.cargarOrdenesPagina(0);
    if (tab === 'inventario') {
      this.cargarIndicadores();
      this.cargarInventarioPagina(0);
    }
    if (tab === 'dispensar') {
      this.scheduleMedDispSearch(true);
      this.cargarPacientesDropdown('');
    }
  }

  toolbarRefresh(): void {
    const t = this.tabActiva();
    if (t === 'ordenes') this.cargarOrdenesPagina(this.ordenesPageNumber());
    else if (t === 'inventario') {
      this.cargarIndicadores();
      this.cargarInventarioPagina(this.invPageNumber());
    } else {
      this.cargarIndicadores();
      this.scheduleMedDispSearch(true);
    }
  }

  cargarIndicadores(): void {
    this.farmaciaService.indicadoresInventario().subscribe({
      next: (d) => {
        this.indicadores.set(d);
        this.cdr.markForCheck();
      },
      error: () => {},
    });
  }

  cargarInventarioPagina(page: number): void {
    this.invLoading.set(true);
    this.farmaciaService
      .listMedicamentos({
        page,
        size: this.invPageSize,
        q: this.qInventario(),
        soloStock: false,
      })
      .subscribe({
        next: (pg) => {
          this.invRows.set(pg.content);
          this.invPageNumber.set(pg.number);
          this.invTotalElements.set(pg.totalElements);
          this.invTotalPages.set(pg.totalPages);
          this.invLoading.set(false);
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.invLoading.set(false);
          this.error.set(err?.error?.error || 'No se pudo cargar inventario');
          this.toast.error(this.error()!, 'Error');
          this.cdr.markForCheck();
        },
      });
  }

  onQInventarioInput(v: string): void {
    this.qInventario.set(v);
    clearTimeout(this.invSearchTimer);
    this.invSearchTimer = setTimeout(() => this.cargarInventarioPagina(0), 350);
  }

  invPrev(): void {
    const n = this.invPageNumber();
    if (n > 0) this.cargarInventarioPagina(n - 1);
  }

  invNext(): void {
    const n = this.invPageNumber();
    if (n < this.invTotalPages() - 1) this.cargarInventarioPagina(n + 1);
  }

  recargar(): void {
    this.cargarIndicadores();
    this.cargarInventarioPagina(this.invPageNumber());
  }

  cargarOrdenesPagina(page: number): void {
    this.cargandoOrdenes.set(true);
    this.error.set(null);
    const q = this.qOrdenes().trim();
    this.farmaciaService
      .listOrdenesPendientes({
        page,
        size: this.ordenesPageSize,
        q: q || undefined,
      })
      .subscribe({
        next: (pg) => {
          this.ordenesRows.set(pg.content ?? []);
          this.ordenesPageNumber.set(pg.number);
          this.ordenesTotalElements.set(pg.totalElements);
          this.ordenesTotalPages.set(pg.totalPages);
          if (!q) {
            this.ordenesPendientesGlobalTotal.set(pg.totalElements);
          }
          this.cargandoOrdenes.set(false);
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.error.set(err?.error?.error || 'No se pudieron cargar las órdenes');
          this.toast.error(this.error()!, 'Error');
          this.cargandoOrdenes.set(false);
          this.cdr.markForCheck();
        },
      });
  }

  onQOrdenesInput(v: string): void {
    this.qOrdenes.set(v);
    clearTimeout(this.ordSearchTimer);
    this.ordSearchTimer = setTimeout(() => this.cargarOrdenesPagina(0), 350);
  }

  limpiarBusquedaOrdenes(): void {
    this.qOrdenes.set('');
    this.cargarOrdenesPagina(0);
  }

  ordenesPrev(): void {
    const n = this.ordenesPageNumber();
    if (n > 0) this.cargarOrdenesPagina(n - 1);
  }

  ordenesNext(): void {
    const n = this.ordenesPageNumber();
    if (n < this.ordenesTotalPages() - 1) this.cargarOrdenesPagina(n + 1);
  }

  formatoPrescripcion(orden: OrdenFarmaciaPendienteDto): string {
    const parts: string[] = [];
    if (orden.cantidadPrescrita != null) {
      parts.push(
        orden.unidadMedida
          ? `${orden.cantidadPrescrita} ${orden.unidadMedida}`
          : `${orden.cantidadPrescrita}`
      );
    }
    if (orden.frecuencia?.trim()) parts.push(orden.frecuencia.trim());
    if (orden.duracionDias != null) parts.push(`${orden.duracionDias} días`);
    return parts.join(' · ');
  }

  formatoPrescripcionItem(item: OrdenFarmaciaPendienteItemDto): string {
    const parts: string[] = [];
    if (item.cantidadPrescrita != null) {
      parts.push(
        item.unidadMedida
          ? `${item.cantidadPrescrita} ${item.unidadMedida}`
          : `${item.cantidadPrescrita}`
      );
    }
    if (item.frecuencia?.trim()) parts.push(item.frecuencia.trim());
    if (item.duracionDias != null) parts.push(`${item.duracionDias} días`);
    return parts.join(' · ');
  }

  ordenConVariosItems(orden: OrdenFarmaciaPendienteDto): boolean {
    return !!orden.items && orden.items.length > 1;
  }

  buscarMedicamentoPorDetalle(detalle: string | undefined): number {
    if (!detalle?.trim()) return 0;
    const list = this.medicamentosConStock();
    if (list.length === 0) return 0;
    const texto = detalle
      .replace(/^Medicamento\s+indicado:\s*/i, '')
      .replace(/^Indicado:\s*/i, '')
      .replace(/^Orden[:\s]*/i, '')
      .trim();
    if (!texto) return 0;
    const encontrado = list.find(
      (m) =>
        m.nombre.trim().toLowerCase() === texto.toLowerCase() ||
        texto.toLowerCase().includes(m.nombre.trim().toLowerCase()) ||
        m.nombre.trim().toLowerCase().includes(texto.toLowerCase())
    );
    return encontrado ? encontrado.id : 0;
  }

  cargarCatalogoStock(onDone?: () => void): void {
    this.farmaciaService.listMedicamentos({ page: 0, size: 500, soloStock: true }).subscribe({
      next: (pg) => {
        this.catalogoStock.set(pg.content);
        if (!pg.last && pg.totalElements > pg.content.length) {
          this.toast.warning(
            `Catálogo parcial (${pg.content.length} de ${pg.totalElements} con stock). Refine con escáner o búsqueda.`,
            'Inventario amplio'
          );
        }
        onDone?.();
        this.cdr.markForCheck();
      },
      error: () => {
        this.catalogoStock.set([]);
        onDone?.();
        this.cdr.markForCheck();
      },
    });
  }

  abrirDispensarOrden(orden: OrdenFarmaciaPendienteDto): void {
    this.ordenSeleccionada.set(orden);
    this.lineasDispensacion.set([]);
    this.cargarCatalogoStock(() => {
      if (orden.items && orden.items.length > 0) {
        const lineas: LineaDispensacionDto[] = orden.items.map((it) => {
          const medicamentoId = this.buscarMedicamentoPorDetalle(it.detalle);
          const cantidad =
            it.cantidadPrescrita != null && it.cantidadPrescrita > 0 ? it.cantidadPrescrita : 1;
          return { medicamentoId: medicamentoId || 0, cantidad };
        });
        this.lineasDispensacion.set(lineas);
      } else {
        const cantidadInicial =
          orden.cantidadPrescrita != null && orden.cantidadPrescrita > 0
            ? orden.cantidadPrescrita
            : 1;
        const medicamentoId = this.buscarMedicamentoPorDetalle(orden.detalle);
        this.lineasDispensacion.set([
          { medicamentoId: medicamentoId || 0, cantidad: cantidadInicial },
        ]);
      }
      this.cdr.markForCheck();
    });
  }

  cerrarDispensarOrden(): void {
    this.ordenSeleccionada.set(null);
    this.lineasDispensacion.set([]);
    this.cdr.markForCheck();
  }

  autoAsignarInventarioDesdePrescripcion(): void {
    const orden = this.ordenSeleccionada();
    if (!orden) return;
    this.lineasDispensacion.update((lineas) => {
      if (orden.items && orden.items.length > 0) {
        return lineas.map((l, i) => {
          const it = orden.items![i];
          const id = this.buscarMedicamentoPorDetalle(it?.detalle);
          return { ...l, medicamentoId: id || l.medicamentoId };
        });
      }
      const id = this.buscarMedicamentoPorDetalle(orden.detalle);
      return lineas.map((l, i) =>
        i === 0 ? { ...l, medicamentoId: id || l.medicamentoId } : l
      );
    });
    this.toast.success('Sugerencias de inventario aplicadas. Revise antes de confirmar.', 'Auto-asignar');
    this.cdr.markForCheck();
  }

  agregarLineaDispensacion(): void {
    this.lineasDispensacion.update((l) => [...l, { medicamentoId: 0, cantidad: 1 }]);
    this.cdr.markForCheck();
  }

  quitarLineaDispensacion(index: number): void {
    this.lineasDispensacion.update((l) => l.filter((_, i) => i !== index));
    this.cdr.markForCheck();
  }

  setLineaMedicamento(index: number, medicamentoId: number): void {
    this.lineasDispensacion.update((l) => {
      const c = [...l];
      c[index] = { ...c[index], medicamentoId, cantidad: c[index].cantidad ?? 1 };
      return c;
    });
    this.cdr.markForCheck();
  }

  setLineaCantidad(index: number, cantidad: number | string): void {
    const n = typeof cantidad === 'string' ? parseInt(cantidad, 10) : cantidad;
    this.lineasDispensacion.update((l) => {
      const c = [...l];
      c[index] = { ...c[index], cantidad: isNaN(n) ? 0 : Math.max(0, n) };
      return c;
    });
    this.cdr.markForCheck();
  }

  ajustarCantidadLinea(index: number, delta: number): void {
    this.lineasDispensacion.update((l) => {
      const c = [...l];
      const actual = c[index].cantidad ?? 1;
      const next = Math.max(1, actual + delta);
      c[index] = { ...c[index], cantidad: next };
      return c;
    });
    this.cdr.markForCheck();
  }

  cantidadPrescritaLinea(index: number): number | null {
    const orden = this.ordenSeleccionada();
    if (!orden) return null;
    if (orden.items && orden.items[index]) {
      const cp = orden.items[index].cantidadPrescrita;
      return cp != null && cp > 0 ? cp : null;
    }
    if (index === 0 && orden.cantidadPrescrita != null && orden.cantidadPrescrita > 0) {
      return orden.cantidadPrescrita;
    }
    return null;
  }

  aplicarCantidadPrescrita(index: number): void {
    const n = this.cantidadPrescritaLinea(index);
    if (n != null) this.setLineaCantidad(index, n);
  }

  nombreInventarioLinea(medicamentoId: number): string {
    if (!medicamentoId) return '';
    return (
      this.catalogoStock().find((m) => m.id === medicamentoId)?.nombre ??
      this.invRows().find((m) => m.id === medicamentoId)?.nombre ??
      ''
    );
  }

  truncNombreInventario(medicamentoId: number, max = 32): string {
    const s = this.nombreInventarioLinea(medicamentoId);
    if (!s) return '';
    return s.length > max ? s.slice(0, max) + '…' : s;
  }

  enviarDispensacionOrden(): void {
    const orden = this.ordenSeleccionada();
    if (!orden) return;
    const lineas = this.lineasDispensacion()
      .map((l) => ({ medicamentoId: l.medicamentoId, lote: l.lote, cantidad: l.cantidad }))
      .filter((l) => l.medicamentoId > 0 && l.cantidad > 0);
    if (lineas.length === 0) {
      this.toast.warning('Agrega al menos un medicamento con cantidad.', 'Dispensación');
      return;
    }
    this.guardando.set(true);
    this.error.set(null);
    this.farmaciaService.dispensarOrden({ ordenId: orden.id, lineas }).subscribe({
      next: () => {
        this.toast.success('Dispensación registrada. Stock actualizado.', 'Listo');
        this.cerrarDispensarOrden();
        this.cargarOrdenesPagina(this.ordenesPageNumber());
        this.recargar();
        this.guardando.set(false);
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error.set(err?.error?.error || 'Error al dispensar');
        this.toast.error(this.error()!, 'Error');
        this.guardando.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  crearMedicamento(): void {
    if (!this.medForm.nombre.trim()) {
      this.error.set('Nombre del medicamento es obligatorio');
      this.cdr.markForCheck();
      return;
    }
    this.error.set(null);
    const cb = this.medForm.codigoBarras?.trim();
    this.farmaciaService
      .createMedicamento({
        nombre: this.medForm.nombre.trim(),
        lote: this.medForm.lote || undefined,
        codigoBarras: cb || undefined,
        fechaVencimiento: this.medForm.fechaVencimiento || undefined,
        cantidad: this.medForm.cantidad ? Number(this.medForm.cantidad) : 0,
        precio: this.medForm.precio ? Number(this.medForm.precio) : undefined,
        stockMinimo: this.medForm.stockMinimo ? Number(this.medForm.stockMinimo) : 0,
        activo: true,
      })
      .subscribe({
        next: () => {
          this.medForm = {
            nombre: '',
            lote: '',
            codigoBarras: '',
            fechaVencimiento: '',
            cantidad: '',
            precio: '',
            stockMinimo: '',
          };
          this.toast.success('Medicamento creado en el inventario.', 'Medicamento creado');
          this.recargar();
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.error.set(err?.error?.error || 'No se pudo crear medicamento');
          this.toast.error(this.error()!, 'Error');
          this.cdr.markForCheck();
        },
      });
  }

  /** Lector en campo código al crear: Enter resuelve y sugiere nombre si existe */
  onCodigoBarrasInventarioKeydown(ev: KeyboardEvent): void {
    if (ev.key !== 'Enter') return;
    const v = this.medForm.codigoBarras?.trim() ?? '';
    if (!this.esProbableCodigoBarras(v)) return;
    ev.preventDefault();
    this.farmaciaService.medicamentoPorCodigo(v).subscribe({
      next: (m) => {
        if (m) {
          this.toast.info(`Ya existe "${m.nombre}" con este código. No duplique si es el mismo ítem.`, 'Código existente');
        }
        this.cdr.markForCheck();
      },
      error: () => {},
    });
  }

  setStockMinimoRapido(n: number): void {
    this.medForm.stockMinimo = String(n);
    this.cdr.markForCheck();
  }

  dispensar(): void {
    if (!this.dispForm.medicamentoId || !this.dispForm.pacienteId || !this.dispForm.cantidad) {
      this.error.set('Selecciona medicamento, paciente y cantidad');
      this.toast.warning(this.error()!, 'Dispensación');
      this.cdr.markForCheck();
      return;
    }
    this.error.set(null);
    this.farmaciaService
      .dispensar({
        medicamentoId: this.dispForm.medicamentoId,
        pacienteId: this.dispForm.pacienteId,
        cantidad: Number(this.dispForm.cantidad),
        entregadoPor: this.dispForm.entregadoPor || undefined,
      })
      .subscribe({
        next: (d) => {
          this.dispensaciones.update((list) => [d, ...list]);
          this.dispForm = {
            medicamentoId: null,
            pacienteId: null,
            cantidad: '',
            entregadoPor: '',
          };
          this.dispMedQuery.set('');
          this.showDropMed.set(false);
          this.showDropPac.set(false);
          this.dispPacQuery.set('');
          this.dispPacResumen.set('');
          this.toast.success('Medicamento dispensado correctamente.', 'Dispensación exitosa');
          this.recargar();
          this.scheduleMedDispSearch(true);
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.error.set(err?.error?.error || 'No se pudo dispensar');
          this.toast.error(this.error()!, 'Error al dispensar');
          this.cdr.markForCheck();
        },
      });
  }

  setCantidadDirecta(n: number): void {
    this.dispForm.cantidad = String(Math.max(1, n));
    this.cdr.markForCheck();
  }

  ajustarCantidadDirecta(delta: number): void {
    const cur = Number(this.dispForm.cantidad) || 1;
    this.dispForm.cantidad = String(Math.max(1, cur + delta));
    this.cdr.markForCheck();
  }

  scheduleMedDispSearch(immediate: boolean): void {
    clearTimeout(this.medDispSearchTimer);
    const run = () => {
      this.farmaciaService
        .listMedicamentos({
          page: 0,
          size: 40,
          q: this.dispMedQuery(),
          soloStock: true,
        })
        .subscribe({
          next: (pg) => {
            this.medsDispResults.set(pg.content);
            this.cdr.markForCheck();
          },
          error: () => {
            this.medsDispResults.set([]);
            this.cdr.markForCheck();
          },
        });
    };
    if (immediate) run();
    else this.medDispSearchTimer = setTimeout(run, 300);
  }

  seleccionarMedDirecto(m: FarmaciaMedicamentoDto): void {
    this.dispForm.medicamentoId = m.id;
    this.dispMedQuery.set(m.nombre);
    this.showDropMed.set(false);
    this.cdr.markForCheck();
  }

  nombreMedSeleccionado(): string {
    const id = this.dispForm.medicamentoId;
    if (!id) return '';
    return (
      this.medsDispResults().find((m) => m.id === id)?.nombre ??
      this.catalogoStock().find((m) => m.id === id)?.nombre ??
      ''
    );
  }

  onMedInputDirecto(v: string): void {
    this.dispMedQuery.set(v);
    this.dispForm.medicamentoId = null;
    this.showDropMed.set(true);
    this.scheduleMedDispSearch(false);
    this.cdr.markForCheck();
  }

  onMedicamentoDirectoKeydown(ev: KeyboardEvent): void {
    if (ev.key !== 'Enter') return;
    const v = this.dispMedQuery().trim();
    if (!this.esProbableCodigoBarras(v)) return;
    ev.preventDefault();
    this.farmaciaService.medicamentoPorCodigo(v).subscribe({
      next: (m) => {
        if (m) {
          if (m.cantidad <= 0) {
            this.toast.warning('Medicamento sin stock.', 'Código');
            return;
          }
          this.seleccionarMedDirecto(m);
          this.toast.success(m.nombre, 'Código reconocido');
        } else {
          this.toast.warning('Código no registrado en inventario activo.', 'Lector');
        }
        this.cdr.markForCheck();
      },
      error: () => this.toast.error('Error al buscar código', 'Lector'),
    });
  }

  esProbableCodigoBarras(s: string): boolean {
    const t = s.trim();
    if (t.length < 8 || t.length > 64) return false;
    return /^[0-9A-Za-z\-]+$/.test(t) && /\d/.test(t);
  }

  onScanOrdenPanelKeydown(ev: KeyboardEvent): void {
    if (ev.key !== 'Enter') return;
    ev.preventDefault();
    const input = ev.target as HTMLInputElement;
    const codigo = input.value.trim();
    if (!codigo) return;
    this.farmaciaService.medicamentoPorCodigo(codigo).subscribe({
      next: (m) => {
        if (!m) {
          this.toast.warning('Código no encontrado', 'Escaneo');
          input.value = '';
          this.cdr.markForCheck();
          return;
        }
        if (m.cantidad <= 0) {
          this.toast.warning('Sin stock: ' + m.nombre, 'Escaneo');
          input.value = '';
          this.cdr.markForCheck();
          return;
        }
        const idx = this.lineasDispensacion().findIndex((l) => l.medicamentoId === 0);
        if (idx < 0) {
          this.toast.info('Todas las líneas tienen medicamento asignado.', 'Escaneo');
          input.value = '';
          this.cdr.markForCheck();
          return;
        }
        this.setLineaMedicamento(idx, m.id);
        this.toast.success(`Línea ${idx + 1}: ${m.nombre}`, 'Escaneo');
        input.value = '';
        this.cdr.markForCheck();
      },
      error: () => this.toast.error('Error al buscar código', 'Escaneo'),
    });
  }

  seleccionarPacDirecto(p: PacienteDto): void {
    this.dispForm.pacienteId = p.id;
    const etiqueta = `${p.nombres} ${p.apellidos ?? ''} — ${p.documento ?? ''}`.trim();
    this.dispPacResumen.set(etiqueta);
    this.dispPacQuery.set(etiqueta);
    this.showDropPac.set(false);
    this.cdr.markForCheck();
  }

  onPacInputDirecto(v: string): void {
    this.dispPacQuery.set(v);
    this.dispForm.pacienteId = null;
    this.dispPacResumen.set('');
    this.showDropPac.set(true);
    this.schedulePacDropdownSearch(false);
    this.cdr.markForCheck();
  }

  onPacFocusDirecto(): void {
    this.showDropPac.set(true);
    if (this.pacDropdownRows().length === 0 && !this.dispPacQuery().trim()) {
      this.cargarPacientesDropdown('');
    }
  }

  schedulePacDropdownSearch(immediate: boolean): void {
    clearTimeout(this.pacSearchTimer);
    const run = () => this.cargarPacientesDropdown(this.dispPacQuery().trim());
    if (immediate) run();
    else this.pacSearchTimer = setTimeout(run, 280);
  }

  private cargarPacientesDropdown(q: string): void {
    this.pacDropdownLoading.set(true);
    this.pacienteService.list(0, 25, q || undefined).subscribe({
      next: (res) => {
        this.pacDropdownRows.set(res.content ?? []);
        this.pacDropdownLoading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        this.pacDropdownRows.set([]);
        this.pacDropdownLoading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  blurMedDirecto(): void {
    setTimeout(() => {
      this.showDropMed.set(false);
      this.cdr.markForCheck();
    }, 180);
  }

  blurPacDirecto(): void {
    setTimeout(() => {
      this.showDropPac.set(false);
      this.cdr.markForCheck();
    }, 180);
  }

  nombrePacSeleccionado(): string {
    if (!this.dispForm.pacienteId) return '';
    const r = this.dispPacResumen().trim();
    if (r) return r;
    const p = this.pacDropdownRows().find((x) => x.id === this.dispForm.pacienteId);
    return p ? `${p.nombres} ${p.apellidos ?? ''}`.trim() : '';
  }

  estadoDispensacionLabel(estado: string): string {
    if (!estado || estado === 'PENDIENTE') return 'Pendiente';
    if (estado === 'PARCIAL') return 'Parcial';
    if (estado === 'COMPLETADA') return 'Completada';
    if (estado === 'CANCELADA') return 'Cancelada';
    return estado;
  }

  trackOrden(_: number, o: OrdenFarmaciaPendienteDto): number {
    return o.id;
  }

  trackMed(_: number, m: FarmaciaMedicamentoDto): number {
    return m.id;
  }

  trackPac(_: number, p: PacienteDto): number {
    return p.id;
  }
}
