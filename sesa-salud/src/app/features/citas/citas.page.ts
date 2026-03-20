/**
 * Citas Premium — búsqueda de paciente por identificación, selector visual de día/hora,
 * tarjetas de especialidad, gestión completa de agenda con spinner por acción.
 * Autor: Ing. J Sebastian Vargas S
 */
import {
  Component, OnInit, OnDestroy, inject, signal, computed, HostListener,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';

import { SesaSkeletonComponent } from '../../shared/components/sesa-skeleton/sesa-skeleton.component';
import { SesaEmptyStateComponent } from '../../shared/components/sesa-empty-state/sesa-empty-state.component';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';
import { SesaConfirmDialogService } from '../../shared/components/sesa-confirm-dialog/sesa-confirm-dialog.component';

import { CitaDto, CitaService } from '../../core/services/cita.service';
import { PacienteDto, PacienteService } from '../../core/services/paciente.service';
import { PersonalDto, PersonalService } from '../../core/services/personal.service';

/* ─── Tipos ─────────────────────────────────────────────────────────── */
export interface Especialidad {
  id: string;
  label: string;
  icon: string;
  color: string;
}

export interface TimeSlot {
  time: string;        // "08:00"
  display: string;     // "8:00 AM"
  available: boolean;
  citaId?: number;
}

type CitaTab = 'nueva' | 'lista';
type WizardStep = 1 | 2 | 3;

/* ─── Componente ─────────────────────────────────────────────────────── */
@Component({
  standalone: true,
  selector: 'sesa-citas-page',
  imports: [
    CommonModule,
    FormsModule,
    SesaSkeletonComponent,
    SesaEmptyStateComponent,
  ],
  templateUrl: './citas.page.html',
  styleUrl: './citas.page.scss',
})
export class CitasPageComponent implements OnInit, OnDestroy {
  private readonly citaService   = inject(CitaService);
  private readonly pacienteService = inject(PacienteService);
  private readonly personalService = inject(PersonalService);
  private readonly router          = inject(Router);
  private readonly toast           = inject(SesaToastService);
  private readonly confirmDialog   = inject(SesaConfirmDialogService);

  private readonly destroy$ = new Subject<void>();
  private readonly searchSubject$ = new Subject<string>();

  /* ── Tabs y wizard ─────────────────────────────────────────────────── */
  activeTab = signal<CitaTab>('nueva');
  wizardStep = signal<WizardStep>(1);

  /* ── Búsqueda de paciente ─────────────────────────────────────────── */
  patientSearchQuery  = '';
  patientResults      = signal<PacienteDto[]>([]);
  searchingPatient    = signal(false);
  showPatientDropdown = signal(false);
  selectedPatient     = signal<PacienteDto | null>(null);

  /* ── Especialidades ─────────────────────────────────────────────────── */
  readonly especialidades: Especialidad[] = [
    { id: 'Medicina general',  label: 'Med. General',  icon: '🩺', color: '#1f6ae1' },
    { id: 'Medicina interna',  label: 'Med. Interna',  icon: '🫀', color: '#7c3aed' },
    { id: 'Pediatría',         label: 'Pediatría',     icon: '🧸', color: '#0891b2' },
    { id: 'Ginecología',       label: 'Ginecología',   icon: '🌸', color: '#db2777' },
    { id: 'Cardiología',       label: 'Cardiología',   icon: '❤️', color: '#dc2626' },
    { id: 'Neurología',        label: 'Neurología',    icon: '🧠', color: '#7c3aed' },
    { id: 'Ortopedia',         label: 'Ortopedia',     icon: '🦴', color: '#d97706' },
    { id: 'Dermatología',      label: 'Dermatología',  icon: '🌿', color: '#16a34a' },
    { id: 'Oftalmología',      label: 'Oftalmología',  icon: '👁️', color: '#0e7490' },
    { id: 'Odontología',       label: 'Odontología',   icon: '🦷', color: '#4f46e5' },
    { id: 'Psicología',        label: 'Psicología',    icon: '💆', color: '#9333ea' },
    { id: 'Nutrición',         label: 'Nutrición',     icon: '🥗', color: '#15803d' },
  ];
  selectedEspecialidad = signal<string>('');

  /* ── Mapa especialidad → roles que deben atender ────────────────────
     Los roles coinciden con PersonalDto.rol (valor exacto en BD).
     Si la especialidad no está en el mapa se hace búsqueda textual
     como fallback y, si aún no hay resultados, se muestran todos.     */
  private readonly _espRolMap: Record<string, string[]> = {
    'Medicina general': ['MEDICO'],
    'Medicina interna': ['MEDICO'],
    'Pediatría':        ['MEDICO'],
    'Ginecología':      ['MEDICO'],
    'Cardiología':      ['MEDICO'],
    'Neurología':       ['MEDICO'],
    'Ortopedia':        ['MEDICO'],
    'Dermatología':     ['MEDICO'],
    'Oftalmología':     ['MEDICO'],
    'Odontología':      ['ODONTOLOGO'],
    'Psicología':       ['PSICOLOGO'],
    'Nutrición':        ['MEDICO', 'ENFERMERO', 'JEFE_ENFERMERIA'],
  };

  /* ── Profesionales ─────────────────────────────────────────────────── */
  todosProfesionales: PersonalDto[] = [];
  loadingProfesionales = signal(false);
  profesionalesFiltrados = signal<PersonalDto[]>([]);
  selectedProfesional    = signal<PersonalDto | null>(null);

  /* ── Calendario ─────────────────────────────────────────────────────── */
  calendarDate       = new Date();          // mes que se muestra
  selectedDay        = signal<Date | null>(null);
  selectedDayStr     = computed(() => {
    const d = this.selectedDay();
    if (!d) return '';
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  });
  calendarDays       = signal<(Date | null)[]>([]);
  citasByDay: Record<string, number> = {};   // 'YYYY-MM-DD' → cantidad de citas

  /* ── Slots de hora ─────────────────────────────────────────────────── */
  allSlots           = signal<TimeSlot[]>([]);
  selectedSlot       = signal<string>('');
  loadingSlots       = signal(false);

  /* ── Nota (opcional) ───────────────────────────────────────────────── */
  nota = '';

  /* ── Campos normativos Res. 2953/2014 ───────────────────────────────── */
  tipoCita         = signal<string>('PRIMERA_VEZ');
  numeroAutorizacion = '';
  duracionMin      = signal<number>(20);

  /* ── Estado de creación ─────────────────────────────────────────────── */
  savingCita    = signal(false);

  /* ── Lista de citas ────────────────────────────────────────────────── */
  listDate      = signal(this._today());
  citasLista    = signal<CitaDto[]>([]);
  loadingCitas  = signal(false);
  filterEstado  = signal<string>('');
  filterServicio   = signal<string>('');   // Especialidad
  filterProfesionalId = signal<number | null>(null);   // Especialista
  actioningId   = signal<number | null>(null);   // ID de cita en proceso de acción

  /* ── Reprogramar ─────────────────────────────────────────────────────── */
  showReprogramarModal = signal(false);
  reprogramarCitaTarget = signal<CitaDto | null>(null);
  reprogramarDate       = '';
  reprogramarTime       = '';
  savingReprogramar     = signal(false);
  normExpanded          = signal(false);   // acordeón campos normativos

  /* ── Timeline ─────────────────────────────────────────────────────────── */
  selectedCitaDetail = signal<CitaDto | null>(null);
  agendaView         = signal<'table' | 'timeline'>('table');
  readonly TL_SLOT_PX  = 40;    // px por slot de 30 min
  readonly TL_START    = 7;     // 7:00
  readonly TL_END      = 19;    // 19:00

  /* ── Computed ─────────────────────────────────────────────────────────── */
  citasListaFiltradas = computed(() => {
    let lista = this.citasLista();
    const estado = this.filterEstado();
    if (estado) lista = lista.filter(c => c.estado === estado);
    const servicio = this.filterServicio();
    if (servicio) lista = lista.filter(c => (c.servicio || '') === servicio);
    const profId = this.filterProfesionalId();
    if (profId != null && profId > 0) lista = lista.filter(c => c.profesionalId === profId);
    return lista;
  });

  totalCitas = computed(() => this.citasLista().length);
  citasPendientes = computed(() => this.citasLista().filter(c => c.estado === 'PENDIENTE' || c.estado === 'CONFIRMADA').length);
  citasAtendidas  = computed(() => this.citasLista().filter(c => c.estado === 'ATENDIDO').length);

  /** Profesionales para el desplegable Especialista en Agenda: filtrados por la especialidad seleccionada. */
  get profesionalesParaFiltroAgenda(): PersonalDto[] {
    const esp = this.filterServicio();
    if (!esp) return this.todosProfesionales;
    return this._getProfesionalesPorEspecialidad(esp);
  }

  wizardComplete = computed(() => {
    return !!this.selectedPatient() && !!this.selectedEspecialidad() &&
           !!this.selectedProfesional() && !!this.selectedDay() && !!this.selectedSlot();
  });

  listDateDisplay = computed(() => {
    const d = new Date(this.listDate() + 'T00:00:00');
    return d.toLocaleDateString('es-CO', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
  });

  timelineCitasLayout = computed(() => {
    const citas = this.citasListaFiltradas();
    const getCitaEnd = (c: CitaDto) => {
      if (!c.fechaHora) return new Date();
      const start = new Date(c.fechaHora);
      return new Date(start.getTime() + (c.duracionEstimadaMin ?? 30) * 60000);
    };
    return citas.map(c => {
      const startC = c.fechaHora ? new Date(c.fechaHora) : new Date();
      const endC   = getCitaEnd(c);
      const overlapping = citas.filter(o => {
        const s = o.fechaHora ? new Date(o.fechaHora) : new Date();
        const e = getCitaEnd(o);
        return startC < e && endC > s;
      });
      const col = overlapping.indexOf(c);
      return { cita: c, col, totalCols: overlapping.length };
    });
  });

  /** Profesionales únicos en las citas filtradas (para columnas del timeline). */
  timelineProfesionales = computed(() => {
    const seen = new Set<number>();
    const result: { id: number; nombre: string }[] = [];
    for (const c of this.citasListaFiltradas()) {
      const id = c.profesionalId ?? 0;
      if (!seen.has(id)) {
        seen.add(id);
        result.push({ id, nombre: c.profesionalNombre || 'Sin asignar' });
      }
    }
    return result;
  });

  /** Layout de citas por profesional con manejo de solapamientos. */
  citasLayoutByProfesional = computed(() => {
    const map = new Map<number, { cita: CitaDto; col: number; totalCols: number }[]>();
    const getCitaEnd = (c: CitaDto) => {
      if (!c.fechaHora) return new Date();
      const start = new Date(c.fechaHora);
      return new Date(start.getTime() + (c.duracionEstimadaMin ?? 30) * 60000);
    };
    for (const prof of this.timelineProfesionales()) {
      const profCitas = this.citasListaFiltradas().filter(c => (c.profesionalId ?? 0) === prof.id);
      map.set(prof.id, profCitas.map(c => {
        const startC = c.fechaHora ? new Date(c.fechaHora) : new Date();
        const endC   = getCitaEnd(c);
        const overlapping = profCitas.filter(o => {
          const s = o.fechaHora ? new Date(o.fechaHora) : new Date();
          return startC < getCitaEnd(o) && endC > s;
        });
        return { cita: c, col: overlapping.indexOf(c), totalCols: overlapping.length };
      }));
    }
    return map;
  });

  /* ─────────────────────────────────────────────────────────────────── */
  ngOnInit(): void {
    this._setupPatientSearch();
    this._loadProfesionales();
    this._buildCalendar();
    this.cargarCitasFecha(this.listDate());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /* ── Tabs ─────────────────────────────────────────────────────────── */
  setTab(tab: CitaTab): void {
    this.activeTab.set(tab);
    if (tab === 'lista') {
      this.cargarCitasFecha(this.listDate());
      this.selectedCitaDetail.set(null);
    }
  }

  /* ── Búsqueda de paciente ─────────────────────────────────────────── */
  private _setupPatientSearch(): void {
    this.searchSubject$.pipe(
      debounceTime(280),
      distinctUntilChanged(),
      takeUntil(this.destroy$),
    ).subscribe(query => this._doSearch(query));
  }

  /** Muestra los primeros pacientes al enfocar el input (sin escribir). */
  onPatientFocus(): void {
    if (this.selectedPatient()) return;
    if (this.patientResults().length === 0 && !this.patientSearchQuery.trim()) {
      this._doSearch('');
    }
    this.showPatientDropdown.set(true);
  }

  onPatientInput(): void {
    this.selectedPatient.set(null);
    this.showPatientDropdown.set(true);
    // Búsqueda vacía → recarga la lista completa
    this.searchSubject$.next(this.patientSearchQuery.trim());
  }

  private _doSearch(q: string): void {
    this.searchingPatient.set(true);
    this.showPatientDropdown.set(true);
    // Sin query → devuelve los primeros 20 pacientes para explorar
    this.pacienteService.list(0, 20, q || undefined).subscribe({
      next: (res) => {
        this.patientResults.set(res.content ?? []);
        this.searchingPatient.set(false);
      },
      error: () => {
        this.patientResults.set([]);
        this.searchingPatient.set(false);
      },
    });
  }

  selectPatient(p: PacienteDto): void {
    this.selectedPatient.set(p);
    this.patientSearchQuery = `${p.nombres} ${p.apellidos ?? ''} — ${p.documento}`;
    this.showPatientDropdown.set(false);
    if (this.wizardStep() === 1) {
      setTimeout(() => this.wizardStep.set(2), 180);
    }
  }

  clearPatient(): void {
    this.selectedPatient.set(null);
    this.patientSearchQuery = '';
    this.patientResults.set([]);
    this.wizardStep.set(1);
  }

  @HostListener('document:click', ['$event'])
  onDocClick(e: Event): void {
    const target = e.target as HTMLElement;
    if (!target.closest('.citas-patient-search')) {
      this.showPatientDropdown.set(false);
    }
    if (!target.closest('.citas-reprogramar-modal')) {
      // handled by backdrop click
    }
  }

  /* ── Profesionales ─────────────────────────────────────────────────── */
  private _loadProfesionales(): void {
    this.loadingProfesionales.set(true);
    this.personalService.list(0, 500).subscribe({
      next: (res) => {
        this.todosProfesionales = res.content ?? [];
        this.loadingProfesionales.set(false);
        this._filtrarProfesionales();
      },
      error: () => this.loadingProfesionales.set(false),
    });
  }

  private _filtrarProfesionales(): void {
    const esp = this.selectedEspecialidad();
    this.selectedProfesional.set(null);

    if (!esp) {
      this.profesionalesFiltrados.set(this.todosProfesionales);
      return;
    }

    // 1. Filtro por mapa especialidad → roles (solo profesionales de esa especialidad)
    const rolesEsp = this._espRolMap[esp] ?? [];
    if (rolesEsp.length > 0) {
      const porRol = this.todosProfesionales.filter(p => {
        const rolPrincipal = (p.rol ?? '').toUpperCase();
        const todosLosRoles = p.roles?.map(r => r.toUpperCase()) ?? [rolPrincipal];
        return rolesEsp.some(r => todosLosRoles.includes(r));
      });
      this.profesionalesFiltrados.set(porRol);
      return;
    }

    // 2. Especialidad sin mapa: fallback por coincidencia en nombre del rol
    const espLower = esp.toLowerCase();
    const porTexto = this.todosProfesionales.filter(p =>
      (p.rol || '').toLowerCase().includes(espLower)
    );
    this.profesionalesFiltrados.set(porTexto);
  }

  selectEspecialidad(id: string): void {
    this.selectedEspecialidad.set(id === this.selectedEspecialidad() ? '' : id);
    this._filtrarProfesionales();
    if (this.wizardStep() < 2) this.wizardStep.set(2);
  }

  /** Filtra profesionales por especialidad (misma lógica que wizard). Usado en Agenda y en Nueva Cita. */
  private _getProfesionalesPorEspecialidad(esp: string): PersonalDto[] {
    const rolesEsp = this._espRolMap[esp] ?? [];
    if (rolesEsp.length > 0) {
      return this.todosProfesionales.filter(p => {
        const rolPrincipal = (p.rol ?? '').toUpperCase();
        const todosLosRoles = p.roles?.map(r => r.toUpperCase()) ?? [rolPrincipal];
        return rolesEsp.some(r => todosLosRoles.includes(r));
      });
    }
    const espLower = esp.toLowerCase();
    return this.todosProfesionales.filter(p =>
      (p.rol || '').toLowerCase().includes(espLower)
    );
  }

  /** Al cambiar Especialidad en la pestaña Agenda, actualizar filtro y limpiar Especialista si ya no aplica. */
  onFilterServicioChange(value: string): void {
    this.filterServicio.set(value);
    const list = value ? this._getProfesionalesPorEspecialidad(value) : this.todosProfesionales;
    const id = this.filterProfesionalId();
    if (id != null && !list.some(p => p.id === id)) {
      this.filterProfesionalId.set(null);
    }
  }

  selectProfesional(p: PersonalDto): void {
    this.selectedProfesional.set(p.id === this.selectedProfesional()?.id ? null : p);
    if (this.selectedProfesional() && this.wizardStep() < 3) {
      setTimeout(() => this.wizardStep.set(3), 150);
    }
  }

  /* ── Calendario ─────────────────────────────────────────────────────── */
  private _buildCalendar(): void {
    const year  = this.calendarDate.getFullYear();
    const month = this.calendarDate.getMonth();
    const first = new Date(year, month, 1);
    const last  = new Date(year, month + 1, 0);
    const days: (Date | null)[] = [];

    // Padding al inicio (lunes = 0)
    const startDay = (first.getDay() + 6) % 7;
    for (let i = 0; i < startDay; i++) days.push(null);

    for (let d = 1; d <= last.getDate(); d++) {
      days.push(new Date(year, month, d));
    }
    this.calendarDays.set(days);
  }

  prevMonth(): void {
    this.calendarDate = new Date(this.calendarDate.getFullYear(), this.calendarDate.getMonth() - 1, 1);
    this._buildCalendar();
  }

  nextMonth(): void {
    this.calendarDate = new Date(this.calendarDate.getFullYear(), this.calendarDate.getMonth() + 1, 1);
    this._buildCalendar();
  }

  selectDay(day: Date): void {
    if (this._isPast(day)) return;
    this.selectedDay.set(day);
    this.selectedSlot.set('');
    this._loadSlots(day);
  }

  isDaySelected(day: Date): boolean {
    const s = this.selectedDay();
    return !!s && s.toDateString() === day.toDateString();
  }

  isDayToday(day: Date): boolean {
    return day.toDateString() === new Date().toDateString();
  }

  private _isPast(day: Date): boolean {
    const today = new Date(); today.setHours(0,0,0,0);
    return day < today;
  }

  isPast(day: Date): boolean { return this._isPast(day); }

  get calendarMonthLabel(): string {
    return this.calendarDate.toLocaleDateString('es-CO', { month: 'long', year: 'numeric' });
  }

  /* ── Time slots (solo por profesional: misma hora puede usarse en otra especialidad) ───────────────── */
  private _loadSlots(day: Date): void {
    this.loadingSlots.set(true);
    const dateStr = `${day.getFullYear()}-${String(day.getMonth() + 1).padStart(2, '0')}-${String(day.getDate()).padStart(2, '0')}`;
    const profId = this.selectedProfesional()?.id;

    this.citaService.list(dateStr, profId ?? undefined).subscribe({
      next: (citas) => {
        const booked = new Set((citas ?? []).map(c => this._extractTime(c.fechaHora)));
        const slots  = this._generateSlots(booked, day);
        this.allSlots.set(slots);
        this.loadingSlots.set(false);
      },
      error: () => {
        this.allSlots.set(this._generateSlots(new Set(), day));
        this.loadingSlots.set(false);
      },
    });
  }

  private _generateSlots(booked: Set<string>, day: Date): TimeSlot[] {
    const slots: TimeSlot[] = [];
    const now  = new Date();
    const isToday = day.toDateString() === now.toDateString();

    for (let h = 7; h < 19; h++) {
      for (const m of [0, 30]) {
        const time    = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
        const isPast  = isToday && (h < now.getHours() || (h === now.getHours() && m <= now.getMinutes()));
        const display = this._toAmPm(h, m);
        slots.push({ time, display, available: !booked.has(time) && !isPast });
      }
    }
    return slots;
  }

  private _extractTime(fechaHora?: string): string {
    if (!fechaHora) return '';
    const d = new Date(fechaHora);
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  }

  private _toAmPm(h: number, m: number): string {
    const suffix = h < 12 ? 'AM' : 'PM';
    const hour   = h % 12 || 12;
    return `${hour}:${String(m).padStart(2, '0')} ${suffix}`;
  }

  selectSlot(slot: TimeSlot): void {
    if (!slot.available) return;
    this.selectedSlot.set(slot.time === this.selectedSlot() ? '' : slot.time);
  }

  /* ── Crear cita ─────────────────────────────────────────────────────── */
  crearCita(): void {
    if (!this.wizardComplete()) return;

    const day    = this.selectedDay()!;
    const [h, m] = this.selectedSlot().split(':').map(Number);
    const fecha  = new Date(day.getFullYear(), day.getMonth(), day.getDate(), h, m);
    const fechaHora = `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}-${String(fecha.getDate()).padStart(2, '0')}T${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:00`;

    this.savingCita.set(true);
    this.citaService.create({
      pacienteId:          this.selectedPatient()!.id,
      profesionalId:       this.selectedProfesional()!.id,
      servicio:            this.selectedEspecialidad(),
      fechaHora,
      estado:              'PENDIENTE',
      notas:               this.nota || undefined,
      tipoCita:            this.tipoCita(),
      numeroAutorizacionEps: this.numeroAutorizacion || undefined,
      duracionEstimadaMin: this.duracionMin(),
    }).subscribe({
      next: () => {
        this.savingCita.set(false);
        this.toast.success(`Cita agendada para ${this._formatFullDate(fecha)} a las ${this.selectedSlot()}.`, 'Cita creada');
        this._resetWizard();
        this.cargarCitasFecha(this.listDate());
        this._loadSlots(day);
      },
      error: (err) => {
        this.savingCita.set(false);
        this.toast.error(err?.error?.error || 'No se pudo crear la cita.', 'Error');
      },
    });
  }

  private _resetWizard(): void {
    this.selectedPatient.set(null);
    this.patientSearchQuery = '';
    this.selectedEspecialidad.set('');
    this.selectedProfesional.set(null);
    this.selectedDay.set(null);
    this.selectedSlot.set('');
    this.nota = '';
    this.tipoCita.set('PRIMERA_VEZ');
    this.numeroAutorizacion = '';
    this.duracionMin.set(20);
    this.allSlots.set([]);
    this.wizardStep.set(1);
  }

  /* ── Lista de citas ────────────────────────────────────────────────── */
  cargarCitasFecha(fecha: string): void {
    this.loadingCitas.set(true);
    this.citaService.list(fecha).subscribe({
      next: (citas) => {
        this.citasLista.set((citas ?? []).sort((a, b) =>
          (a.fechaHora || '').localeCompare(b.fechaHora || '')
        ));
        this.loadingCitas.set(false);
      },
      error: (err) => {
        this.loadingCitas.set(false);
        this.toast.error(err?.error?.error || 'No se pudieron cargar las citas.', 'Error');
      },
    });
  }

  onListDateChange(fecha: string): void {
    this.listDate.set(fecha);
    this.cargarCitasFecha(fecha);
    this.selectedCitaDetail.set(null);
  }

  /* ── Acciones sobre citas ─────────────────────────────────────────── */
  async confirmarCita(cita: CitaDto): Promise<void> {
    this.actioningId.set(cita.id);
    this._updateEstado(cita, 'CONFIRMADA');
  }

  async marcarAtendido(cita: CitaDto): Promise<void> {
    this.actioningId.set(cita.id);
    this._updateEstado(cita, 'ATENDIDO');
  }

  async cancelarCita(cita: CitaDto): Promise<void> {
    const ok = await this.confirmDialog.confirm({
      title:         'Cancelar cita',
      message:       `¿Deseas cancelar la cita de ${cita.pacienteNombre} a las ${this.formatHora(cita.fechaHora)}?`,
      type:          'warning',
      confirmLabel:  'Cancelar cita',
      cancelLabel:   'Mantener',
    });
    if (!ok) return;
    this.actioningId.set(cita.id);
    this._updateEstado(cita, 'CANCELADA');
  }

  private _updateEstado(cita: CitaDto, estado: string): void {
    this.citaService.update(cita.id, {
      pacienteId:    cita.pacienteId,
      profesionalId: cita.profesionalId,
      servicio:      cita.servicio,
      fechaHora:     cita.fechaHora,
      estado,
      notas:         cita.notas,
    }).subscribe({
      next: () => {
        this.actioningId.set(null);
        const labels: Record<string, string> = {
          CONFIRMADA: 'confirmada', ATENDIDO: 'marcada como atendida', CANCELADA: 'cancelada',
        };
        this.toast.success(`Cita ${labels[estado] ?? estado.toLowerCase()}.`, 'Estado actualizado');
        this.cargarCitasFecha(this.listDate());
      },
      error: (err) => {
        this.actioningId.set(null);
        this.toast.error(err?.error?.error || 'No se pudo actualizar la cita.', 'Error');
      },
    });
  }

  /* ── Reprogramar ─────────────────────────────────────────────────────── */
  abrirReprogramar(cita: CitaDto): void {
    this.reprogramarCitaTarget.set(cita);
    this.reprogramarDate = (cita.fechaHora || '').slice(0, 10);
    this.reprogramarTime = this.formatHora(cita.fechaHora);
    if (this.reprogramarTime === '--:--') this.reprogramarTime = '';
    this.showReprogramarModal.set(true);
  }

  cerrarReprogramar(): void {
    this.showReprogramarModal.set(false);
    this.reprogramarCitaTarget.set(null);
    this.reprogramarDate = '';
    this.reprogramarTime = '';
  }

  confirmarReprogramar(): void {
    const cita = this.reprogramarCitaTarget();
    if (!cita || !this.reprogramarDate || !this.reprogramarTime) {
      this.toast.warning('Selecciona la nueva fecha y hora.', 'Campos requeridos');
      return;
    }
    const fechaHora = `${this.reprogramarDate}T${this.reprogramarTime}:00`;
    this.savingReprogramar.set(true);
    this.citaService.update(cita.id, {
      pacienteId:    cita.pacienteId,
      profesionalId: cita.profesionalId,
      servicio:      cita.servicio,
      fechaHora,
      estado:        'REPROGRAMADA',
      notas:         cita.notas,
    }).subscribe({
      next: () => {
        this.savingReprogramar.set(false);
        this.toast.success('Cita reprogramada correctamente.', 'Reprogramada');
        this.cerrarReprogramar();
        this.cargarCitasFecha(this.listDate());
      },
      error: (err) => {
        this.savingReprogramar.set(false);
        this.toast.error(err?.error?.error || 'No se pudo reprogramar.', 'Error');
      },
    });
  }

  /* ── Historia clínica ─────────────────────────────────────────────── */
  irHistoriaClinica(cita: CitaDto): void {
    this.router.navigate(['/historia-clinica'], { queryParams: { pacienteId: cita.pacienteId } });
  }

  /* ── Helpers ─────────────────────────────────────────────────────────── */
  formatHora(fechaHora?: string): string {
    if (!fechaHora) return '--:--';
    const d = new Date(fechaHora);
    if (isNaN(d.getTime())) return '--:--';
    return d.toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit', hour12: false });
  }

  private _formatFullDate(d: Date): string {
    return d.toLocaleDateString('es-CO', { weekday: 'long', day: 'numeric', month: 'long' });
  }

  private _today(): string {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }

  estadoClass(estado?: string): string {
    const map: Record<string, string> = {
      PENDIENTE:    'badge--warning',
      CONFIRMADA:   'badge--info',
      ATENDIDO:     'badge--success',
      CANCELADA:    'badge--danger',
      REPROGRAMADA: 'badge--secondary',
      NO_ASISTIO:   'badge--danger',
    };
    return map[(estado ?? '').toUpperCase()] ?? 'badge--secondary';
  }

  estadoLabel(estado?: string): string {
    const map: Record<string, string> = {
      PENDIENTE:    'Pendiente',
      CONFIRMADA:   'Confirmada',
      ATENDIDO:     'Atendido',
      CANCELADA:    'Cancelada',
      REPROGRAMADA: 'Reprogramada',
      NO_ASISTIO:   'No asistió',
    };
    return map[(estado ?? '').toUpperCase()] ?? (estado ?? 'Pendiente');
  }

  isActioning(id: number): boolean {
    return this.actioningId() === id;
  }

  get today(): string { return this._today(); }

  get profesionalesForStep2(): PersonalDto[] {
    return this.profesionalesFiltrados().slice(0, 20);
  }

  getEspecialidad(id: string): Especialidad | undefined {
    return this.especialidades.find(e => e.id === id);
  }

  patronNombre(p: PersonalDto): string {
    return [p.nombres, p.apellidos].filter(Boolean).join(' ');
  }

  fechaCompleta(d: Date): string {
    return d.toLocaleDateString('es-CO', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
  }

  slotGroups(): { label: string; slots: TimeSlot[] }[] {
    const all = this.allSlots();
    return [
      { label: 'Mañana',  slots: all.filter(s => parseInt(s.time) < 12) },
      { label: 'Tarde',   slots: all.filter(s => parseInt(s.time) >= 12 && parseInt(s.time) < 18) },
      { label: 'Noche',   slots: all.filter(s => parseInt(s.time) >= 18) },
    ].filter(g => g.slots.length > 0);
  }

  /* ── Timeline methods ─────────────────────────────────────────────── */
  selectCitaDetail(cita: CitaDto): void {
    this.selectedCitaDetail.set(
      this.selectedCitaDetail()?.id === cita.id ? null : cita
    );
  }

  closeCitaDetail(): void {
    this.selectedCitaDetail.set(null);
  }

  citaTopPx(cita: CitaDto): number {
    if (!cita.fechaHora) return 0;
    const d = new Date(cita.fechaHora);
    const mins = (d.getHours() - this.TL_START) * 60 + d.getMinutes();
    return Math.max(0, (mins / 30) * this.TL_SLOT_PX);
  }

  citaHeightPx(cita: CitaDto): number {
    const dur = cita.duracionEstimadaMin ?? 30;
    return Math.max((dur / 30) * this.TL_SLOT_PX - 4, Math.round(this.TL_SLOT_PX * 0.65));
  }

  currentTimePx(): number {
    const now = new Date();
    const mins = (now.getHours() - this.TL_START) * 60 + now.getMinutes();
    if (mins < 0 || mins > (this.TL_END - this.TL_START) * 60) return -1;
    return (mins / 30) * this.TL_SLOT_PX;
  }

  isListDateToday(): boolean {
    return this.listDate() === this._today();
  }

  private _dateToStr(d: Date): string {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }

  prevDay(): void {
    const d = new Date(this.listDate() + 'T00:00:00');
    d.setDate(d.getDate() - 1);
    const str = this._dateToStr(d);
    this.listDate.set(str);
    this.cargarCitasFecha(str);
    this.selectedCitaDetail.set(null);
  }

  nextDay(): void {
    const d = new Date(this.listDate() + 'T00:00:00');
    d.setDate(d.getDate() + 1);
    const str = this._dateToStr(d);
    this.listDate.set(str);
    this.cargarCitasFecha(str);
    this.selectedCitaDetail.set(null);
  }

  goToday(): void {
    const str = this._today();
    this.listDate.set(str);
    this.cargarCitasFecha(str);
    this.selectedCitaDetail.set(null);
  }

  get timelineHourRows(): { label: string; half: boolean }[] {
    const rows: { label: string; half: boolean }[] = [];
    for (let h = this.TL_START; h < this.TL_END; h++) {
      rows.push({ label: `${String(h).padStart(2, '0')}:00`, half: false });
      rows.push({ label: '', half: true });
    }
    return rows;
  }

  getLayoutForProfesional(profId: number): { cita: CitaDto; col: number; totalCols: number }[] {
    return this.citasLayoutByProfesional().get(profId) ?? [];
  }

  citaStateClass(estado?: string): string {
    const map: Record<string, string> = {
      PENDIENTE:    'tl-card--pending',
      CONFIRMADA:   'tl-card--confirmed',
      ATENDIDO:     'tl-card--attended',
      CANCELADA:    'tl-card--cancelled',
      REPROGRAMADA: 'tl-card--reprogrammed',
      NO_ASISTIO:   'tl-card--no-show',
    };
    return map[(estado ?? '').toUpperCase()] ?? 'tl-card--pending';
  }

  // ── Indicador de oportunidad Res. 2953/2014 ────────────────────────
  oportunidadClass(cita: CitaDto): string {
    if (cita.alertaOportunidad === true)  return 'oportunidad--alerta';
    if (cita.alertaOportunidad === false) return 'oportunidad--ok';
    return '';
  }

  oportunidadLabel(cita: CitaDto): string {
    if (cita.diasEspera == null) return '';
    const limite = cita.tipoCita === 'PRIMERA_VEZ' ? 3 : 15;
    const tipo   = cita.tipoCita === 'PRIMERA_VEZ' ? 'Primera vez' : 'Control/esp.';
    return `${tipo}: ${cita.diasEspera}d / límite ${limite}d`;
  }

  tipoCitaLabel(tipo?: string): string {
    const map: Record<string, string> = {
      PRIMERA_VEZ:  'Primera vez',
      CONTROL:      'Control',
      ESPECIALISTA: 'Especialista',
      URGENTE:      'Urgente',
    };
    return map[tipo ?? ''] ?? (tipo ?? '');
  }
}
