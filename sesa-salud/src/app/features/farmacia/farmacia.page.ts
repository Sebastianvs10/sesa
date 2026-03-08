/**
 * Farmacia — Órdenes médicas (HC), inventario y dispensación.
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';
import {
  FarmaciaService,
  FarmaciaMedicamentoDto,
  FarmaciaDispensacionDto,
  OrdenFarmaciaPendienteDto,
  OrdenFarmaciaPendienteItemDto,
  LineaDispensacionDto,
} from '../../core/services/farmacia.service';
import { PacienteDto, PacienteService } from '../../core/services/paciente.service';

type TabFarmacia = 'ordenes' | 'inventario' | 'dispensar';

@Component({
  standalone: true,
  selector: 'sesa-farmacia-page',
  imports: [CommonModule, FormsModule, SesaCardComponent],
  templateUrl: './farmacia.page.html',
  styleUrl: './farmacia.page.scss',
})
export class FarmaciaPageComponent implements OnInit {
  private readonly farmaciaService = inject(FarmaciaService);
  private readonly pacienteService = inject(PacienteService);
  private readonly toast = inject(SesaToastService);

  readonly tabActiva = signal<TabFarmacia>('ordenes');
  readonly ordenesPendientes = signal<OrdenFarmaciaPendienteDto[]>([]);
  readonly medicamentos = signal<FarmaciaMedicamentoDto[]>([]);
  readonly dispensaciones = signal<FarmaciaDispensacionDto[]>([]);
  readonly cargandoOrdenes = signal(false);
  readonly guardando = signal(false);
  error = signal<string | null>(null);

  pacientes: PacienteDto[] = [];

  /** Orden seleccionada para dispensar (desde Órdenes médicas) */
  readonly ordenSeleccionada = signal<OrdenFarmaciaPendienteDto | null>(null);
  /** Líneas a dispensar para la orden seleccionada */
  readonly lineasDispensacion = signal<LineaDispensacionDto[]>([]);

  medForm = {
    nombre: '',
    lote: '',
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

  readonly medicamentosConStock = computed(() =>
    this.medicamentos().filter((m) => m.cantidad > 0)
  );

  /** Medicamentos con cantidad ≤ stock mínimo (alerta de reposición). */
  readonly medicamentosStockBajo = computed(() =>
    this.medicamentos().filter((m) => m.stockMinimo != null && m.cantidad <= m.stockMinimo)
  );

  /** Medicamentos que vencen en los próximos 30 días (alerta de vencimiento). */
  readonly medicamentosProximosVencer = computed(() => {
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    const en30 = new Date(hoy);
    en30.setDate(en30.getDate() + 30);
    return this.medicamentos().filter((m) => {
      if (!m.fechaVencimiento?.trim()) return false;
      const v = new Date(m.fechaVencimiento.trim());
      if (isNaN(v.getTime())) return false;
      v.setHours(0, 0, 0, 0);
      return v >= hoy && v <= en30;
    });
  });

  /** Texto para alerta de stock bajo (nombres separados por coma). */
  readonly medicamentosStockBajoNombres = computed(() =>
    this.medicamentosStockBajo().map((m) => m.nombre).join(', ')
  );

  /** Texto para alerta de vencimiento (nombre + fecha). */
  readonly medicamentosProximosVencerNombres = computed(() =>
    this.medicamentosProximosVencer().map((m) => m.nombre + ' (' + (m.fechaVencimiento || '') + ')').join(', ')
  );

  ngOnInit(): void {
    this.recargar();
    this.cargarOrdenesPendientes();
    this.pacienteService.list(0, 300).subscribe({ next: (res) => (this.pacientes = res.content ?? []) });
  }

  setTab(tab: TabFarmacia): void {
    this.tabActiva.set(tab);
    if (tab === 'ordenes') this.cargarOrdenesPendientes();
    if (tab === 'inventario') this.recargar();
  }

  cargarOrdenesPendientes(): void {
    this.cargandoOrdenes.set(true);
    this.error.set(null);
    this.farmaciaService.getOrdenesPendientes().subscribe({
      next: (list) => {
        this.ordenesPendientes.set(list ?? []);
        this.cargandoOrdenes.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.error || 'No se pudieron cargar las órdenes');
        this.toast.error(this.error()!, 'Error');
        this.cargandoOrdenes.set(false);
      },
    });
  }

  recargar(): void {
    this.farmaciaService.listMedicamentos().subscribe({
      next: (res) => this.medicamentos.set(res ?? []),
      error: (err) => {
        this.error.set(err?.error?.error || 'No se pudo cargar inventario');
        this.toast.error(this.error()!, 'Error de carga');
      },
    });
  }

  /** Texto resumido de prescripción: cantidad + unidad + frecuencia + duración (para mostrar en lista y detalle). */
  formatoPrescripcion(orden: OrdenFarmaciaPendienteDto): string {
    const parts: string[] = [];
    if (orden.cantidadPrescrita != null) {
      parts.push(orden.unidadMedida ? `${orden.cantidadPrescrita} ${orden.unidadMedida}` : `${orden.cantidadPrescrita}`);
    }
    if (orden.frecuencia?.trim()) parts.push(orden.frecuencia.trim());
    if (orden.duracionDias != null) parts.push(`${orden.duracionDias} días`);
    return parts.join(' · ');
  }

  /** Prescripción para un ítem (órdenes con varios medicamentos). */
  formatoPrescripcionItem(item: OrdenFarmaciaPendienteItemDto): string {
    const parts: string[] = [];
    if (item.cantidadPrescrita != null) {
      parts.push(item.unidadMedida ? `${item.cantidadPrescrita} ${item.unidadMedida}` : `${item.cantidadPrescrita}`);
    }
    if (item.frecuencia?.trim()) parts.push(item.frecuencia.trim());
    if (item.duracionDias != null) parts.push(`${item.duracionDias} días`);
    return parts.join(' · ');
  }

  /** Si la orden tiene varios ítems de medicamento (compuesta). */
  ordenConVariosItems(orden: OrdenFarmaciaPendienteDto): boolean {
    return !!orden.items && orden.items.length > 1;
  }

  /**
   * Busca en el inventario un medicamento cuyo nombre coincida con el detalle prescrito
   * (ej. detalle "Medicamento indicado: Azitromicina 500 mg" → coincide con nombre "Azitromicina 500 mg").
   */
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

  abrirDispensarOrden(orden: OrdenFarmaciaPendienteDto): void {
    this.ordenSeleccionada.set(orden);
    if (orden.items && orden.items.length > 0) {
      const lineas: LineaDispensacionDto[] = orden.items.map((it) => {
        const medicamentoId = this.buscarMedicamentoPorDetalle(it.detalle);
        const cantidad = it.cantidadPrescrita != null && it.cantidadPrescrita > 0 ? it.cantidadPrescrita : 1;
        return { medicamentoId: medicamentoId || 0, cantidad };
      });
      this.lineasDispensacion.set(lineas);
    } else {
      const cantidadInicial = orden.cantidadPrescrita != null && orden.cantidadPrescrita > 0
        ? orden.cantidadPrescrita
        : 1;
      const medicamentoId = this.buscarMedicamentoPorDetalle(orden.detalle);
      this.lineasDispensacion.set([{ medicamentoId: medicamentoId || 0, cantidad: cantidadInicial }]);
    }
  }

  cerrarDispensarOrden(): void {
    this.ordenSeleccionada.set(null);
    this.lineasDispensacion.set([]);
  }

  agregarLineaDispensacion(): void {
    this.lineasDispensacion.update((l) => [...l, { medicamentoId: 0, cantidad: 1 }]);
  }

  quitarLineaDispensacion(index: number): void {
    this.lineasDispensacion.update((l) => l.filter((_, i) => i !== index));
  }

  setLineaMedicamento(index: number, medicamentoId: number): void {
    this.lineasDispensacion.update((l) => {
      const c = [...l];
      c[index] = { ...c[index], medicamentoId, cantidad: c[index].cantidad ?? 1 };
      return c;
    });
  }

  setLineaCantidad(index: number, cantidad: number | string): void {
    const n = typeof cantidad === 'string' ? parseInt(cantidad, 10) : cantidad;
    this.lineasDispensacion.update((l) => {
      const c = [...l];
      c[index] = { ...c[index], cantidad: isNaN(n) ? 0 : n };
      return c;
    });
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
        this.cargarOrdenesPendientes();
        this.recargar();
        this.guardando.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.error || 'Error al dispensar');
        this.toast.error(this.error()!, 'Error');
        this.guardando.set(false);
      },
    });
  }

  crearMedicamento(): void {
    if (!this.medForm.nombre.trim()) {
      this.error.set('Nombre del medicamento es obligatorio');
      return;
    }
    this.error.set(null);
    this.farmaciaService
      .createMedicamento({
        nombre: this.medForm.nombre.trim(),
        lote: this.medForm.lote || undefined,
        fechaVencimiento: this.medForm.fechaVencimiento || undefined,
        cantidad: this.medForm.cantidad ? Number(this.medForm.cantidad) : 0,
        precio: this.medForm.precio ? Number(this.medForm.precio) : undefined,
        stockMinimo: this.medForm.stockMinimo ? Number(this.medForm.stockMinimo) : 0,
        activo: true,
      })
      .subscribe({
        next: () => {
          this.medForm = { nombre: '', lote: '', fechaVencimiento: '', cantidad: '', precio: '', stockMinimo: '' };
          this.toast.success('Medicamento creado en el inventario.', 'Medicamento creado');
          this.recargar();
        },
        error: (err) => {
          this.error.set(err?.error?.error || 'No se pudo crear medicamento');
          this.toast.error(this.error()!, 'Error');
        },
      });
  }

  dispensar(): void {
    if (!this.dispForm.medicamentoId || !this.dispForm.pacienteId || !this.dispForm.cantidad) {
      this.error.set('Selecciona medicamento, paciente y cantidad');
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
          this.dispForm = { medicamentoId: null, pacienteId: null, cantidad: '', entregadoPor: '' };
          this.toast.success('Medicamento dispensado correctamente.', 'Dispensación exitosa');
          this.recargar();
        },
        error: (err) => {
          this.error.set(err?.error?.error || 'No se pudo dispensar');
          this.toast.error(this.error()!, 'Error al dispensar');
        },
      });
  }

  estadoDispensacionLabel(estado: string): string {
    if (!estado || estado === 'PENDIENTE') return 'Pendiente';
    if (estado === 'PARCIAL') return 'Parcial';
    if (estado === 'COMPLETADA') return 'Completada';
    if (estado === 'CANCELADA') return 'Cancelada';
    return estado;
  }
}
