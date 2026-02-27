/**
 * Urgencias — Workbench Clínico Multi-Rol
 * Autor: Ing. J Sebastian Vargas S
 */

import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { UrgenciaRegistroService, UrgenciaRegistroDto } from '../../core/services/urgencia-registro.service';
import { EvolucionService, EvolucionDto, EvolucionRequestDto } from '../../core/services/evolucion.service';
import { NotaEnfermeriaService, NotaEnfermeriaDto, NotaEnfermeriaRequestDto } from '../../core/services/nota-enfermeria.service';
import { PacienteService, PacienteDto } from '../../core/services/paciente.service';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';

export type NivelTriage = 'I' | 'II' | 'III' | 'IV' | 'V' | 'TODOS';
export type EstadoFiltro = 'TODOS' | 'EN_ESPERA' | 'EN_ATENCION' | 'EN_OBSERVACION' | 'ALTA' | 'HOSPITALIZADO' | 'REFERIDO';

// Tiempos máximos de espera en minutos según Res. 5596/2015
const TRIAGE_LIMITES: Record<string, number> = { I: 0, II: 30, III: 60, IV: 120, V: 240 };

@Component({
  standalone: true,
  selector: 'sesa-urgencias-page',
  imports: [CommonModule, FormsModule],
  templateUrl: './urgencias.page.html',
  styleUrl: './urgencias.page.scss',
})
export class UrgenciasPageComponent implements OnInit, OnDestroy {
  private readonly urgenciaService = inject(UrgenciaRegistroService);
  private readonly evolucionService = inject(EvolucionService);
  private readonly notaService = inject(NotaEnfermeriaService);
  private readonly pacienteService = inject(PacienteService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(SesaToastService);

  private refreshTimer: ReturnType<typeof setInterval> | null = null;

  // ── Signals principales ─────────────────────────────────────────────────
  urgencias = signal<UrgenciaRegistroDto[]>([]);
  seleccionada = signal<UrgenciaRegistroDto | null>(null);
  tabPanel = signal<'medico' | 'enfermeria'>('medico');
  mostrarIngreso = signal(false);
  autoRefresh = signal(true);
  cargando = signal(false);
  guardando = signal(false);

  // Filtros
  filtroTriage = signal<NivelTriage>('TODOS');
  filtroEstado = signal<EstadoFiltro>('TODOS');
  busqueda = signal('');

  // Evoluciones y notas
  evoluciones = signal<EvolucionDto[]>([]);
  notas = signal<NotaEnfermeriaDto[]>([]);

  // Formulario nueva evolución SOAP estructurada
  formEvolucion = signal({
    subjetivo: '',
    objetivo: '',
    analisis: '',
    plan: '',
    codigoCie10: '',
  });
  // Formulario nueva nota
  formNota = signal({ nota: '' });

  // Formulario nuevo ingreso
  pacienteQuery = signal('');
  pacientesBusqueda = signal<PacienteDto[]>([]);
  pacienteSeleccionado = signal<PacienteDto | null>(null);
  buscandoPaciente = signal(false);
  formIngreso = signal({
    nivelTriage: 'III' as string,
    tipoLlegada: '',
    motivoConsulta: '',
    observaciones: '',
    svPresionArterial: '',
    svFrecuenciaCardiaca: '',
    svFrecuenciaRespiratoria: '',
    svTemperatura: '',
    svSaturacionO2: '',
    svPeso: '',
    svDolorEva: '',
    glasgowOcular: '' as string,
    glasgowVerbal: '' as string,
    glasgowMotor: '' as string,
  });

  // ── Computed: roles ──────────────────────────────────────────────────────
  readonly rol = computed(() => this.auth.currentUser()?.role ?? '');
  readonly esMedico = computed(() => ['MEDICO', 'COORDINADOR_MEDICO', 'ODONTOLOGO'].includes(this.rol()));
  readonly esJefeEnfermeria = computed(() => this.rol() === 'JEFE_ENFERMERIA');
  readonly esEnfermeria = computed(() => ['JEFE_ENFERMERIA', 'ENFERMERO', 'AUXILIAR_ENFERMERIA'].includes(this.rol()));
  readonly esAuxiliar = computed(() => this.rol() === 'AUXILIAR_ENFERMERIA');
  readonly puedeCrearEvolucion = computed(() => this.esMedico() || ['ADMIN', 'SUPERADMINISTRADOR'].includes(this.rol()));
  readonly puedeCrearNota = computed(() => this.esEnfermeria() || ['ADMIN', 'SUPERADMINISTRADOR'].includes(this.rol()));
  readonly puedeCambiarEstado = computed(() => this.esMedico() || ['JEFE_ENFERMERIA', 'ENFERMERO', 'ADMIN', 'SUPERADMINISTRADOR'].includes(this.rol()));

  // ── Computed: listas filtradas ───────────────────────────────────────────
  readonly urgenciasFiltradas = computed(() => {
    const q = this.busqueda().toLowerCase().trim();
    return this.urgencias().filter((u) => {
      const matchTriage = this.filtroTriage() === 'TODOS' || u.nivelTriage === this.filtroTriage();
      const matchEstado = this.filtroEstado() === 'TODOS' || u.estado === this.filtroEstado();
      const matchQ = !q ||
        (u.pacienteNombre ?? '').toLowerCase().includes(q) ||
        (u.pacienteDocumento ?? '').toLowerCase().includes(q);
      return matchTriage && matchEstado && matchQ;
    });
  });

  // ── Computed: stats ──────────────────────────────────────────────────────
  readonly statsTotal = computed(() => this.urgencias().length);
  readonly statsEspera = computed(() => this.urgencias().filter((u) => u.estado === 'EN_ESPERA').length);
  readonly statsAtencion = computed(() => this.urgencias().filter((u) => u.estado === 'EN_ATENCION').length);
  readonly statsCriticos = computed(() => this.urgencias().filter((u) => u.nivelTriage === 'I' || u.nivelTriage === 'II').length);

  // ── Lifecycle ────────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.tabPanel.set(this.esMedico() ? 'medico' : 'enfermeria');
    this.cargarUrgencias();
    this.iniciarAutoRefresh();
  }

  ngOnDestroy(): void {
    if (this.refreshTimer) clearInterval(this.refreshTimer);
  }

  // ── Carga de datos ───────────────────────────────────────────────────────
  cargarUrgencias(): void {
    this.cargando.set(true);
    this.urgenciaService.list().subscribe({
      next: (res) => {
        this.urgencias.set(res?.content ?? []);
        this.cargando.set(false);
      },
      error: (err) => {
        this.toast.error(err?.error?.error || 'Error al cargar urgencias', 'Error');
        this.cargando.set(false);
      },
    });
  }

  private iniciarAutoRefresh(): void {
    this.refreshTimer = setInterval(() => {
      if (this.autoRefresh()) this.cargarUrgencias();
    }, 30000);
  }

  // ── Selección de urgencia ────────────────────────────────────────────────
  seleccionar(u: UrgenciaRegistroDto): void {
    this.seleccionada.set(u);
    this.evoluciones.set([]);
    this.notas.set([]);
    this.formEvolucion.set({ subjetivo: '', objetivo: '', analisis: '', plan: '', codigoCie10: '' });
    this.formNota.set({ nota: '' });

    if (u.atencionId) {
      this.cargarEvoluciones(u.atencionId);
      this.cargarNotas(u.atencionId);
    }
  }

  private cargarEvoluciones(atencionId: number): void {
    this.evolucionService.listarPorAtencion(atencionId).subscribe({
      next: (list) => this.evoluciones.set(list),
      error: () => {},
    });
  }

  private cargarNotas(atencionId: number): void {
    this.notaService.listarPorAtencion(atencionId).subscribe({
      next: (list) => this.notas.set(list),
      error: () => {},
    });
  }

  // ── Cambio de estado ─────────────────────────────────────────────────────
  cambiarEstado(nuevoEstado: string): void {
    const sel = this.seleccionada();
    if (!sel) return;
    this.guardando.set(true);
    this.urgenciaService.cambiarEstado(sel.id, nuevoEstado).subscribe({
      next: (updated) => {
        this.urgencias.update((list) =>
          list.map((u) => (u.id === updated.id ? updated : u))
        );
        this.seleccionada.set(updated);
        this.guardando.set(false);
        this.toast.success(`Estado cambiado a ${this.estadoLabel(nuevoEstado)}`, 'Urgencias');
      },
      error: (err) => {
        this.toast.error(err?.error?.error || 'Error al cambiar estado', 'Error');
        this.guardando.set(false);
      },
    });
  }

  // ── Evoluciones SOAP ──────────────────────────────────────────────────────
  guardarEvolucion(): void {
    const sel = this.seleccionada();
    const form = this.formEvolucion();
    if (!sel?.atencionId || !form.subjetivo.trim()) {
      this.toast.error('Se requiere atención vinculada y descripción subjetiva', 'Campos requeridos');
      return;
    }
    const notaEstructurada = [
      form.subjetivo.trim() ? `S — Subjetivo:\n${form.subjetivo.trim()}` : '',
      form.objetivo.trim() ? `O — Objetivo:\n${form.objetivo.trim()}` : '',
      form.analisis.trim() ? `A — Análisis/Diagnóstico${form.codigoCie10 ? ' [' + form.codigoCie10 + ']' : ''}:\n${form.analisis.trim()}` : '',
      form.plan.trim() ? `P — Plan:\n${form.plan.trim()}` : '',
    ].filter(Boolean).join('\n\n');

    const user = this.auth.currentUser();
    const dto: EvolucionRequestDto = {
      atencionId: sel.atencionId,
      notaEvolucion: notaEstructurada,
    };
    if (user?.userId) dto.profesionalId = user.userId;
    this.guardando.set(true);
    this.evolucionService.crear(dto).subscribe({
      next: (nueva) => {
        this.evoluciones.update((list) => [nueva, ...list]);
        this.formEvolucion.set({ subjetivo: '', objetivo: '', analisis: '', plan: '', codigoCie10: '' });
        this.guardando.set(false);
        this.toast.success('Evolución SOAP registrada', 'Historia Clínica');
      },
      error: (err) => {
        this.toast.error(err?.error?.error || 'Error al registrar evolución', 'Error');
        this.guardando.set(false);
      },
    });
  }

  // ── Notas de enfermería ──────────────────────────────────────────────────
  guardarNota(): void {
    const sel = this.seleccionada();
    const form = this.formNota();
    if (!sel?.atencionId || !form.nota.trim()) {
      this.toast.error('Se requiere atención vinculada y nota', 'Campos requeridos');
      return;
    }
    const user = this.auth.currentUser();
    const dto: NotaEnfermeriaRequestDto = {
      atencionId: sel.atencionId,
      nota: form.nota.trim(),
    };
    if (user?.userId) dto.profesionalId = user.userId;
    this.guardando.set(true);
    this.notaService.crear(dto).subscribe({
      next: (nueva) => {
        this.notas.update((list) => [nueva, ...list]);
        this.formNota.set({ nota: '' });
        this.guardando.set(false);
        this.toast.success('Nota registrada', 'Enfermería');
      },
      error: (err) => {
        this.toast.error(err?.error?.error || 'Error al registrar nota', 'Error');
        this.guardando.set(false);
      },
    });
  }

  // ── Nuevo ingreso ────────────────────────────────────────────────────────
  buscarPaciente(): void {
    const q = this.pacienteQuery().trim();
    if (q.length < 2) return;
    this.buscandoPaciente.set(true);
    this.pacienteService.list(0, 10, q).subscribe({
      next: (res) => {
        this.pacientesBusqueda.set(res.content);
        this.buscandoPaciente.set(false);
      },
      error: () => this.buscandoPaciente.set(false),
    });
  }

  seleccionarPaciente(p: PacienteDto): void {
    this.pacienteSeleccionado.set(p);
    this.pacientesBusqueda.set([]);
    this.pacienteQuery.set(`${p.nombres} ${p.apellidos ?? ''} — ${p.documento}`.trim());
  }

  abrirIngreso(): void {
    this.pacienteSeleccionado.set(null);
    this.pacientesBusqueda.set([]);
    this.pacienteQuery.set('');
    this.formIngreso.set({
      nivelTriage: 'III', tipoLlegada: '', motivoConsulta: '', observaciones: '',
      svPresionArterial: '', svFrecuenciaCardiaca: '', svFrecuenciaRespiratoria: '',
      svTemperatura: '', svSaturacionO2: '', svPeso: '', svDolorEva: '',
      glasgowOcular: '', glasgowVerbal: '', glasgowMotor: '',
    });
    this.mostrarIngreso.set(true);
  }

  crearIngreso(): void {
    const paciente = this.pacienteSeleccionado();
    const form = this.formIngreso();
    if (!paciente) {
      this.toast.error('Seleccione un paciente', 'Ingreso');
      return;
    }
    if (!form.motivoConsulta?.trim()) {
      this.toast.error('El motivo de consulta es obligatorio (Res. 5596/2015)', 'Campo requerido');
      return;
    }
    this.guardando.set(true);
    this.urgenciaService.crear({
      pacienteId: paciente.id,
      nivelTriage: form.nivelTriage,
      tipoLlegada: form.tipoLlegada || undefined,
      motivoConsulta: form.motivoConsulta.trim(),
      observaciones: form.observaciones || undefined,
      estado: 'EN_ESPERA',
      svPresionArterial: form.svPresionArterial || undefined,
      svFrecuenciaCardiaca: form.svFrecuenciaCardiaca || undefined,
      svFrecuenciaRespiratoria: form.svFrecuenciaRespiratoria || undefined,
      svTemperatura: form.svTemperatura || undefined,
      svSaturacionO2: form.svSaturacionO2 || undefined,
      svPeso: form.svPeso || undefined,
      svDolorEva: form.svDolorEva || undefined,
      glasgowOcular: form.glasgowOcular ? Number(form.glasgowOcular) : undefined,
      glasgowVerbal: form.glasgowVerbal ? Number(form.glasgowVerbal) : undefined,
      glasgowMotor: form.glasgowMotor ? Number(form.glasgowMotor) : undefined,
    }).subscribe({
      next: (nueva) => {
        this.urgencias.update((list) => [nueva, ...list]);
        this.mostrarIngreso.set(false);
        this.guardando.set(false);
        this.toast.success(`Paciente ${nueva.pacienteNombre} ingresado`, 'Urgencias');
      },
      error: (err) => {
        this.toast.error(err?.error?.error || 'Error al crear ingreso', 'Error');
        this.guardando.set(false);
      },
    });
  }

  // ── Helpers para evitar arrow functions en templates ────────────────────
  setBusqueda(val: string): void { this.busqueda.set(val); }
  setFiltroTriage(val: NivelTriage): void { this.filtroTriage.set(val); }
  setFiltroEstado(val: EstadoFiltro): void { this.filtroEstado.set(val); }
  setTabPanel(val: 'medico' | 'enfermeria'): void { this.tabPanel.set(val); }
  setAutoRefresh(val: boolean): void { this.autoRefresh.set(val); }
  setEvolucionSubjetivo(val: string): void { this.formEvolucion.update((f) => ({ ...f, subjetivo: val })); }
  setEvolucionObjetivo(val: string): void { this.formEvolucion.update((f) => ({ ...f, objetivo: val })); }
  setEvolucionAnalisis(val: string): void { this.formEvolucion.update((f) => ({ ...f, analisis: val })); }
  setEvolucionPlan(val: string): void { this.formEvolucion.update((f) => ({ ...f, plan: val })); }
  setEvolucionCie10(val: string): void { this.formEvolucion.update((f) => ({ ...f, codigoCie10: val.toUpperCase() })); }
  setNotaEnfermeria(val: string): void { this.formNota.update((f) => ({ ...f, nota: val })); }
  setPacienteQuery(val: string): void { this.pacienteQuery.set(val); }
  setNivelTriage(val: string): void { this.formIngreso.update((f) => ({ ...f, nivelTriage: val })); }
  setTipoLlegada(val: string): void { this.formIngreso.update((f) => ({ ...f, tipoLlegada: val })); }
  setMotivoConsulta(val: string): void { this.formIngreso.update((f) => ({ ...f, motivoConsulta: val })); }
  setObservacionesIngreso(val: string): void { this.formIngreso.update((f) => ({ ...f, observaciones: val })); }
  setSvField(field: string, val: string): void { this.formIngreso.update((f) => ({ ...f, [field]: val })); }

  // ── UX helpers ───────────────────────────────────────────────────────────
  triageLabel(nivel: string): string {
    const map: Record<string, string> = { I: 'Rojo', II: 'Naranja', III: 'Amarillo', IV: 'Verde', V: 'Azul' };
    return map[nivel] ?? nivel;
  }

  triageClass(nivel: string): string {
    const map: Record<string, string> = {
      I: 'triage-i', II: 'triage-ii', III: 'triage-iii', IV: 'triage-iv', V: 'triage-v',
    };
    return map[nivel] ?? '';
  }

  estadoLabel(estado: string): string {
    const map: Record<string, string> = {
      EN_ESPERA: 'En Espera', EN_ATENCION: 'En Atención',
      EN_OBSERVACION: 'En Observación', ALTA: 'Alta',
      HOSPITALIZADO: 'Hospitalizado', REFERIDO: 'Referido',
    };
    return map[estado] ?? estado;
  }

  estadoClass(estado: string): string {
    const map: Record<string, string> = {
      EN_ESPERA: 'estado-espera', EN_ATENCION: 'estado-atencion',
      EN_OBSERVACION: 'estado-observacion', ALTA: 'estado-alta',
      HOSPITALIZADO: 'estado-hospitalizado', REFERIDO: 'estado-referido',
    };
    return map[estado] ?? '';
  }

  tiempoEspera(fechaStr?: string): string {
    if (!fechaStr) return '--';
    const diff = Math.floor((Date.now() - new Date(fechaStr).getTime()) / 60000);
    if (diff < 60) return `${diff} min`;
    const h = Math.floor(diff / 60);
    const m = diff % 60;
    return `${h}h ${m}m`;
  }

  tiempoEsperaClass(u: UrgenciaRegistroDto): string {
    if (!u.fechaHoraIngreso || u.estado !== 'EN_ESPERA') return '';
    const diff = Math.floor((Date.now() - new Date(u.fechaHoraIngreso).getTime()) / 60000);
    const limite = TRIAGE_LIMITES[u.nivelTriage] ?? 60;
    if (u.nivelTriage === 'I') return 'tiempo-critico'; // T1 siempre inmediato
    if (diff >= limite) return 'tiempo-critico'; // superó el límite normativo
    if (diff >= limite * 0.75) return 'tiempo-alerta'; // alerta al 75% del tiempo límite
    return 'tiempo-ok';
  }

  tiempoLimiteLabel(nivelTriage: string): string {
    const limites: Record<string, string> = {
      I: 'Inmediato', II: '≤30 min', III: '≤60 min', IV: '≤120 min', V: '≤240 min',
    };
    return limites[nivelTriage] ?? '—';
  }

  tiempoRestante(u: UrgenciaRegistroDto): string {
    if (!u.fechaHoraIngreso || u.estado !== 'EN_ESPERA' || u.nivelTriage === 'I') return '';
    const diff = Math.floor((Date.now() - new Date(u.fechaHoraIngreso).getTime()) / 60000);
    const limite = TRIAGE_LIMITES[u.nivelTriage] ?? 60;
    const restante = limite - diff;
    if (restante <= 0) return '¡Tiempo superado!';
    return `${restante} min restantes`;
  }

  glasgowTotal(u: UrgenciaRegistroDto): string {
    const o = u.glasgowOcular ?? 0;
    const v = u.glasgowVerbal ?? 0;
    const m = u.glasgowMotor ?? 0;
    if (!o && !v && !m) return '—';
    return `${o + v + m}/15`;
  }

  iniciales(nombre?: string): string {
    if (!nombre) return '?';
    return nombre.split(' ').slice(0, 2).map((w) => w[0]).join('').toUpperCase();
  }

  formatFechaHora(fechaStr?: string): string {
    if (!fechaStr) return '--';
    try {
      return new Date(fechaStr).toLocaleString('es-CO', {
        day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false,
      });
    } catch {
      return fechaStr;
    }
  }

  readonly nivelesTriageOpciones: NivelTriage[] = ['TODOS', 'I', 'II', 'III', 'IV', 'V'];
  readonly estadosFiltroOpciones: EstadoFiltro[] = ['TODOS', 'EN_ESPERA', 'EN_ATENCION', 'EN_OBSERVACION', 'ALTA', 'HOSPITALIZADO', 'REFERIDO'];
  readonly estadosBotones: string[] = ['EN_ESPERA', 'EN_ATENCION', 'EN_OBSERVACION', 'ALTA', 'HOSPITALIZADO', 'REFERIDO'];
  readonly nivelesTriageIngreso = ['I', 'II', 'III', 'IV', 'V'];
}
