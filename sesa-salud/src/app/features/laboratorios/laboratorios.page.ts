/**
 * Laboratorios — Workbench profesional del Bacteriólogo.
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  LaboratorioSolicitudService,
  LaboratorioSolicitudDto,
  OrdenClinicaLabDto,
  LaboratorioSolicitudRequestDto,
} from '../../core/services/laboratorio-solicitud.service';
import { OrdenClinicaService } from '../../core/services/orden-clinica.service';
import { PacienteService, PacienteDto } from '../../core/services/paciente.service';
import { AuthService } from '../../core/services/auth.service';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';
import {
  getPlantillaParaPrueba,
  serializarResultadoPlantilla,
  type PlantillaLab,
} from './lab-plantillas';
import { parseResultadoToItems } from '../../core/utils/resultado-display.util';

export type TabActiva = 'solicitudes' | 'ordenes-hc';
export type FiltroEstado = 'TODOS' | 'PENDIENTE' | 'EN_PROCESO' | 'COMPLETADO';

export interface ItemUnificado {
  _tipo: 'solicitud' | 'orden';
  id: number;
  pacienteId: number;
  pacienteNombre: string;
  pacienteDocumento?: string;
  tipoPrueba: string;
  estado: string;
  fecha: string;
  solicitanteNombre?: string;
  resultado?: string;
  observaciones?: string;
  bacteriologoNombre?: string;
  fechaResultado?: string;
  resultadoRegistradoPorNombre?: string;
  resultadoRegistradoPorRol?: string;
  raw?: LaboratorioSolicitudDto | OrdenClinicaLabDto;
}

@Component({
  standalone: true,
  selector: 'sesa-laboratorios-page',
  imports: [CommonModule, FormsModule],
  templateUrl: './laboratorios.page.html',
  styleUrl: './laboratorios.page.scss',
})
export class LaboratoriosPageComponent implements OnInit {
  /** Expuesto para la plantilla: resultado por ítem (etiqueta en negrita + valor). */
  protected readonly parseResultadoToItems = parseResultadoToItems;

  private readonly labService = inject(LaboratorioSolicitudService);
  private readonly ordenService = inject(OrdenClinicaService);
  private readonly pacienteService = inject(PacienteService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(SesaToastService);

  // --- estado ---
  readonly tabActiva = signal<TabActiva>('solicitudes');
  readonly filtroEstado = signal<FiltroEstado>('TODOS');
  readonly busqueda = signal('');
  readonly cargando = signal(false);
  readonly guardando = signal(false);
  readonly mostrarFormNuevo = signal(false);

  readonly solicitudes = signal<LaboratorioSolicitudDto[]>([]);
  readonly ordenesHC = signal<OrdenClinicaLabDto[]>([]);
  readonly itemSeleccionado = signal<ItemUnificado | null>(null);

  // form resultado (resultado/observaciones para texto libre; templateValues cuando hay plantilla)
  readonly formResultado = signal<{
    resultado: string;
    observaciones: string;
    templateValues: Record<string, string>;
  }>({ resultado: '', observaciones: '', templateValues: {} });

  // form nueva solicitud
  readonly formNuevo = signal<{ pacienteQuery: string; tipoPrueba: string; pacienteId: number | null; pacienteOpciones: PacienteDto[]; buscandoPaciente: boolean }>({
    pacienteQuery: '',
    tipoPrueba: '',
    pacienteId: null,
    pacienteOpciones: [],
    buscandoPaciente: false,
  });

  // --- computed stats ---
  readonly itemsActivos = computed<ItemUnificado[]>(() => {
    const base =
      this.tabActiva() === 'solicitudes'
        ? this.solicitudes().map((s) => this.solicitudAItem(s))
        : this.ordenesHC().map((o) => this.ordenAItem(o));

    const q = this.busqueda().toLowerCase().trim();
    const est = this.filtroEstado();
    return base.filter((item) => {
      const matchEstado = est === 'TODOS' || item.estado === est;
      const matchBusq =
        !q ||
        item.pacienteNombre.toLowerCase().includes(q) ||
        item.tipoPrueba.toLowerCase().includes(q) ||
        (item.pacienteDocumento ?? '').includes(q);
      return matchEstado && matchBusq;
    });
  });

  readonly statTotal = computed(() =>
    this.tabActiva() === 'solicitudes' ? this.solicitudes().length : this.ordenesHC().length
  );
  readonly statPendiente = computed(() =>
    this.tabActiva() === 'solicitudes'
      ? this.solicitudes().filter((s) => s.estado === 'PENDIENTE').length
      : this.ordenesHC().filter((o) => (o.estado ?? '').toUpperCase() === 'PENDIENTE').length
  );
  readonly statEnProceso = computed(() =>
    this.tabActiva() === 'solicitudes'
      ? this.solicitudes().filter((s) => s.estado === 'EN_PROCESO').length
      : this.ordenesHC().filter((o) => (o.estado ?? '').toUpperCase() === 'EN_PROCESO').length
  );
  readonly statCompletado = computed(() =>
    this.tabActiva() === 'solicitudes'
      ? this.solicitudes().filter((s) => s.estado === 'COMPLETADO').length
      : this.ordenesHC().filter((o) => {
          const e = (o.estado ?? '').toUpperCase();
          return e === 'COMPLETADO' || e === 'COMPLETADA';
        }).length
  );

  // tipos de prueba comunes en Colombia
  readonly tiposPrueba = [
    'Hemograma completo (CBC)',
    'Perfil lipídico',
    'Glicemia en ayunas',
    'Creatinina sérica',
    'Uroanálisis',
    'Urocultivo',
    'Coprocultivo',
    'Prueba de embarazo (bhCG)',
    'TSH / T4 libre',
    'Transaminasas (AST/ALT)',
    'Antígeno prostático (PSA)',
    'PCR / VSG',
    'INR / Tiempo de protrombina',
    'Hemoglobina glicosilada (HbA1c)',
    'Nitrógeno ureico (BUN)',
    'Bilirrubinas totales y fraccionadas',
    'Serología VIH (ELISA)',
    'Antígeno de superficie hepatitis B',
    'VDRL / RPR',
    'Frotis de sangre periférica',
    'Cultivo de herida / secreción',
    'Antibiograma',
    'Extendido de esputo – BK',
    'Proteína C reactiva (PCR cuantitativa)',
    'Electrolitos séricos (Na / K / Cl)',
  ];

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.cargando.set(true);
    this.labService.list().subscribe({
      next: (res) => {
        this.solicitudes.set(res?.content ?? []);
        this.cargando.set(false);
      },
      error: () => {
        this.toast.error('No se pudieron cargar las solicitudes', 'Error');
        this.cargando.set(false);
      },
    });
    this.labService.getOrdenesLab().subscribe({
      next: (ordenes) => this.ordenesHC.set(ordenes ?? []),
      error: () => { /* silencioso — puede no tener permisos */ },
    });
  }

  seleccionarItem(item: ItemUnificado): void {
    this.itemSeleccionado.set(item);
    const plantilla = getPlantillaParaPrueba(item.tipoPrueba);
    const templateValues: Record<string, string> = {};
    if (plantilla) {
      for (const c of plantilla.campos) templateValues[c.id] = '';
    }
    this.formResultado.set({
      resultado: item.resultado ?? '',
      observaciones: item.observaciones ?? '',
      templateValues,
    });
  }

  cerrarPanel(): void {
    this.itemSeleccionado.set(null);
  }

  cambiarEstado(estado: string): void {
    const item = this.itemSeleccionado();
    if (!item || item._tipo !== 'solicitud') return;
    this.guardando.set(true);
    this.labService.cambiarEstado(item.id, estado).subscribe({
      next: (updated) => {
        this.actualizarSolicitudEnLista(updated);
        this.itemSeleccionado.set({ ...item, estado });
        this.guardando.set(false);
        this.toast.success(`Estado actualizado a ${this.etiquetaEstado(estado)}`, 'Laboratorios');
      },
      error: () => {
        this.toast.error('No se pudo actualizar el estado', 'Error');
        this.guardando.set(false);
      },
    });
  }

  /** Obtiene la plantilla para el ítem seleccionado (según tipo de prueba). */
  getPlantilla(item: ItemUnificado | null): PlantillaLab | null {
    return item ? getPlantillaParaPrueba(item.tipoPrueba) : null;
  }

  setTemplateValue(campoId: string, value: string): void {
    this.formResultado.update((f) => ({
      ...f,
      templateValues: { ...f.templateValues, [campoId]: value },
    }));
  }

  /** Resultado listo para enviar: texto libre o serializado desde plantilla. */
  getResultadoParaGuardar(): string {
    const form = this.formResultado();
    const item = this.itemSeleccionado();
    const plantilla = item ? getPlantillaParaPrueba(item.tipoPrueba) : null;
    if (plantilla && Object.keys(form.templateValues).length > 0) {
      const texto = serializarResultadoPlantilla(plantilla, form.templateValues);
      if (texto.trim()) return texto;
    }
    return form.resultado.trim();
  }

  tieneResultadoValido(): boolean {
    return this.getResultadoParaGuardar().length > 0;
  }

  guardarResultado(): void {
    const item = this.itemSeleccionado();
    if (!item) return;
    const resultadoTexto = this.getResultadoParaGuardar();
    if (!resultadoTexto) {
      this.toast.error('Complete al menos el resultado o los campos de la plantilla.', 'Validación');
      return;
    }
    this.guardando.set(true);
    if (item._tipo === 'solicitud') {
      const personalId = this.auth.currentUser()?.userId ?? undefined;
      this.labService
        .registrarResultado(item.id, {
          resultado: resultadoTexto,
          observaciones: this.formResultado().observaciones,
          bacteriologoId: personalId,
        })
        .subscribe({
          next: (updated) => {
            this.actualizarSolicitudEnLista(updated);
            this.itemSeleccionado.set(this.solicitudAItem(updated));
            this.guardando.set(false);
            this.toast.success('Resultado registrado correctamente', 'Laboratorios');
          },
          error: () => {
            this.toast.error('No se pudo guardar el resultado', 'Error');
            this.guardando.set(false);
          },
        });
    } else {
      this.ordenService.registrarResultado(item.id, resultadoTexto).subscribe({
        next: (updated) => {
          const asLab = { ...updated } as OrdenClinicaLabDto;
          this.actualizarOrdenHCEnLista(asLab);
          this.itemSeleccionado.set(this.ordenAItem(asLab));
          this.guardando.set(false);
          this.toast.success('Resultado asociado a la orden de HC. Se verá en la Historia Clínica del paciente.', 'Laboratorios');
        },
        error: () => {
          this.toast.error('No se pudo guardar el resultado en la orden', 'Error');
          this.guardando.set(false);
        },
      });
    }
  }

  buscarPaciente(): void {
    const q = this.formNuevo().pacienteQuery.trim();
    if (q.length < 2) return;
    this.formNuevo.update((f) => ({ ...f, buscandoPaciente: true }));
    this.pacienteService.list(0, 10, q).subscribe({
      next: (res) => {
        const opciones = res?.content ?? [];
        this.formNuevo.update((f) => ({ ...f, pacienteOpciones: opciones, buscandoPaciente: false }));
      },
      error: () => this.formNuevo.update((f) => ({ ...f, buscandoPaciente: false })),
    });
  }

  seleccionarPaciente(p: PacienteDto): void {
    this.formNuevo.update((f) => ({
      ...f,
      pacienteId: p.id,
      pacienteQuery: `${p.nombres} ${p.apellidos ?? ''} — ${p.documento}`.trim(),
      pacienteOpciones: [],
    }));
  }

  crearSolicitud(): void {
    const form = this.formNuevo();
    if (!form.pacienteId || !form.tipoPrueba.trim()) {
      this.toast.error('Paciente y tipo de prueba son obligatorios', 'Validación');
      return;
    }
    this.guardando.set(true);
    const dto: LaboratorioSolicitudRequestDto = {
      pacienteId: form.pacienteId,
      tipoPrueba: form.tipoPrueba,
    };
    this.labService.crear(dto).subscribe({
      next: (nueva) => {
        this.solicitudes.update((list) => [nueva, ...list]);
        this.mostrarFormNuevo.set(false);
        this.formNuevo.set({ pacienteQuery: '', tipoPrueba: '', pacienteId: null, pacienteOpciones: [], buscandoPaciente: false });
        this.guardando.set(false);
        this.toast.success('Solicitud creada correctamente', 'Laboratorios');
      },
      error: () => {
        this.toast.error('No se pudo crear la solicitud', 'Error');
        this.guardando.set(false);
      },
    });
  }

  // --- helpers UX ---
  estadoClass(estado: string): string {
    switch (estado?.toUpperCase()) {
      case 'PENDIENTE': return 'badge-pendiente';
      case 'EN_PROCESO': return 'badge-en-proceso';
      case 'COMPLETADO':
      case 'COMPLETADA': return 'badge-completado';
      case 'RECHAZADO': return 'badge-rechazado';
      default: return 'badge-pendiente';
    }
  }

  etiquetaEstado(estado: string): string {
    switch (estado?.toUpperCase()) {
      case 'PENDIENTE': return 'Pendiente';
      case 'EN_PROCESO': return 'En proceso';
      case 'COMPLETADO':
      case 'COMPLETADA': return 'Completado';
      case 'RECHAZADO': return 'Rechazado';
      default: return estado ?? 'Pendiente';
    }
  }

  iniciales(nombre: string): string {
    if (!nombre) return '?';
    const parts = nombre.trim().split(/\s+/);
    if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase();
    return nombre[0].toUpperCase();
  }

  avatarStyle(nombre: string): { [k: string]: string } {
    const colores = [
      '#0ea5e9', '#8b5cf6', '#f59e0b', '#10b981',
      '#ef4444', '#ec4899', '#14b8a6', '#f97316',
    ];
    let hash = 0;
    for (const c of nombre) hash = (hash * 31 + c.charCodeAt(0)) & 0xffffffff;
    return { 'background-color': colores[Math.abs(hash) % colores.length] };
  }

  setResultado(val: string): void {
    this.formResultado.update((f) => ({ ...f, resultado: val }));
  }

  setObservaciones(val: string): void {
    this.formResultado.update((f) => ({ ...f, observaciones: val }));
  }

  setTipoPrueba(val: string): void {
    this.formNuevo.update((f) => ({ ...f, tipoPrueba: val }));
  }

  setPacienteQuery(val: string): void {
    this.formNuevo.update((f) => ({ ...f, pacienteQuery: val, pacienteId: null }));
    this.buscarPaciente();
  }

  formatFecha(fecha?: string): string {
    if (!fecha) return '--/--/----';
    const f = new Date(fecha);
    if (Number.isNaN(f.getTime())) return '--/--/----';
    return f.toLocaleDateString('es-CO', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  setTab(tab: TabActiva): void {
    this.tabActiva.set(tab);
    this.itemSeleccionado.set(null);
    this.busqueda.set('');
    this.filtroEstado.set('TODOS');
  }

  setFiltro(f: FiltroEstado): void {
    this.filtroEstado.set(f);
  }

  puedeEditarEstado(item: ItemUnificado): boolean {
    return item._tipo === 'solicitud' && item.estado !== 'COMPLETADO';
  }

  // --- privados ---
  private solicitudAItem(s: LaboratorioSolicitudDto): ItemUnificado {
    return {
      _tipo: 'solicitud',
      id: s.id,
      pacienteId: s.pacienteId,
      pacienteNombre: s.pacienteNombre,
      pacienteDocumento: s.pacienteDocumento,
      tipoPrueba: s.tipoPrueba,
      estado: s.estado,
      fecha: s.fechaSolicitud,
      solicitanteNombre: s.solicitanteNombre,
      resultado: s.resultado,
      observaciones: s.observaciones,
      bacteriologoNombre: s.bacteriologoNombre,
      fechaResultado: s.fechaResultado,
      raw: s,
    };
  }

  private ordenAItem(o: OrdenClinicaLabDto): ItemUnificado {
    return {
      _tipo: 'orden',
      id: o.id,
      pacienteId: o.pacienteId,
      pacienteNombre: o.pacienteNombre,
      tipoPrueba: o.detalle ?? o.tipo,
      estado: o.estado ?? 'PENDIENTE',
      fecha: o.createdAt ?? '',
      resultado: o.resultado,
      fechaResultado: o.fechaResultado,
      resultadoRegistradoPorNombre: o.resultadoRegistradoPorNombre,
      resultadoRegistradoPorRol: o.resultadoRegistradoPorRol,
      raw: o,
    };
  }

  private actualizarOrdenHCEnLista(updated: OrdenClinicaLabDto): void {
    this.ordenesHC.update((list) =>
      list.map((o) => (o.id === updated.id ? updated : o))
    );
  }

  private actualizarSolicitudEnLista(updated: LaboratorioSolicitudDto): void {
    this.solicitudes.update((list) =>
      list.map((s) => (s.id === updated.id ? updated : s))
    );
  }
}
