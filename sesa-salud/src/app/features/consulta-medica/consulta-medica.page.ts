/**
 * Módulo Consulta Médica — Vista de citas del profesional logueado.
 * Permite al médico/jefe de enfermería ver su agenda del día y al admin
 * filtrar por cualquier profesional médico.
 * Autor: Ing. J Sebastian Vargas S
 */

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
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  faCalendarDays,
  faChevronLeft,
  faChevronRight,
  faSearch,
  faStethoscope,
  faUser,
  faIdCard,
  faPhone,
  faHospital,
  faFileAlt,
  faCheckCircle,
  faTimesCircle,
  faExclamationTriangle,
  faClock,
  faNotesMedical,
  faFileMedical,
  faMoneyBillWave,
  faPrint,
  faFileExcel,
  faChevronDown,
  faFilter,
  faRotateRight,
  faBell,
  faCircleInfo,
  faVideo,
  faFilePrescription,
} from '@fortawesome/free-solid-svg-icons';
import { Subscription, interval } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { PermissionsService } from '../../core/services/permissions.service';
import {
  CitaService,
  ConsultaMedicaDto,
  ConsultasStatsDto,
  ProfesionalDto,
} from '../../core/services/cita.service';
import { SesaCalendarComponent } from '../../shared/components/sesa-calendar/sesa-calendar.component';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';
import { RecetaElectronicaModalComponent } from '../receta-electronica/receta-electronica-modal.component';

@Component({
  selector: 'sesa-consulta-medica',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    FontAwesomeModule,
    SesaCalendarComponent,
    RecetaElectronicaModalComponent,
  ],
  templateUrl: './consulta-medica.page.html',
  styleUrl: './consulta-medica.page.scss',
})
export class ConsultaMedicaPageComponent implements OnInit, OnDestroy {

  // ── Ícones ─────────────────────────────────────────────────────────────
  readonly faCalendarDays      = faCalendarDays;
  readonly faChevronLeft       = faChevronLeft;
  readonly faChevronRight      = faChevronRight;
  readonly faSearch            = faSearch;
  readonly faStethoscope       = faStethoscope;
  readonly faUser              = faUser;
  readonly faIdCard            = faIdCard;
  readonly faPhone             = faPhone;
  readonly faHospital          = faHospital;
  readonly faFileAlt           = faFileAlt;
  readonly faCheckCircle       = faCheckCircle;
  readonly faTimesCircle       = faTimesCircle;
  readonly faExclamationTriangle = faExclamationTriangle;
  readonly faClock             = faClock;
  readonly faNotesMedical      = faNotesMedical;
  readonly faFileMedical       = faFileMedical;
  readonly faMoneyBillWave     = faMoneyBillWave;
  readonly faPrint             = faPrint;
  readonly faFileExcel         = faFileExcel;
  readonly faChevronDown       = faChevronDown;
  readonly faFilter            = faFilter;
  readonly faRotateRight       = faRotateRight;
  readonly faBell              = faBell;
  readonly faCircleInfo        = faCircleInfo;
  readonly faVideo             = faVideo;
  readonly faFilePrescription  = faFilePrescription;

  // ── Servicios ──────────────────────────────────────────────────────────
  private readonly citaService   = inject(CitaService);
  private readonly auth          = inject(AuthService);
  readonly permissions           = inject(PermissionsService);
  private readonly router        = inject(Router);
  private readonly toast         = inject(SesaToastService);
  private readonly cdr           = inject(ChangeDetectorRef);

  // ── Estado principal ──────────────────────────────────────────────────
  citas           = signal<ConsultaMedicaDto[]>([]);
  stats           = signal<ConsultasStatsDto | null>(null);
  profesionales   = signal<ProfesionalDto[]>([]);
  loading         = signal(false);

  // ── Fecha seleccionada ─────────────────────────────────────────────────
  private _today = new Date();
  selectedDateStr = signal<string>(this.toDateStr(this._today));

  // ── Filtros ───────────────────────────────────────────────────────────
  searchQuery            = signal('');
  selectedProfesionalId  = signal<number | null>(null);
  selectedEstadoFiltro   = signal<string>('');

  // ── Computed ──────────────────────────────────────────────────────────
  isAdmin = computed(() => {
    const role = this.auth.currentUser()?.role?.toUpperCase() ?? '';
    return ['ADMIN', 'SUPERADMINISTRADOR'].includes(role);
  });

  citasFiltradas = computed(() => {
    const q = this.searchQuery().toLowerCase().trim();
    const est = this.selectedEstadoFiltro();
    return this.citas().filter(c => {
      const matchQ = !q || c.pacienteNombreCompleto.toLowerCase().includes(q)
                       || c.pacienteDocumento.includes(q)
                       || (c.servicio?.toLowerCase().includes(q) ?? false);
      const matchEst = !est || c.estado === est;
      return matchQ && matchEst;
    });
  });

  citasProximas = computed(() => {
    const now = new Date();
    return this.citas().filter(c => {
      if (c.estado !== 'AGENDADA') return false;
      const diff = new Date(c.fechaHora).getTime() - now.getTime();
      return diff > 0 && diff <= 15 * 60 * 1000;
    });
  });

  totalCitas   = computed(() => this.stats()?.total ?? 0);
  agendadas    = computed(() => this.stats()?.agendadas ?? 0);
  atendidas    = computed(() => this.stats()?.atendidas ?? 0);
  canceladas   = computed(() => this.stats()?.canceladas ?? 0);
  pctAsistencia = computed(() => this.stats()?.porcentajeAsistencia ?? 0);

  // ── Reloj en vivo ─────────────────────────────────────────────────────
  horaActual = signal<string>('');
  private clockSub?: Subscription;

  // ── Modal cancelar ────────────────────────────────────────────────────
  showCancelModal  = signal(false);
  citaCancelarId   = signal<number | null>(null);
  motivoCancelacion = '';

  // ── Modal detalle ─────────────────────────────────────────────────────
  showDetalleModal  = signal(false);
  citaDetalle       = signal<ConsultaMedicaDto | null>(null);

  // ── Modal receta electrónica ──────────────────────────────────────────
  showRecetaModal   = signal(false);
  citaParaReceta    = signal<ConsultaMedicaDto | null>(null);

  // ── ID del personal logueado (para rol Médico/Jefe) ──────────────────
  private miPersonalId: number | null = null;

  // ── Ciclo de vida ─────────────────────────────────────────────────────
  ngOnInit(): void {
    this.tickClock();
    this.clockSub = interval(60_000).subscribe(() => {
      this.tickClock();
      this.cdr.markForCheck();
    });
    this.loadProfesionalesIfAdmin();
    this.loadMiPersonalId();
  }

  ngOnDestroy(): void {
    this.clockSub?.unsubscribe();
  }

  // ── Carga de datos ────────────────────────────────────────────────────

  private loadMiPersonalId(): void {
    // Para roles médico, usamos el profesionalId del usuario actual.
    // El backend detecta el usuario desde el JWT, así que basta con
    // pasar profesionalId = null cuando sea admin o pasarlo explícito.
    // Aquí obtenemos el personal del usuario actual si el backend tiene endpoint.
    // Por ahora: si es admin no necesitamos filtrar por profesional al inicio.
    if (!this.isAdmin()) {
      // Cargamos todas las citas y el backend filtrará por el JWT.
      // Para la UI pasamos profesionalId si lo conocemos.
      this.cargarCitas();
    } else {
      this.cargarCitas();
    }
  }

  private loadProfesionalesIfAdmin(): void {
    if (!this.isAdmin()) return;
    this.citaService.getProfesionalesMedicos().subscribe({
      next: (list) => {
        this.profesionales.set(list);
        this.cdr.markForCheck();
      },
      error: () => {},
    });
  }

  cargarCitas(): void {
    this.loading.set(true);
    const fecha = this.selectedDateStr();
    const profId = this.isAdmin() ? this.selectedProfesionalId() ?? undefined : undefined;

    this.citaService.getConsultasMedicas(fecha, profId ?? undefined).subscribe({
      next: (data) => {
        this.citas.set(data);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading.set(false);
        this.toast.error('No se pudieron cargar las consultas.');
        this.cdr.markForCheck();
      },
    });

    this.citaService.getStats(fecha, profId ?? undefined).subscribe({
      next: (s) => {
        this.stats.set(s);
        this.cdr.markForCheck();
      },
      error: () => {},
    });
  }

  // ── Cambio de fecha ──────────────────────────────────────────────────

  onDateSelected(dateStr: string): void {
    this.selectedDateStr.set(dateStr);
    this.cargarCitas();
  }

  irHoy(): void {
    this.selectedDateStr.set(this.toDateStr(new Date()));
    this.cargarCitas();
  }

  // ── Filtro de profesional (admin) ────────────────────────────────────

  seleccionarProfesional(id: number | null): void {
    this.selectedProfesionalId.set(id);
    this.cargarCitas();
  }

  // ── Acciones sobre citas ─────────────────────────────────────────────

  irAHistoriaClinica(cita: ConsultaMedicaDto): void {
    this.router.navigate(['/historia-clinica'], {
      queryParams: { pacienteId: cita.pacienteId, consultaRapida: 'true' },
    });
  }

  marcarAtendida(cita: ConsultaMedicaDto): void {
    if (cita.estado === 'ATENDIDA') return;
    this.citaService.cambiarEstado(cita.id, 'ATENDIDA').subscribe({
      next: () => {
        this.toast.success('Cita marcada como atendida.');
        this.cargarCitas();
      },
      error: () => this.toast.error('No se pudo actualizar el estado.'),
    });
  }

  abrirCancelModal(cita: ConsultaMedicaDto): void {
    this.citaCancelarId.set(cita.id);
    this.motivoCancelacion = '';
    this.showCancelModal.set(true);
  }

  cerrarCancelModal(): void {
    this.showCancelModal.set(false);
    this.citaCancelarId.set(null);
    this.motivoCancelacion = '';
  }

  confirmarCancelacion(): void {
    const id = this.citaCancelarId();
    if (id == null) return;
    this.citaService.cancelarCita(id, this.motivoCancelacion).subscribe({
      next: () => {
        this.toast.success('Cita cancelada correctamente.');
        this.cerrarCancelModal();
        this.cargarCitas();
      },
      error: () => this.toast.error('No se pudo cancelar la cita.'),
    });
  }

  verDetalle(cita: ConsultaMedicaDto): void {
    this.citaDetalle.set(cita);
    this.showDetalleModal.set(true);
  }

  cerrarDetalle(): void {
    this.showDetalleModal.set(false);
    this.citaDetalle.set(null);
  }

  generarFactura(cita: ConsultaMedicaDto): void {
    this.router.navigate(['/facturacion'], {
      queryParams: { pacienteId: cita.pacienteId },
    });
  }

  iniciarVideoconsulta(cita: ConsultaMedicaDto): void {
    this.router.navigate(['/videoconsulta'], {
      queryParams: { citaId: cita.id },
    });
  }

  abrirRecetaElectronica(cita: ConsultaMedicaDto): void {
    this.citaParaReceta.set(cita);
    this.showRecetaModal.set(true);
  }

  cerrarRecetaModal(): void {
    this.showRecetaModal.set(false);
    this.citaParaReceta.set(null);
  }

  exportarExcel(): void {
    this.toast.info('Función de exportación disponible próximamente.');
  }

  imprimirListado(): void {
    window.print();
  }

  // ── Helpers ──────────────────────────────────────────────────────────

  getEstadoBadgeClass(estado: string): string {
    switch (estado) {
      case 'AGENDADA':  return 'badge--agendada';
      case 'ATENDIDA':  return 'badge--atendida';
      case 'CANCELADA': return 'badge--cancelada';
      case 'EN_SALA':   return 'badge--en-sala';
      default:          return 'badge--default';
    }
  }

  getCardAccentClass(estado: string): string {
    switch (estado) {
      case 'AGENDADA':  return 'accent--agendada';
      case 'ATENDIDA':  return 'accent--atendida';
      case 'CANCELADA': return 'accent--cancelada';
      case 'EN_SALA':   return 'accent--en-sala';
      default:          return '';
    }
  }

  esProxima(cita: ConsultaMedicaDto): boolean {
    const now = new Date();
    const diff = new Date(cita.fechaHora).getTime() - now.getTime();
    return cita.estado === 'AGENDADA' && diff > 0 && diff <= 15 * 60 * 1000;
  }

  formatHora(fechaHora: string): string {
    const d = new Date(fechaHora);
    return d.toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' });
  }

  formatFechaCorta(isoStr?: string): string {
    if (!isoStr) return '—';
    return new Date(isoStr).toLocaleDateString('es-CO', {
      day: '2-digit', month: 'short', year: 'numeric',
    });
  }

  formatFechaDisplay(): string {
    const [y, m, d] = this.selectedDateStr().split('-').map(Number);
    const fecha = new Date(y, m - 1, d);
    return fecha.toLocaleDateString('es-CO', {
      weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
    });
  }

  esHoy(): boolean {
    return this.selectedDateStr() === this.toDateStr(new Date());
  }

  tipoDocLabel(tipo?: string): string {
    const map: Record<string, string> = {
      CC: 'C.C.', TI: 'T.I.', RC: 'R.C.', CE: 'C.E.',
      PA: 'Pasaporte', MS: 'Menor sin id.', AS: 'Adulto sin id.',
    };
    return map[tipo ?? ''] ?? tipo ?? '';
  }

  nombreRol(rol?: string): string {
    const map: Record<string, string> = {
      MEDICO: 'Médico', JEFE_ENFERMERIA: 'Jefe Enfermería',
      ODONTOLOGO: 'Odontólogo', PSICOLOGO: 'Psicólogo',
      COORDINADOR_MEDICO: 'Coordinador Médico',
    };
    return map[rol ?? ''] ?? rol ?? '';
  }

  private toDateStr(d: Date): string {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }

  private tickClock(): void {
    this.horaActual.set(
      new Date().toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' }),
    );
  }

  trackByCitaId(_: number, c: ConsultaMedicaDto): number {
    return c.id;
  }

  estadoLabel(estado: string): string {
    const map: Record<string, string> = {
      AGENDADA: 'Agendada', ATENDIDA: 'Atendida',
      CANCELADA: 'Cancelada', EN_SALA: 'En sala',
    };
    return map[estado] ?? estado;
  }
}
