/**
 * Agenda de Turnos — IPS Nivel II Colombia.
 * Coordinador Médico: gestiona médicos.
 * Jefe de Enfermería: gestiona enfermeros y auxiliares.
 * Autor: Ing. J Sebastian Vargas S
 */
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';
import { AgendaService } from './agenda.service';
import { AgendaValidationService } from './agenda-validation.service';
import {
  Profesional, Turno, DiaCalendario, ResumenProfesional,
  TipoTurno, ServicioClinico, EstadoProgramacion, AlertaTipo,
  TURNO_CONFIG, SERVICIO_CONFIG, FESTIVOS_CO, DIAS_SEMANA, MESES_ES,
  LIMITES_LABORALES,
} from './agenda.models';

@Component({
  standalone:   true,
  selector:     'sesa-agenda-page',
  imports:      [CommonModule, FormsModule],
  templateUrl:  './agenda.page.html',
  styleUrl:     './agenda.page.scss',
})
export class AgendaPageComponent implements OnInit {
  /* ── Servicios ─────────────────────────────────────────────────── */
  private readonly auth       = inject(AuthService);
  private readonly agendaSvc  = inject(AgendaService);
  private readonly validar    = inject(AgendaValidationService);
  private readonly toast      = inject(SesaToastService);

  /* ── Estado del calendario ──────────────────────────────────────── */
  currentDate  = signal(new Date());
  turnos       = signal<Turno[]>([]);
  profesionales= signal<Profesional[]>([]);
  estadoMes    = signal<EstadoProgramacion>('BORRADOR');
  loading      = signal(false);

  /* ── Filtros ────────────────────────────────────────────────────── */
  filtroServicio    = signal<ServicioClinico | 'TODOS'>('TODOS');
  filtroTipo        = signal<TipoTurno | 'TODOS'>('TODOS');
  filtroProfesional = signal<number | 0>(0);

  /* ── Modal ──────────────────────────────────────────────────────── */
  showModal     = signal(false);
  modalFecha    = signal<Date | null>(null);
  modalTurno    = signal<Partial<Turno> | null>(null);
  modalEsNuevo  = signal(true);
  guardandoModal= signal(false);

  /* ── Drag & drop ────────────────────────────────────────────────── */
  draggingId = signal<string | null>(null);

  /* ── Vista ──────────────────────────────────────────────────────── */
  showDashboard = signal(true);

  toggleDashboard(): void {
    this.showDashboard.update(v => !v);
  }

  /* ── Catálogos expuestos al template ────────────────────────────── */
  readonly turnoConfig   = TURNO_CONFIG;
  readonly servicioConfig= SERVICIO_CONFIG;
  readonly diasSemana    = DIAS_SEMANA;
  readonly mesesEs       = MESES_ES;
  readonly tiposTurno    = Object.keys(TURNO_CONFIG) as TipoTurno[];
  readonly servicios     = Object.keys(SERVICIO_CONFIG) as ServicioClinico[];

  /* ── Rol del usuario ────────────────────────────────────────────── */
  get rol(): string {
    return this.auth.currentUser()?.role ?? '';
  }

  get esCoordinador(): boolean {
    return ['COORDINADOR_MEDICO', 'ADMIN', 'SUPERADMINISTRADOR'].includes(this.rol);
  }

  get esJefeEnfermeria(): boolean {
    return ['JEFE_ENFERMERIA', 'ADMIN', 'SUPERADMINISTRADOR'].includes(this.rol);
  }

  get puedeEditar(): boolean {
    const e = this.estadoMes();
    return e === 'BORRADOR' || e === 'EN_REVISION';
  }

  /* ── Computed: profesionales visibles según rol ─────────────────── */
  profVisibles = computed<Profesional[]>(() => {
    const todos = this.profesionales();
    if (this.esCoordinador && !this.esJefeEnfermeria) {
      return todos.filter((p) => p.tipo === 'MEDICO');
    }
    if (this.esJefeEnfermeria && !this.esCoordinador) {
      return todos.filter((p) => p.tipo !== 'MEDICO');
    }
    return todos; // ADMIN / SUPER
  });

  /* ── Computed: turnos filtrados ─────────────────────────────────── */
  turnosFiltrados = computed<Turno[]>(() => {
    const visibleIds = new Set(this.profVisibles().map((p) => p.id));
    return this.turnos().filter((t) => {
      if (!visibleIds.has(t.profesionalId)) return false;
      if (this.filtroServicio() !== 'TODOS' && t.servicio !== this.filtroServicio()) return false;
      if (this.filtroTipo()     !== 'TODOS' && t.tipo     !== this.filtroTipo())     return false;
      if (this.filtroProfesional() !== 0    && t.profesionalId !== this.filtroProfesional()) return false;
      return true;
    });
  });

  /* ── Computed: grilla del mes ───────────────────────────────────── */
  calGrid = computed<DiaCalendario[]>(() => this.buildGrid());

  /* ── Computed: resúmenes ────────────────────────────────────────── */
  resumenes = computed<ResumenProfesional[]>(() =>
    this.profVisibles().map((p) => {
      const mine = this.turnos().filter((t) => t.profesionalId === p.id);
      const h    = this.validar.resumenHoras(p.id, mine);
      return {
        profesional:    p,
        horasTotales:   h.horasMensuales,
        horasNocturnas: h.horasNocturnas,
        horasFestivos:  h.horasFestivos,
        turnosCount:    mine.length,
        alerta:         this.validar.alertaGlobal(p.id, mine),
      } satisfies ResumenProfesional;
    }),
  );

  /* ── Computed: stats del dashboard ─────────────────────────────── */
  dashStats = computed(() => {
    const r = this.resumenes();
    return {
      totalProfesionales: r.length,
      totalTurnos:        this.turnosFiltrados().length,
      totalHoras:         r.reduce((s, x) => s + x.horasTotales, 0),
      conflictos:         r.filter((x) => x.alerta === 'CONFLICTO').length,
      advertencias:       r.filter((x) => x.alerta === 'ADVERTENCIA').length,
      horasNocturnas:     r.reduce((s, x) => s + x.horasNocturnas, 0),
      horasFestivos:      r.reduce((s, x) => s + x.horasFestivos, 0),
    };
  });

  /* ── Init ───────────────────────────────────────────────────────── */
  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    const y = this.currentDate().getFullYear();
    const m = this.currentDate().getMonth() + 1; // backend: 1-based
    this.agendaSvc.getProfesionales().subscribe((p) => this.profesionales.set(p));
    this.agendaSvc.getTurnos(y, m).subscribe({
      next: (t) => {
        // Re-validar alertas locales con los datos recibidos
        let resultado = t;
        const ids = [...new Set(t.map((x) => x.profesionalId))];
        ids.forEach((pid) => { resultado = this.validar.revalidarProfesional(pid, resultado); });
        this.turnos.set(resultado);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
    this.agendaSvc.getEstado(y, m).subscribe({
      next:  (e) => this.estadoMes.set(e),
      error: () => this.estadoMes.set('BORRADOR'),
    });
  }

  /* ── Navegación mes ─────────────────────────────────────────────── */
  prevMes(): void {
    const d = new Date(this.currentDate());
    d.setMonth(d.getMonth() - 1);
    this.currentDate.set(d);
    this.loadData();
  }

  nextMes(): void {
    const d = new Date(this.currentDate());
    d.setMonth(d.getMonth() + 1);
    this.currentDate.set(d);
    this.loadData();
  }

  get mesLabel(): string {
    const d = this.currentDate();
    return `${MESES_ES[d.getMonth()]} ${d.getFullYear()}`;
  }

  /* ── Grilla del mes ─────────────────────────────────────────────── */
  private buildGrid(): DiaCalendario[] {
    const d   = this.currentDate();
    const y   = d.getFullYear();
    const m   = d.getMonth();
    const pri = new Date(y, m, 1);
    const dow = pri.getDay(); // 0=dom
    const ini = new Date(pri);
    ini.setDate(1 - (dow === 0 ? 6 : dow - 1)); // lunes anterior

    const hoy = new Date();
    const dias: DiaCalendario[] = [];

    for (let i = 0; i < 42; i++) {
      const fecha  = new Date(ini);
      fecha.setDate(ini.getDate() + i);
      const fStr   = this.fmtDate(fecha);
      const festivo= FESTIVOS_CO.find((f) => f.fecha === fStr);
      const turnosDia = this.turnosFiltrados().filter((t) => {
        const tf = new Date(t.fechaInicio);
        return tf.getFullYear() === fecha.getFullYear()
            && tf.getMonth()    === fecha.getMonth()
            && tf.getDate()     === fecha.getDate();
      });
      dias.push({
        fecha,
        esHoy:         this.mismaFecha(fecha, hoy),
        esMesActual:   fecha.getMonth() === m,
        esFestivo:     !!festivo,
        festivoNombre: festivo?.nombre,
        turnos:        turnosDia,
      });
    }
    return dias;
  }

  /* ── Modal: abrir ───────────────────────────────────────────────── */
  abrirModalNuevo(dia: DiaCalendario): void {
    if (!this.puedeEditar) return;
    this.modalEsNuevo.set(true);
    this.modalFecha.set(dia.fecha);
    this.modalTurno.set({
      profesionalId: this.filtroProfesional() || (this.profVisibles()[0]?.id ?? 0),
      tipo:          'TURNO_8H',
      servicio:      'URGENCIAS',
      estado:        'BORRADOR',
    });
    this.showModal.set(true);
  }

  abrirModalEditar(turno: Turno, ev: Event): void {
    ev.stopPropagation();
    if (!this.puedeEditar) return;
    this.modalEsNuevo.set(false);
    this.modalFecha.set(new Date(turno.fechaInicio));
    this.modalTurno.set({ ...turno });
    this.showModal.set(true);
  }

  cerrarModal(): void {
    this.showModal.set(false);
    this.modalTurno.set(null);
  }

  /* ── Modal: form ────────────────────────────────────────────────── */
  get formTurno(): Partial<Turno> {
    return this.modalTurno() ?? {};
  }

  setFormField(field: keyof Turno, value: unknown): void {
    this.modalTurno.update((t) => ({ ...t, [field]: value }));
  }

  guardarTurno(): void {
    const ft    = this.formTurno;
    const fecha = this.modalFecha()!;
    if (!ft.profesionalId || !ft.tipo || !ft.servicio) {
      this.toast.warning('Completa todos los campos obligatorios.', 'Campos requeridos');
      return;
    }

    const fechaStr = this.fmtDate(fecha); // yyyy-MM-dd
    const payload = {
      personalId: ft.profesionalId!,
      tipoTurno:  ft.tipo!,
      servicio:   ft.servicio!,
      fecha:      fechaStr,
      estado:     ft.estado ?? 'BORRADOR',
      notas:      ft.notas,
    };

    this.guardandoModal.set(true);
    const obs = this.modalEsNuevo()
      ? this.agendaSvc.guardarTurno(payload)
      : this.agendaSvc.actualizarTurno(ft.id!, payload);

    obs.subscribe({
      next: (saved) => {
        const prev = this.turnos().filter((t) => t.id !== saved.id);
        let nuevo  = [...prev, saved];
        nuevo = this.validar.revalidarProfesional(saved.profesionalId, nuevo);
        this.turnos.set(nuevo);
        this.guardandoModal.set(false);
        this.cerrarModal();
        this.toast.success(
          this.modalEsNuevo() ? 'Turno creado correctamente.' : 'Turno actualizado.',
          '¡Listo!'
        );
      },
      error: (err) => {
        this.guardandoModal.set(false);
        const msg = err?.error?.message ?? 'No se pudo guardar el turno.';
        this.toast.error(msg, 'Error de validación');
      },
    });
  }

  eliminarTurno(turno: Turno, ev: Event): void {
    ev.stopPropagation();
    if (!this.puedeEditar) return;
    this.agendaSvc.eliminarTurno(turno.id).subscribe(() => {
      let nuevo = this.turnos().filter((t) => t.id !== turno.id);
      nuevo = this.validar.revalidarProfesional(turno.profesionalId, nuevo);
      this.turnos.set(nuevo);
      this.toast.success('Turno eliminado.', 'Eliminado');
    });
  }

  /* ── Flujo de aprobación ────────────────────────────────────────── */
  enviarARevision(): void {
    const d = this.currentDate();
    this.agendaSvc.enviarARevision(d.getFullYear(), d.getMonth() + 1).subscribe({
      next: () => {
        this.estadoMes.set('EN_REVISION');
        this.toast.info('Programación enviada para revisión y aprobación.', 'En revisión');
      },
      error: () => this.toast.error('No se pudo enviar a revisión.', 'Error'),
    });
  }

  aprobarMes(): void {
    if (!this.esCoordinador) return;
    const d = this.currentDate();
    this.agendaSvc.aprobarProgramacion(d.getFullYear(), d.getMonth() + 1).subscribe({
      next: () => {
        this.estadoMes.set('APROBADO');
        this.toast.success(`Programación de ${this.mesLabel} aprobada.`, 'Aprobado');
      },
      error: () => this.toast.error('No se pudo aprobar la programación.', 'Error'),
    });
  }

  cerrarMes(): void {
    if (!this.esCoordinador) return;
    const d = this.currentDate();
    this.agendaSvc.cerrarProgramacion(d.getFullYear(), d.getMonth() + 1).subscribe({
      next: () => {
        this.estadoMes.set('CERRADO');
        this.toast.warning(`Programación de ${this.mesLabel} cerrada. Sin más ediciones.`, 'Cerrado');
      },
      error: () => this.toast.error('No se pudo cerrar la programación.', 'Error'),
    });
  }

  /* ── Exportar ───────────────────────────────────────────────────── */
  exportarExcel(): void {
    this.toast.info('Exportación a Excel en desarrollo. Disponible en próxima versión.', 'Próximamente');
  }

  exportarPDF(): void {
    this.toast.info('Exportación a PDF en desarrollo. Disponible en próxima versión.', 'Próximamente');
  }

  /* ── Drag & drop (HTML5 nativo) ─────────────────────────────────── */
  onDragStart(turno: Turno, ev: DragEvent): void {
    if (!this.puedeEditar) { ev.preventDefault(); return; }
    this.draggingId.set(turno.id);
    ev.dataTransfer!.effectAllowed = 'move';
  }

  onDragOver(ev: DragEvent): void {
    if (this.draggingId()) { ev.preventDefault(); ev.dataTransfer!.dropEffect = 'move'; }
  }

  onDrop(dia: DiaCalendario, ev: DragEvent): void {
    ev.preventDefault();
    const id = this.draggingId();
    if (!id) return;
    const turno = this.turnos().find((t) => t.id === id);
    if (!turno) { this.draggingId.set(null); return; }

    const fechaStr = this.fmtDate(dia.fecha);
    this.agendaSvc.moverFecha(id, fechaStr).subscribe({
      next: (actualizado) => {
        let nuevo = this.turnos().map((t) => (t.id === id ? actualizado : t));
        nuevo = this.validar.revalidarProfesional(turno.profesionalId, nuevo);
        this.turnos.set(nuevo);
        this.draggingId.set(null);
        this.toast.success('Turno movido correctamente.', 'Movido');
      },
      error: (err) => {
        this.draggingId.set(null);
        const msg = err?.error?.message ?? 'No se pudo mover el turno.';
        this.toast.error(msg, 'Error de validación');
      },
    });
  }

  onDragEnd(): void { this.draggingId.set(null); }

  /* ── Helpers expuestos al template ──────────────────────────────── */
  getProfesional(id: number): Profesional | undefined {
    return this.profesionales().find((p) => p.id === id);
  }

  nombreCorto(p: Profesional): string {
    return `${p.nombre.split(' ')[0]} ${p.apellido.split(' ')[0]}`;
  }

  alertaClase(a: AlertaTipo): string {
    return { CONFLICTO: 'alerta-rojo', ADVERTENCIA: 'alerta-amarillo', OK: 'alerta-verde' }[a];
  }

  alertaIcon(a: AlertaTipo): string {
    return { CONFLICTO: '🔴', ADVERTENCIA: '🟡', OK: '🟢' }[a];
  }

  porcentajeHoras(total: number): number {
    return Math.min(100, Math.round((total / LIMITES_LABORALES.MAX_HORAS_MENSUALES) * 100));
  }

  barColor(total: number): string {
    const p = this.porcentajeHoras(total);
    if (p >= 95) return '#ef4444';
    if (p >= 80) return '#f59e0b';
    return '#22c55e';
  }

  estadoBadge(e: EstadoProgramacion): { label: string; cls: string } {
    const MAP: Record<EstadoProgramacion, { label: string; cls: string }> = {
      BORRADOR:    { label: 'Borrador',     cls: 'badge-draft'    },
      EN_REVISION: { label: 'En revisión',  cls: 'badge-review'   },
      APROBADO:    { label: 'Aprobado',     cls: 'badge-approved' },
      CERRADO:     { label: 'Cerrado',      cls: 'badge-closed'   },
    };
    return MAP[e];
  }

  semanasGrid(): DiaCalendario[][] {
    const g = this.calGrid();
    const weeks: DiaCalendario[][] = [];
    for (let i = 0; i < g.length; i += 7) weeks.push(g.slice(i, i + 7));
    return weeks;
  }

  private fmtDate(d: Date): string {
    return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
  }

  private mismaFecha(a: Date, b: Date): boolean {
    return a.getFullYear() === b.getFullYear() &&
           a.getMonth()    === b.getMonth()    &&
           a.getDate()     === b.getDate();
  }

  trackByDia(_: number, d: DiaCalendario): string { return d.fecha.toISOString(); }
  trackByTurno(_: number, t: Turno): string        { return t.id; }
}
