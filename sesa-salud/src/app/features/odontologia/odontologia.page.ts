/**
 * OdontologiaPageComponent — Módulo completo de Odontología. v2
 * Dashboard, lista de citas, historia clínica, odontograma, procedimientos,
 * plan de tratamiento, imágenes y evolución.
 * Autor: Ing. J Sebastian Vargas S
 */

import {
  Component, OnInit, inject, signal, computed, ViewChild, ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  faTooth, faUser, faCalendarCheck, faClipboardList, faFileInvoiceDollar,
  faChartLine, faImages, faNotesMedical, faPlus, faSave, faPrint,
  faArrowLeft, faSearch, faChevronLeft, faChevronRight,
  faSpinner, faCircleCheck, faCircleXmark, faTrash, faEdit, faEye,
  faUpload, faMoneyBillWave, faStethoscope, faShieldHalved,
} from '@fortawesome/free-solid-svg-icons';
import { OdontologiaService } from '../../core/services/odontologia.service';
import { CitaService, ConsultaMedicaDto } from '../../core/services/cita.service';
import { AuthService } from '../../core/services/auth.service';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';
import { OdontogramaComponent, OdontogramaCambio } from './odontograma/odontograma.component';
import {
  ConsultaOdontologicaDto, PiezaDental, PlanTratamientoDto, PlanTratamientoItemDto,
  ProcedimientoCatalogo, ImagenClinicaDto, EvolucionOdontologicaDto,
  OdontogramaEstadoDto, dtoListToPiezas, piezasChangedToDtos,
  ESTADO_LABEL, ESTADO_COLOR,
} from './odontologia.models';

type TabPrincipal = 'dashboard' | 'paciente';
type TabFicha = 'historia' | 'odontograma' | 'procedimientos' | 'plan' | 'imagenes' | 'evolucion' | 'historial';

/**
 * Estados clínicos completos de una cita odontológica.
 * Flujo: AGENDADA → EN_ESPERA → EN_ATENCION → ATENDIDA
 */
type EstadoCita =
  | 'AGENDADA'       // programada, sin confirmar
  | 'CONFIRMADA'     // paciente confirmó asistencia
  | 'EN_ESPERA'      // paciente llegó, sala de espera
  | 'EN_ATENCION'    // en silla dental (atención activa)
  | 'ATENDIDA'       // consulta finalizada con éxito
  | 'FINALIZADO'     // alias de ATENDIDA (backend compat.)
  | 'NO_ASISTIO'     // no se presentó
  | 'CANCELADA'      // cancelada
  | 'CANCELADO';     // alias frontend

interface CitaHoy {
  id: number;
  pacienteId: number;
  pacienteNombre: string;
  pacienteDocumento: string;
  pacienteEdad?: number;
  pacienteEps?: string;
  hora: string;
  estado: EstadoCita;
  motivo?: string;
}

@Component({
  selector: 'app-odontologia-page',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterLink, FontAwesomeModule, OdontogramaComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './odontologia.page.html',
  styleUrl: './odontologia.page.scss',
})
export class OdontologiaPageComponent implements OnInit {

  // ── Servicios ────────────────────────────────────────────────────────
  private readonly odontoSvc  = inject(OdontologiaService);
  private readonly citaSvc    = inject(CitaService);
  private readonly auth       = inject(AuthService);
  private readonly toast      = inject(SesaToastService);

  // ── Iconos ───────────────────────────────────────────────────────────
  readonly faTooth = faTooth;
  readonly faUser = faUser;
  readonly faCalendarCheck = faCalendarCheck;
  readonly faClipboardList = faClipboardList;
  readonly faFileInvoiceDollar = faFileInvoiceDollar;
  readonly faChartLine = faChartLine;
  readonly faImages = faImages;
  readonly faNotesMedical = faNotesMedical;
  readonly faPlus = faPlus;
  readonly faSave = faSave;
  readonly faPrint = faPrint;
  readonly faArrowLeft = faArrowLeft;
  readonly faSearch = faSearch;
  readonly faChevronLeft = faChevronLeft;
  readonly faChevronRight = faChevronRight;
  readonly faSpinner = faSpinner;
  readonly faCircleCheck = faCircleCheck;
  readonly faCircleXmark = faCircleXmark;
  readonly faTrash = faTrash;
  readonly faEdit = faEdit;
  readonly faEye = faEye;
  readonly faUpload = faUpload;
  readonly faMoneyBillWave = faMoneyBillWave;
  readonly faStethoscope = faStethoscope;
  readonly faShieldHalved = faShieldHalved;

  // ── Estado principal ─────────────────────────────────────────────────
  readonly tabPrincipal = signal<TabPrincipal>('dashboard');
  readonly tabFicha     = signal<TabFicha>('odontograma');
  readonly cargando     = signal(false);

  // ── Dashboard ────────────────────────────────────────────────────────
  readonly stats = signal({
    citasHoy: 0, atendidos: 0, pendientes: 0, cancelados: 0,
    planesActivos: 0, pacientesSinControl: 0,
  });

  readonly citasHoy     = signal<CitaHoy[]>([]);
  readonly fechaActual  = signal(new Date());
  readonly busquedaCita = signal('');

  // ── Flujo de atención ─────────────────────────────────────────────────
  /** ID de la cita que está esperando confirmación de cancelación */
  readonly citaCancelId    = signal<number | null>(null);
  /** Procesando cambio de estado (muestra spinner en botón) */
  readonly citaCambiandoId = signal<number | null>(null);

  readonly citasFiltradas = computed(() => {
    const q = this.busquedaCita().toLowerCase();
    return !q
      ? this.citasHoy()
      : this.citasHoy().filter(c =>
          c.pacienteNombre.toLowerCase().includes(q) ||
          c.pacienteDocumento.includes(q)
        );
  });

  // ── Paciente seleccionado ────────────────────────────────────────────
  readonly pacienteActivo = signal<CitaHoy | null>(null);
  readonly consultaActiva = signal<ConsultaOdontologicaDto | null>(null);
  readonly consultasHistorial = signal<ConsultaOdontologicaDto[]>([]);

  // ── Historial de consultas ───────────────────────────────────────────
  /** ID de la consulta cuyo detalle está expandido en el historial */
  readonly consultaExpandidaId  = signal<number | null>(null);
  /** Cambios del odontograma de la consulta histórica seleccionada */
  readonly odontogramaHistorial = signal<OdontogramaEstadoDto[]>([]);
  readonly cargandoOdontHist    = signal(false);

  // ── Formulario de consulta (SOAP) ────────────────────────────────────
  readonly formConsulta = signal<ConsultaOdontologicaDto>({
    pacienteId: 0, profesionalId: 0,
    tipoConsulta: 'PRIMERA_VEZ',
    higieneOral: 'BUENA', riesgoCaries: 'BAJO',
    condicionPeriodontal: 'SANA', estado: 'EN_ATENCION',
    consentimientoFirmado: false,
  });
  readonly modoEdicionConsulta = signal(false);

  // ── Odontograma ──────────────────────────────────────────────────────
  @ViewChild(OdontogramaComponent) odontogramaRef?: OdontogramaComponent;
  readonly piezasDentales = signal<Map<number, PiezaDental>>(new Map());
  readonly cambiosPendientes = signal<OdontogramaCambio[]>([]);

  // ── Procedimientos / Plan de tratamiento ─────────────────────────────
  readonly catalogo         = signal<ProcedimientoCatalogo[]>([]);
  readonly planesPaciente   = signal<PlanTratamientoDto[]>([]);
  readonly planActivo       = signal<PlanTratamientoDto | null>(null);
  readonly busquedaProc     = signal('');
  readonly procedimientoSel = signal<ProcedimientoCatalogo | null>(null);
  readonly piezaSelPlan     = signal<number | null>(null);
  readonly cantidadPlan     = signal(1);
  readonly descuentoItem    = signal(0);
  readonly montoAbono       = signal(0);

  readonly catalogoFiltrado = computed(() => {
    const q = this.busquedaProc().toLowerCase();
    return !q
      ? this.catalogo()
      : this.catalogo().filter(p =>
          p.nombre.toLowerCase().includes(q) || (p.codigo ?? '').toLowerCase().includes(q)
        );
  });

  readonly totalPlanNuevo = computed(() => {
    const items = this.planActivo()?.items ?? [];
    return items.reduce((acc, i) => {
      const base = (i.precioUnitario ?? 0) * (i.cantidad ?? 1);
      const desc = (base * (i.descuento ?? 0)) / 100;
      return acc + base - desc;
    }, 0);
  });

  // ── Imágenes ─────────────────────────────────────────────────────────
  readonly imagenes        = signal<ImagenClinicaDto[]>([]);
  readonly imagenVisor     = signal<ImagenClinicaDto | null>(null);
  readonly nuevaImgTipo    = signal('FOTO_CLINICA');
  readonly nuevaImgDesc    = signal('');
  readonly nuevaImgPieza   = signal<number | null>(null);
  readonly nuevaImgBase64  = signal('');

  // ── Evoluciones ──────────────────────────────────────────────────────
  readonly evoluciones    = signal<EvolucionOdontologicaDto[]>([]);
  readonly nuevaNota      = signal('');
  readonly nuevoControl   = signal('');
  readonly proxCita       = signal('');

  // ── Colores odontograma (expuestos al template) ───────────────────────
  readonly dxLabel = ESTADO_LABEL;
  readonly dxColor = ESTADO_COLOR;

  // ── Profesional actual ───────────────────────────────────────────────
  /** Usa personalId (ID de Personal) para filtrar citas; cae a userId si no hay vínculo. */
  get profesionalId(): number {
    const u = this.auth.currentUser();
    return u?.personalId ?? u?.userId ?? 0;
  }
  get profesionalNombre(): string {
    return this.auth.currentUser()?.nombreCompleto ?? '';
  }

  // ────────────────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.cargarCitasHoy();
    this.cargarCatalogo();
    this.cargarStats();
  }

  // ── Citas ────────────────────────────────────────────────────────────

  /** Formatea una fecha usando la hora local (evita desfase UTC en Colombia). */
  private toLocalDateStr(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  cargarCitasHoy(): void {
    this.cargando.set(true);
    const fechaISO = this.toLocalDateStr(this.fechaActual());
    this.citaSvc.getConsultasMedicas(fechaISO, this.profesionalId).subscribe({
      next: (dtos: ConsultaMedicaDto[]) => {
        this.citasHoy.set(dtos.map(d => ({
          id:               d.id,
          pacienteId:       d.pacienteId,
          pacienteNombre:   d.pacienteNombreCompleto,
          pacienteDocumento: d.pacienteDocumento,
          pacienteEdad:     d.pacienteEdad,
          pacienteEps:      d.pacienteEps,
          hora:             d.fechaHora ? d.fechaHora.substring(11, 16) : '',
          estado:           (d.estado as EstadoCita) ?? 'EN_ESPERA',
          motivo:           d.notas,
        })));
        this.actualizarStats();
        this.cargando.set(false);
      },
      error: () => {
        this.toast.error('Error al cargar las citas del día');
        this.cargando.set(false);
      },
    });
  }

  cambiarFecha(dias: number): void {
    const f = new Date(this.fechaActual());
    f.setDate(f.getDate() + dias);
    this.fechaActual.set(f);
    this.cargarCitasHoy();
  }

  seleccionarCita(cita: CitaHoy): void {
    this.pacienteActivo.set(cita);
    this.tabPrincipal.set('paciente');
    this.tabFicha.set('odontograma');
    this.inicializarFicha(cita);
  }

  // ── Flujo clínico de atención ─────────────────────────────────────

  /** Paciente llegó — pasa a sala de espera */
  marcarLlegada(event: Event, cita: CitaHoy): void {
    event.stopPropagation();
    this.cambiarEstadoCita(cita, 'EN_ESPERA');
  }

  /** Odontólogo llama al paciente — inicia la atención y abre la ficha */
  iniciarAtencion(event: Event, cita: CitaHoy): void {
    event.stopPropagation();
    this.citaCambiandoId.set(cita.id);
    this.citaSvc.cambiarEstado(cita.id, 'EN_ATENCION').subscribe({
      next: () => {
        this.actualizarEstadoLocal(cita.id, 'EN_ATENCION');
        this.citaCambiandoId.set(null);
        this.toast.success(`Atendiendo a ${cita.pacienteNombre}`);
        // Abre la ficha automáticamente
        this.seleccionarCita({ ...cita, estado: 'EN_ATENCION' });
      },
      error: () => {
        this.citaCambiandoId.set(null);
        this.toast.error('No se pudo iniciar la atención');
      },
    });
  }

  /** Finaliza la consulta — marca como ATENDIDA */
  finalizarCita(event: Event, cita: CitaHoy): void {
    event.stopPropagation();
    this.cambiarEstadoCita(cita, 'ATENDIDA');
  }

  /** Paciente no asistió */
  marcarNoAsistio(event: Event, cita: CitaHoy): void {
    event.stopPropagation();
    this.cambiarEstadoCita(cita, 'NO_ASISTIO');
  }

  /** Muestra confirmación inline de cancelación */
  pedirConfirmCancelar(event: Event, cita: CitaHoy): void {
    event.stopPropagation();
    this.citaCancelId.set(this.citaCancelId() === cita.id ? null : cita.id);
  }

  /** Confirma y ejecuta la cancelación */
  confirmarCancelar(event: Event, cita: CitaHoy): void {
    event.stopPropagation();
    this.citaCancelId.set(null);
    this.citaSvc.cancelarCita(cita.id, 'Cancelada desde módulo de odontología').subscribe({
      next: () => {
        this.actualizarEstadoLocal(cita.id, 'CANCELADA');
        this.toast.warning(`Cita de ${cita.pacienteNombre} cancelada`);
        this.actualizarStats();
      },
      error: () => this.toast.error('No se pudo cancelar la cita'),
    });
  }

  private cambiarEstadoCita(cita: CitaHoy, estado: string): void {
    this.citaCambiandoId.set(cita.id);
    this.citaSvc.cambiarEstado(cita.id, estado).subscribe({
      next: () => {
        this.actualizarEstadoLocal(cita.id, estado as EstadoCita);
        this.citaCambiandoId.set(null);
        this.actualizarStats();
      },
      error: () => {
        this.citaCambiandoId.set(null);
        this.toast.error('No se pudo cambiar el estado');
      },
    });
  }

  /** Actualiza el estado de una cita en la lista local sin recargar */
  private actualizarEstadoLocal(id: number, estado: EstadoCita): void {
    this.citasHoy.update(lista =>
      lista.map(c => c.id === id ? { ...c, estado } : c)
    );
  }

  /** Helpers de presentación para estados de cita */
  estadoCitaClass(estado: EstadoCita): string {
    const map: Record<string, string> = {
      AGENDADA: 'badge-neutral', CONFIRMADA: 'badge-info',
      EN_ESPERA: 'badge-warning', EN_ATENCION: 'badge-primary',
      ATENDIDA: 'badge-success',  FINALIZADO: 'badge-success',
      NO_ASISTIO: 'badge-orange', CANCELADA: 'badge-danger', CANCELADO: 'badge-danger',
    };
    return map[estado] ?? 'badge-neutral';
  }

  esCitaActiva(estado: EstadoCita): boolean {
    return estado === 'EN_ATENCION';
  }

  esCitaPendiente(estado: EstadoCita): boolean {
    return estado === 'AGENDADA' || estado === 'CONFIRMADA';
  }

  esCitaEspera(estado: EstadoCita): boolean {
    return estado === 'EN_ESPERA';
  }

  esCitaTerminada(estado: EstadoCita): boolean {
    return estado === 'ATENDIDA' || estado === 'FINALIZADO' ||
           estado === 'CANCELADA' || estado === 'CANCELADO' || estado === 'NO_ASISTIO';
  }

  private inicializarFicha(cita: CitaHoy): void {
    this.formConsulta.set({
      pacienteId: cita.pacienteId,
      profesionalId: this.profesionalId,
      citaId: cita.id,
      alergias: '',
      higieneOral: 'BUENA',
      riesgoCaries: 'BAJO',
      condicionPeriodontal: 'SANA',
      estado: 'EN_ATENCION',
    });
    this.cargarDatosPaciente(cita.pacienteId);
  }

  private cargarDatosPaciente(pacienteId: number): void {
    this.cargando.set(true);
    // Cargar en paralelo
    this.odontoSvc.getOdontograma(pacienteId).subscribe({
      next: dtos => {
        const mapa = dtoListToPiezas(dtos);
        this.piezasDentales.set(mapa);
        this.odontogramaRef?.inicializar(mapa);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
    this.odontoSvc.getConsultasByPaciente(pacienteId).subscribe({
      next: cs => this.consultasHistorial.set(cs),
      error: () => {},
    });
    this.odontoSvc.getPlanesByPaciente(pacienteId).subscribe({
      next: ps => this.planesPaciente.set(ps),
      error: () => {},
    });
    this.odontoSvc.getImagenesByPaciente(pacienteId).subscribe({
      next: imgs => this.imagenes.set(imgs),
      error: () => {},
    });
    this.odontoSvc.getEvolucionesByPaciente(pacienteId).subscribe({
      next: evs => this.evoluciones.set(evs),
      error: () => {},
    });
  }

  volver(): void {
    this.tabPrincipal.set('dashboard');
    this.pacienteActivo.set(null);
    this.cambiosPendientes.set([]);
  }

  /**
   * Finaliza la consulta desde dentro de la ficha del paciente.
   * Marca la cita como ATENDIDA en el backend, actualiza la lista local y vuelve al dashboard.
   */
  finalizarDesdeFicha(): void {
    const cita = this.pacienteActivo();
    if (!cita) return;

    this.cargando.set(true);
    this.citaSvc.cambiarEstado(cita.id, 'ATENDIDA').subscribe({
      next: () => {
        this.actualizarEstadoLocal(cita.id, 'ATENDIDA');
        this.actualizarStats();
        this.cargando.set(false);
        this.toast.success(`Consulta de ${cita.pacienteNombre} finalizada`);
        this.volver();
      },
      error: () => {
        this.cargando.set(false);
        this.toast.error('No se pudo finalizar la consulta');
      },
    });
  }

  // ── Historia clínica ─────────────────────────────────────────────────

  guardarConsulta(): void {
    const form = this.formConsulta();
    this.cargando.set(true);
    if (this.consultaActiva()?.id) {
      this.odontoSvc.actualizarConsulta(this.consultaActiva()!.id!, form).subscribe({
        next: c => { this.consultaActiva.set(c); this.toast.success('Historia guardada'); this.cargando.set(false); },
        error: () => { this.toast.error('Error al guardar'); this.cargando.set(false); },
      });
    } else {
      this.odontoSvc.crearConsulta(form).subscribe({
        next: c => {
          this.consultaActiva.set(c);
          this.formConsulta.update(f => ({ ...f, id: c.id }));
          this.toast.success('Consulta creada');
          this.cargando.set(false);
        },
        error: () => { this.toast.error('Error al crear consulta'); this.cargando.set(false); },
      });
    }
  }

  updateFormConsulta(campo: keyof ConsultaOdontologicaDto, valor: unknown): void {
    this.formConsulta.update(f => ({ ...f, [campo]: valor }));
  }

  // ── Odontograma ──────────────────────────────────────────────────────

  onOdontogramaCambio(cambio: OdontogramaCambio): void {
    this.cambiosPendientes.update(cs => [...cs, cambio]);
  }

  guardarOdontograma(): void {
    if (!this.odontogramaRef) return;
    const piezas = this.odontogramaRef.getPiezasLocal();
    const pacId = this.pacienteActivo()!.pacienteId;
    const consId = this.consultaActiva()?.id;
    const dtos = piezasChangedToDtos(piezas, pacId, this.profesionalId, consId);

    this.cargando.set(true);
    this.odontoSvc.guardarOdontogramaBatch(dtos).subscribe({
      next: () => {
        this.cambiosPendientes.set([]);
        this.toast.success('Odontograma guardado');
        this.cargando.set(false);
      },
      error: () => { this.toast.error('Error al guardar odontograma'); this.cargando.set(false); },
    });
  }

  // ── Plan de tratamiento ──────────────────────────────────────────────

  cargarCatalogo(): void {
    this.odontoSvc.getCatalogoProcedimientos().subscribe({
      next: c => this.catalogo.set(c),
      error: () => {},
    });
  }

  nuevoPlan(): void {
    const paciente = this.pacienteActivo();
    if (!paciente) return;
    this.planActivo.set({
      pacienteId: paciente.pacienteId,
      profesionalId: this.profesionalId,
      consultaId: this.consultaActiva()?.id,
      nombre: 'Plan de Tratamiento',
      fase: (this.planesPaciente().length + 1),
      tipoPago: 'PARTICULAR',
      estado: 'PENDIENTE',
      items: [],
    });
  }

  agregarItemPlan(): void {
    const proc = this.procedimientoSel();
    if (!proc || !proc.id) return;
    const precio = proc.precioBase ?? 0;
    const desc = this.descuentoItem();
    const total = precio * this.cantidadPlan() * (1 - desc / 100);
    const item: PlanTratamientoItemDto = {
      procedimientoId: proc.id,
      procedimientoNombre: proc.nombre,
      procedimientoCodigo: proc.codigo,
      piezaFdi: this.piezaSelPlan() ?? undefined,
      cantidad: this.cantidadPlan(),
      precioUnitario: precio,
      descuento: desc,
      valorTotal: total,
      estado: 'PENDIENTE',
    };
    this.planActivo.update(p => p ? { ...p, items: [...(p.items ?? []), item] } : p);
    this.procedimientoSel.set(null);
    this.piezaSelPlan.set(null);
    this.cantidadPlan.set(1);
    this.descuentoItem.set(0);
  }

  eliminarItemPlan(idx: number): void {
    this.planActivo.update(p => p ? { ...p, items: p.items!.filter((_, i) => i !== idx) } : p);
  }

  guardarPlan(): void {
    const plan = this.planActivo();
    if (!plan) return;
    const total = this.totalPlanNuevo();
    const desc = plan.descuento ?? 0;
    const final = total * (1 - desc / 100);
    const payload: PlanTratamientoDto = { ...plan, valorTotal: total, valorFinal: final };
    this.cargando.set(true);
    if (plan.id) {
      this.odontoSvc.actualizarPlan(plan.id, payload).subscribe({
        next: p => { this.actualizarPlanEnLista(p); this.toast.success('Plan actualizado'); this.cargando.set(false); },
        error: () => { this.toast.error('Error al actualizar'); this.cargando.set(false); },
      });
    } else {
      this.odontoSvc.crearPlan(payload).subscribe({
        next: p => {
          this.planesPaciente.update(ps => [p, ...ps]);
          this.planActivo.set(p);
          this.toast.success('Plan creado');
          this.cargando.set(false);
        },
        error: () => { this.toast.error('Error al crear plan'); this.cargando.set(false); },
      });
    }
  }

  registrarAbono(): void {
    const plan = this.planActivo();
    if (!plan?.id || this.montoAbono() <= 0) return;
    this.odontoSvc.registrarAbono(plan.id, this.montoAbono()).subscribe({
      next: p => {
        this.actualizarPlanEnLista(p);
        this.planActivo.set(p);
        this.montoAbono.set(0);
        this.toast.success('Abono registrado');
      },
      error: () => this.toast.error('Error al registrar abono'),
    });
  }

  cambiarEstadoPlan(plan: PlanTratamientoDto, estado: string): void {
    if (!plan.id) return;
    this.odontoSvc.cambiarEstadoPlan(plan.id, estado).subscribe({
      next: p => { this.actualizarPlanEnLista(p); this.planActivo.set(p); this.toast.success('Estado actualizado'); },
      error: () => this.toast.error('Error al cambiar estado'),
    });
  }

  private actualizarPlanEnLista(plan: PlanTratamientoDto): void {
    this.planesPaciente.update(ps => ps.map(p => p.id === plan.id ? plan : p));
  }

  // ── Imágenes ─────────────────────────────────────────────────────────

  onArchivoSeleccionado(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (e) => {
      this.nuevaImgBase64.set(e.target?.result as string);
    };
    reader.readAsDataURL(file);
  }

  subirImagen(): void {
    const paciente = this.pacienteActivo();
    if (!paciente || !this.nuevaImgBase64()) return;
    const dto: ImagenClinicaDto = {
      pacienteId: paciente.pacienteId,
      profesionalId: this.profesionalId,
      consultaId: this.consultaActiva()?.id,
      tipo: this.nuevaImgTipo() as any,
      descripcion: this.nuevaImgDesc(),
      piezaFdi: this.nuevaImgPieza() ?? undefined,
      thumbnailBase64: this.nuevaImgBase64(),
      url: this.nuevaImgBase64(),
      nombreArchivo: `imagen_${Date.now()}.jpg`,
    };
    this.odontoSvc.subirImagen(dto).subscribe({
      next: img => {
        this.imagenes.update(imgs => [img, ...imgs]);
        this.nuevaImgBase64.set('');
        this.nuevaImgDesc.set('');
        this.toast.success('Imagen subida correctamente');
      },
      error: () => this.toast.error('Error al subir imagen'),
    });
  }

  eliminarImagen(img: ImagenClinicaDto): void {
    if (!img.id) return;
    this.odontoSvc.eliminarImagen(img.id).subscribe({
      next: () => {
        this.imagenes.update(imgs => imgs.filter(i => i.id !== img.id));
        if (this.imagenVisor()?.id === img.id) this.imagenVisor.set(null);
        this.toast.success('Imagen eliminada');
      },
      error: () => this.toast.error('Error al eliminar'),
    });
  }

  // ── Evolución ────────────────────────────────────────────────────────

  guardarEvolucion(): void {
    const paciente = this.pacienteActivo();
    const consulta = this.consultaActiva();
    if (!paciente || !consulta?.id || !this.nuevaNota()) {
      this.toast.warning('Complete la nota de evolución y asegúrese de tener una consulta activa');
      return;
    }
    const dto: EvolucionOdontologicaDto = {
      pacienteId: paciente.pacienteId,
      profesionalId: this.profesionalId,
      consultaId: consulta.id,
      planId: this.planActivo()?.id,
      notaEvolucion: this.nuevaNota(),
      controlPostTratamiento: this.nuevoControl(),
      proximaCitaRecomendada: this.proxCita() ? new Date(this.proxCita()).toISOString() : undefined,
    };
    this.odontoSvc.registrarEvolucion(dto).subscribe({
      next: ev => {
        this.evoluciones.update(evs => [ev, ...evs]);
        this.nuevaNota.set('');
        this.nuevoControl.set('');
        this.proxCita.set('');
        this.toast.success('Evolución registrada');
      },
      error: () => this.toast.error('Error al guardar evolución'),
    });
  }

  // ── Stats ────────────────────────────────────────────────────────────

  private cargarStats(): void {
    const fechaISO = this.toLocalDateStr(this.fechaActual());
    // Stats de citas del día vía CitaService
    this.citaSvc.getStats(fechaISO, this.profesionalId).subscribe({
      next: s => {
        this.stats.update(prev => ({
          ...prev,
          citasHoy:   s.total      ?? prev.citasHoy,
          atendidos:  s.atendidas  ?? prev.atendidos,
          cancelados: s.canceladas ?? prev.cancelados,
          pendientes: s.agendadas  ?? prev.pendientes,
        }));
      },
      error: () => {},
    });
    // Stats de odontología (planes activos, pacientes sin control)
    this.odontoSvc.getStats(this.profesionalId).subscribe({
      next: s => {
        const d = s as { planesActivos?: number; pacientesSinControl?: number };
        this.stats.update(prev => ({
          ...prev,
          planesActivos:       d.planesActivos       ?? prev.planesActivos,
          pacientesSinControl: d.pacientesSinControl ?? prev.pacientesSinControl,
        }));
      },
      error: () => {},
    });
  }

  private actualizarStats(): void {
    const citas = this.citasHoy();
    this.stats.update(s => ({
      ...s,
      citasHoy: citas.length,
      atendidos: citas.filter(c => c.estado === 'FINALIZADO').length,
      pendientes: citas.filter(c => c.estado === 'EN_ESPERA' || c.estado === 'EN_ATENCION').length,
      cancelados: citas.filter(c => c.estado === 'CANCELADO').length,
    }));
  }

  // ── Helpers template ─────────────────────────────────────────────────

  fechaStr(fecha: Date): string {
    return fecha.toLocaleDateString('es-CO', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });
  }

  getEstadoLabel(estado: string): string {
    const map: Record<string, string> = {
      AGENDADA: 'Agendada', CONFIRMADA: 'Confirmada',
      EN_ESPERA: 'En espera', EN_ATENCION: 'En atención',
      ATENDIDA: 'Atendida', FINALIZADO: 'Finalizado',
      NO_ASISTIO: 'No asistió', CANCELADA: 'Cancelada', CANCELADO: 'Cancelada',
      PENDIENTE: 'Pendiente', EN_TRATAMIENTO: 'En tratamiento',
    };
    return map[estado] ?? estado;
  }

  planEstadoClass(estado: string): string {
    const map: Record<string, string> = {
      PENDIENTE: 'badge-warning', EN_TRATAMIENTO: 'badge-primary',
      FINALIZADO: 'badge-success', CANCELADO: 'badge-danger',
    };
    return map[estado] ?? '';
  }

  formatCurrency(val: number | undefined): string {
    return new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', minimumFractionDigits: 0 })
      .format(val ?? 0);
  }

  formatFecha(iso: string | undefined): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  formatHora(iso: string | undefined): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' });
  }

  // ── Historial de consultas ─────────────────────────────────────────────

  /**
   * Alterna la tarjeta expandida del historial y carga el odontograma
   * de esa consulta si aún no se ha cargado.
   */
  toggleConsultaHistorial(c: ConsultaOdontologicaDto): void {
    if (this.consultaExpandidaId() === c.id) {
      this.consultaExpandidaId.set(null);
      this.odontogramaHistorial.set([]);
      return;
    }
    this.consultaExpandidaId.set(c.id!);
    if (c.id) {
      this.cargandoOdontHist.set(true);
      this.odontoSvc.getOdontogramaByConsulta(c.id).subscribe({
        next: dtos => {
          this.odontogramaHistorial.set(dtos);
          this.cargandoOdontHist.set(false);
        },
        error: () => {
          this.odontogramaHistorial.set([]);
          this.cargandoOdontHist.set(false);
        },
      });
    }
  }

  /** Helper: label del valor de enum higiene oral */
  labelHigiene(v?: string): string {
    return v === 'BUENA' ? '😊 Buena' : v === 'REGULAR' ? '😐 Regular' : v === 'MALA' ? '😟 Mala' : v ?? '—';
  }

  /** Helper: label del riesgo de caries */
  labelRiesgo(v?: string): string {
    return v === 'BAJO' ? '🟢 Bajo' : v === 'MEDIO' ? '🟡 Medio' : v === 'ALTO' ? '🔴 Alto' : v ?? '—';
  }

  /** Helper: label de condición periodontal */
  labelPerio(v?: string): string {
    const m: Record<string, string> = { SANA: '✅ Sana', LEVE: '⚠️ Leve', MODERADA: '🟠 Moderada', SEVERA: '🔴 Severa' };
    return m[v ?? ''] ?? v ?? '—';
  }

  /** Nombre legible de superficie dental */
  labelSuperficie(s: string | undefined | null): string {
    if (!s) return '—';
    const m: Record<string, string> = {
      VESTIBULAR: 'Vestibular', LINGUAL: 'Lingual/Palatino',
      MESIAL: 'Mesial', DISTAL: 'Distal', OCLUSAL: 'Oclusal', GENERAL: 'General',
    };
    return m[s] ?? s;
  }

  /** Color de badge para estado dental en el historial */
  getEstadoColorHist(estado: string | undefined | null): string {
    if (!estado) return '#94a3b8';
    return ESTADO_COLOR[estado as keyof typeof ESTADO_COLOR] ?? '#94a3b8';
  }

  seleccionarProcedimiento(proc: ProcedimientoCatalogo): void {
    this.procedimientoSel.set(proc);
  }

  // Setters del plan activo (evitan spread inline en templates Angular)
  setPlanNombre(val: string): void {
    this.planActivo.update(p => p ? { ...p, nombre: val } : p);
  }
  setPlanFase(val: number): void {
    this.planActivo.update(p => p ? { ...p, fase: val } : p);
  }
  setPlanTipoPago(val: string): void {
    this.planActivo.update(p => p ? { ...p, tipoPago: val as any } : p);
  }
  setPlanDescuento(val: number): void {
    this.planActivo.update(p => p ? { ...p, descuento: val } : p);
  }

  abrirPlan(plan: PlanTratamientoDto): void {
    this.planActivo.set({ ...plan });
  }

  readonly TIPOS_IMAGEN = [
    { value: 'FOTO_CLINICA', label: 'Foto Clínica' },
    { value: 'RADIOGRAFIA_PERIAPICAL', label: 'Rx Periapical' },
    { value: 'RADIOGRAFIA_PANORAMICA', label: 'Rx Panorámica' },
    { value: 'MODELO', label: 'Modelo de estudio' },
    { value: 'OTRO', label: 'Otro' },
  ];

  readonly HIGIENE_OPTIONS = ['BUENA','REGULAR','MALA'];
  readonly RIESGO_OPTIONS  = ['BAJO','MEDIO','ALTO'];
  readonly PERIODO_OPTIONS = ['SANA','LEVE','MODERADA','SEVERA'];
  readonly TIPO_PAGO_OPTIONS = [
    { value: 'PARTICULAR', label: 'Particular' },
    { value: 'EPS', label: 'EPS / Seguridad Social' },
    { value: 'MIXTO', label: 'Mixto' },
  ];

  readonly TIPO_CONSULTA_OPTIONS = [
    { value: 'PRIMERA_VEZ',          label: 'Primera vez' },
    { value: 'CONTROL',              label: 'Control' },
    { value: 'URGENCIA_ODONTOLOGICA', label: 'Urgencia odontológica' },
    { value: 'INTERCONSULTA',        label: 'Interconsulta' },
  ];

  // CIE-10 K00-K14 (Enfermedades de los dientes y estructuras de soporte)
  readonly CIE10_ODONTO = [
    { code: 'K00', desc: 'Trastornos del desarrollo y erupción de los dientes' },
    { code: 'K01', desc: 'Dientes incluidos e impactados' },
    { code: 'K02', desc: 'Caries dental' },
    { code: 'K03', desc: 'Otras enfermedades de los tejidos duros de los dientes' },
    { code: 'K04', desc: 'Enfermedades de la pulpa y de los tejidos periapicales' },
    { code: 'K05', desc: 'Gingivitis y enfermedades periodontales' },
    { code: 'K06', desc: 'Otros trastornos de la encía y de la zona edéntula' },
    { code: 'K07', desc: 'Anomalías dentofaciales (incl. maloclusión)' },
    { code: 'K08', desc: 'Otros trastornos de los dientes y de sus estructuras de sostén' },
    { code: 'K09', desc: 'Quistes de la región bucal' },
    { code: 'K10', desc: 'Otras enfermedades de los maxilares' },
    { code: 'K11', desc: 'Enfermedades de las glándulas salivales' },
    { code: 'K12', desc: 'Estomatitis y lesiones afines' },
    { code: 'K13', desc: 'Otras enfermedades de los labios y de la mucosa bucal' },
    { code: 'K14', desc: 'Enfermedades de la lengua' },
  ];

  // ── Computed IHO-S ─────────────────────────────────────────────────
  readonly ihosTotal = computed(() => {
    const f = this.formConsulta();
    const placa  = f.ihosPlaca   ?? 0;
    const calculo = f.ihosCalculo ?? 0;
    return +(placa + calculo).toFixed(2);
  });

  readonly ihosClassification = computed(() => {
    const t = this.ihosTotal();
    if (t === 0)        return { label: 'Excelente (0)', css: 'ihos--excelente' };
    if (t <= 0.6)       return { label: 'Buena (0.1-0.6)', css: 'ihos--buena' };
    if (t <= 1.8)       return { label: 'Regular (0.7-1.8)', css: 'ihos--regular' };
    return              { label: 'Mala (>1.8)', css: 'ihos--mala' };
  });

  // ── Computed CPOD ──────────────────────────────────────────────────
  readonly cpodTotal = computed(() => {
    const f = this.formConsulta();
    return (f.cpodCariados ?? 0) + (f.cpodPerdidos ?? 0) + (f.cpodObturados ?? 0);
  });

  readonly ceodTotal = computed(() => {
    const f = this.formConsulta();
    return (f.ceodCariados ?? 0) + (f.ceodExtraidos ?? 0) + (f.ceodObturados ?? 0);
  });

  // ── Setters formulario consulta (compatibilidad ngModel-like) ───────
  setConsultaField<K extends keyof ConsultaOdontologicaDto>(key: K, val: ConsultaOdontologicaDto[K]): void {
    this.formConsulta.update(f => ({ ...f, [key]: val }));
  }

  seleccionarCie10(item: { code: string; desc: string }): void {
    this.formConsulta.update(f => ({ ...f, codigoCie10: item.code, descripcionCie10: item.desc }));
    this.showCie10Dropdown.set(false);
  }

  readonly showCie10Dropdown = signal(false);
  cie10SearchQuery = '';

  readonly cie10Filtrado = computed(() => {
    const q = this.cie10SearchQuery.toLowerCase();
    if (!q) return this.CIE10_ODONTO;
    return this.CIE10_ODONTO.filter(c =>
      c.code.toLowerCase().includes(q) || c.desc.toLowerCase().includes(q)
    );
  });
}
