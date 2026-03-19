/**
 * Urgencias — Workbench Clínico Multi-Rol
 * Autor: Ing. J Sebastian Vargas S
 */

import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy, inject, signal, computed, HostListener } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import {
  UrgenciaRegistroService,
  UrgenciaRegistroDto,
  UrgenciaDashboardDto,
  SignosVitalesUrgenciaDto,
  SignosVitalesUrgenciaRequestDto,
} from '../../core/services/urgencia-registro.service';
import { EvolucionService, EvolucionDto, EvolucionRequestDto } from '../../core/services/evolucion.service';
import { NotaEnfermeriaService, NotaEnfermeriaDto, NotaEnfermeriaRequestDto } from '../../core/services/nota-enfermeria.service';
import { PacienteService, PacienteDto } from '../../core/services/paciente.service';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';
import { SesaJspdfService } from '../../core/services/sesa-jspdf.service';
import { RdaService } from '../../core/services/rda.service';

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
  private readonly router = inject(Router);
  private readonly sesaPdf = inject(SesaJspdfService);
  private readonly rdaService = inject(RdaService);

  private refreshTimer: ReturnType<typeof setInterval> | null = null;

  // ── Signals principales ─────────────────────────────────────────────────
  urgencias = signal<UrgenciaRegistroDto[]>([]);
  seleccionada = signal<UrgenciaRegistroDto | null>(null);
  tabPanel = signal<'medico' | 'enfermeria'>('medico');
  mostrarIngreso = signal(false);
  autoRefresh = signal(true);
  cargando = signal(false);
  guardando = signal(false);
  mostrarModalRetriage = signal(false);
  mostrarChecklistAtencion = signal(false);
  checklistIdentificacion = signal(false);
  checklistAlergias = signal(false);
  resumenAlta = signal('');
  instruccionesAlta = signal('');
  /** S6: Modal de alta con checklist */
  mostrarModalAlta = signal(false);
  formAltaDiagnostico = signal('');
  formAltaTratamiento = signal('');
  formAltaRecomendaciones = signal('');
  formAltaProximaCita = signal('');
  /** S11: RDA urgencias — enviando */
  rdaEnviando = signal(false);

  // Filtros
  filtroTriage = signal<NivelTriage>('TODOS');
  filtroEstado = signal<EstadoFiltro>('TODOS');
  busqueda = signal('');
  dashboard = signal<UrgenciaDashboardDto | null>(null);
  sonidoAlertas = signal(true);

  // Evoluciones y notas
  evoluciones = signal<EvolucionDto[]>([]);
  notas = signal<NotaEnfermeriaDto[]>([]);
  /** Signos vitales seriados (Res. 5596/2015) — múltiples tomas durante espera/atención */
  signosVitalesSeriados = signal<SignosVitalesUrgenciaDto[]>([]);

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
  /** Formulario nuevo registro de signos vitales seriados */
  formSignosVitales = signal({
    presionArterial: '',
    frecuenciaCardiaca: '',
    frecuenciaRespiratoria: '',
    temperatura: '',
    saturacionO2: '',
    peso: '',
    dolorEva: '',
    glasgowOcular: '' as string,
    glasgowVerbal: '' as string,
    glasgowMotor: '' as string,
  });

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
        this.cargarDashboard();
        this.notificarCriticosSiAplica();
      },
      error: (err) => {
        this.toast.error(err?.error?.error || 'Error al cargar urgencias', 'Error');
        this.cargando.set(false);
      },
    });
  }

  private cargarDashboard(): void {
    this.urgenciaService.dashboard().subscribe({
      next: (d) => this.dashboard.set(d),
      error: () => {},
    });
  }

  private notificarCriticosSiAplica(): void {
    const d = this.dashboard();
    const fuera = d?.fueraDeTiempo?.length ?? 0;
    const criticos = this.urgencias().filter((u) => (u.nivelTriage === 'I' || u.nivelTriage === 'II') && u.estado === 'EN_ESPERA').length;
    if (fuera > 0) {
      this.toast.warning(`${fuera} paciente(s) fuera de tiempo límite`, 'Alerta Res. 5596/2015');
      if (this.sonidoAlertas()) this.reproducirSonidoAlerta();
    }
    if (criticos > 0 && fuera === 0) {
      this.toast.info(`${criticos} paciente(s) crítico(s) en espera`, 'T I/II');
      if (this.sonidoAlertas()) this.reproducirSonidoAlerta();
    }
  }

  private reproducirSonidoAlerta(): void {
    try {
      const ctx = new (window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext)();
      const o = ctx.createOscillator();
      const g = ctx.createGain();
      o.connect(g);
      g.connect(ctx.destination);
      o.frequency.value = 800;
      g.gain.value = 0.15;
      o.start(ctx.currentTime);
      o.stop(ctx.currentTime + 0.2);
    } catch {
      // Ignorar si el navegador no permite audio
    }
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
    this.signosVitalesSeriados.set([]);
    this.formEvolucion.set({ subjetivo: '', objetivo: '', analisis: '', plan: '', codigoCie10: '' });
    this.formNota.set({ nota: '' });
    this.formSignosVitales.set({
      presionArterial: '', frecuenciaCardiaca: '', frecuenciaRespiratoria: '',
      temperatura: '', saturacionO2: '', peso: '', dolorEva: '',
      glasgowOcular: '', glasgowVerbal: '', glasgowMotor: '',
    });

    if (u.atencionId) {
      this.cargarEvoluciones(u.atencionId);
      this.cargarNotas(u.atencionId);
    }
    this.cargarSignosVitales(u.id);
  }

  private cargarSignosVitales(urgenciaId: number): void {
    this.urgenciaService.listSignosVitales(urgenciaId).subscribe({
      next: (list) => this.signosVitalesSeriados.set(list ?? []),
      error: () => this.signosVitalesSeriados.set([]),
    });
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
    if (nuevoEstado === 'EN_ATENCION') {
      this.checklistIdentificacion.set(false);
      this.checklistAlergias.set(false);
      this.mostrarChecklistAtencion.set(true);
      this.estadoPendienteChecklist.set(nuevoEstado);
      return;
    }
    if (nuevoEstado === 'ALTA') {
      this.formAltaDiagnostico.set(sel.altaDiagnostico ?? '');
      this.formAltaTratamiento.set(sel.altaTratamiento ?? '');
      this.formAltaRecomendaciones.set(sel.altaRecomendaciones ?? '');
      this.formAltaProximaCita.set(sel.altaProximaCita ?? '');
      this.mostrarModalAlta.set(true);
      return;
    }
    this.ejecutarCambioEstado(nuevoEstado);
  }

  estadoPendienteChecklist = signal<string | null>(null);

  confirmarChecklistAtencion(): void {
    if (!this.checklistIdentificacion() || !this.checklistAlergias()) {
      this.toast.warning('Marque las dos verificaciones de seguridad', 'Checklist');
      return;
    }
    const estado = this.estadoPendienteChecklist();
    this.mostrarChecklistAtencion.set(false);
    this.estadoPendienteChecklist.set(null);
    if (estado) this.ejecutarCambioEstado(estado);
  }

  cancelarChecklistAtencion(): void {
    this.mostrarChecklistAtencion.set(false);
    this.estadoPendienteChecklist.set(null);
  }

  cancelarModalAlta(): void {
    this.mostrarModalAlta.set(false);
  }

  /** S6: Cerrar alta — POST con checklist y cierra modal. */
  cerrarAlta(): void {
    const sel = this.seleccionada();
    if (!sel) return;
    this.guardando.set(true);
    this.urgenciaService.darAlta(sel.id, {
      diagnostico: this.formAltaDiagnostico() || undefined,
      tratamiento: this.formAltaTratamiento() || undefined,
      recomendaciones: this.formAltaRecomendaciones() || undefined,
      proximaCita: this.formAltaProximaCita() || undefined,
    }).subscribe({
      next: (updated) => {
        this.urgencias.update((list) => list.map((u) => (u.id === updated.id ? updated : u)));
        this.seleccionada.set(updated);
        this.mostrarModalAlta.set(false);
        this.guardando.set(false);
        this.toast.success('Alta registrada. Puede descargar el PDF para el paciente.', 'Alta');
      },
      error: (err) => {
        this.toast.error(err?.error?.error || 'Error al registrar alta', 'Error');
        this.guardando.set(false);
      },
    });
  }

  /** S6: Descargar PDF de resumen de alta (backend). */
  descargarPdfAlta(): void {
    const sel = this.seleccionada();
    if (!sel) return;
    this.guardando.set(true);
    this.urgenciaService.getPdfAlta(sel.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `alta-urgencia-${sel.id}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
        this.guardando.set(false);
        this.toast.success('PDF descargado', 'Alta');
      },
      error: (err) => {
        this.toast.error(err?.error?.error || 'Error al generar PDF', 'Error');
        this.guardando.set(false);
      },
    });
  }

  /** S6: Guardar datos de alta y luego descargar PDF (para uso desde el modal). */
  guardarYDescargarPdfAlta(): void {
    const sel = this.seleccionada();
    if (!sel) return;
    this.guardando.set(true);
    this.urgenciaService.darAlta(sel.id, {
      diagnostico: this.formAltaDiagnostico() || undefined,
      tratamiento: this.formAltaTratamiento() || undefined,
      recomendaciones: this.formAltaRecomendaciones() || undefined,
      proximaCita: this.formAltaProximaCita() || undefined,
    }).subscribe({
      next: (updated) => {
        this.urgencias.update((list) => list.map((u) => (u.id === updated.id ? updated : u)));
        this.seleccionada.set(updated);
        this.urgenciaService.getPdfAlta(sel.id).subscribe({
          next: (blob) => {
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `alta-urgencia-${sel.id}.pdf`;
            a.click();
            URL.revokeObjectURL(url);
            this.guardando.set(false);
            this.toast.success('Alta guardada y PDF descargado', 'Alta');
          },
          error: () => { this.guardando.set(false); },
        });
      },
      error: (err) => {
        this.toast.error(err?.error?.error || 'Error al registrar alta', 'Error');
        this.guardando.set(false);
      },
    });
  }

  private ejecutarCambioEstado(nuevoEstado: string): void {
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

  abrirModalRetriage(): void {
    this.nivelTriageRetriage.set(this.seleccionada()?.nivelTriage ?? 'III');
    this.mostrarModalRetriage.set(true);
  }

  nivelTriageRetriage = signal<string>('III');

  setNivelTriageRetriage(val: string): void {
    this.nivelTriageRetriage.set(val);
  }

  guardarRetriage(): void {
    const sel = this.seleccionada();
    if (!sel) return;
    this.guardando.set(true);
    this.urgenciaService.updateTriage(sel.id, { nivelTriage: this.nivelTriageRetriage() }).subscribe({
      next: (updated) => {
        this.urgencias.update((list) => list.map((u) => (u.id === updated.id ? updated : u)));
        this.seleccionada.set(updated);
        this.mostrarModalRetriage.set(false);
        this.guardando.set(false);
        this.toast.success('Triage actualizado', 'Re-clasificación');
      },
      error: (err) => {
        this.toast.error(err?.error?.error || 'Error al actualizar triage', 'Error');
        this.guardando.set(false);
      },
    });
  }

  generarPdfAlta(): void {
    const sel = this.seleccionada();
    const resumen = this.resumenAlta().trim();
    const instrucciones = this.instruccionesAlta().trim();
    if (!sel) return;
    this.sesaPdf.generarResumenAltaUrgenciasPdf({
      pacienteNombre: sel.pacienteNombre ?? '',
      pacienteDocumento: sel.pacienteDocumento ?? '',
      fechaAlta: new Date().toLocaleDateString('es-CO'),
      resumen: resumen || '—',
      instrucciones: instrucciones || '—',
    });
    this.toast.success('PDF generado. Revisa las descargas.', 'Resumen de alta');
  }

  setResumenAlta(val: string): void { this.resumenAlta.set(val); }
  setInstruccionesAlta(val: string): void { this.instruccionesAlta.set(val); }
  setChecklistIdentificacion(val: boolean): void { this.checklistIdentificacion.set(val); }
  setChecklistAlergias(val: boolean): void { this.checklistAlergias.set(val); }
  setFormAltaDiagnostico(val: string): void { this.formAltaDiagnostico.set(val); }
  setFormAltaTratamiento(val: string): void { this.formAltaTratamiento.set(val); }
  setFormAltaRecomendaciones(val: string): void { this.formAltaRecomendaciones.set(val); }
  setFormAltaProximaCita(val: string): void { this.formAltaProximaCita.set(val); }

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
    if (user?.personalId != null || user?.userId != null) dto.profesionalId = user.personalId ?? user.userId;
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
    if (user?.personalId != null || user?.userId != null) dto.profesionalId = user.personalId ?? user.userId;
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

  /** Registra un nuevo registro de signos vitales seriados (Res. 5596/2015). */
  guardarSignosVitales(): void {
    const sel = this.seleccionada();
    const f = this.formSignosVitales();
    if (!sel) return;
    const hasAlgunValor = [
      f.presionArterial, f.frecuenciaCardiaca, f.frecuenciaRespiratoria,
      f.temperatura, f.saturacionO2, f.peso, f.dolorEva,
      f.glasgowOcular, f.glasgowVerbal, f.glasgowMotor,
    ].some((v) => v != null && String(v).trim() !== '');
    if (!hasAlgunValor) {
      this.toast.warning('Ingrese al menos un signo vital', 'Signos vitales');
      return;
    }
    const dto: SignosVitalesUrgenciaRequestDto = {
      presionArterial: f.presionArterial?.trim() || undefined,
      frecuenciaCardiaca: f.frecuenciaCardiaca?.trim() || undefined,
      frecuenciaRespiratoria: f.frecuenciaRespiratoria?.trim() || undefined,
      temperatura: f.temperatura?.trim() || undefined,
      saturacionO2: f.saturacionO2?.trim() || undefined,
      peso: f.peso?.trim() || undefined,
      dolorEva: f.dolorEva?.trim() || undefined,
      glasgowOcular: f.glasgowOcular ? parseInt(f.glasgowOcular, 10) : undefined,
      glasgowVerbal: f.glasgowVerbal ? parseInt(f.glasgowVerbal, 10) : undefined,
      glasgowMotor: f.glasgowMotor ? parseInt(f.glasgowMotor, 10) : undefined,
    };
    this.guardando.set(true);
    this.urgenciaService.createSignosVitales(sel.id, dto).subscribe({
      next: (nuevo) => {
        this.signosVitalesSeriados.update((list) => [...list, nuevo]);
        this.formSignosVitales.set({
          presionArterial: '', frecuenciaCardiaca: '', frecuenciaRespiratoria: '',
          temperatura: '', saturacionO2: '', peso: '', dolorEva: '',
          glasgowOcular: '', glasgowVerbal: '', glasgowMotor: '',
        });
        this.guardando.set(false);
        this.toast.success('Signos vitales registrados', 'Urgencias');
      },
      error: (err) => {
        this.toast.error(err?.error?.error || 'Error al registrar signos vitales', 'Error');
        this.guardando.set(false);
      },
    });
  }

  setFormSignosVitales(field: string, value: string): void {
    this.formSignosVitales.update((f) => ({ ...f, [field]: value }));
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

  /** Alerta por signos vitales fuera de rango (sugerencia 2.3). */
  hasAlertaSV(u: UrgenciaRegistroDto): boolean {
    const ta = u.svPresionArterial ?? '';
    const sistolica = parseInt(ta.split('/')[0], 10);
    if (!isNaN(sistolica) && (sistolica > 180 || sistolica < 90)) return true;
    const spo2 = u.svSaturacionO2 != null ? parseInt(String(u.svSaturacionO2), 10) : NaN;
    if (!isNaN(spo2) && spo2 < 92) return true;
    const g = (u.glasgowOcular ?? 0) + (u.glasgowVerbal ?? 0) + (u.glasgowMotor ?? 0);
    if (g > 0 && g < 15) return true;
    return false;
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

  /**
   * Navega a Historia Clínica con el paciente seleccionado.
   * Permite crear la HC o la atención si no existe, sin bloquear al profesional en urgencias.
   */
  irAHistoriaClinica(): void {
    const u = this.seleccionada();
    if (!u?.pacienteId) return;
    this.router.navigate(['/historia-clinica'], { queryParams: { pacienteId: u.pacienteId } });
  }

  readonly nivelesTriageOpciones: NivelTriage[] = ['TODOS', 'I', 'II', 'III', 'IV', 'V'];
  readonly estadosFiltroOpciones: EstadoFiltro[] = ['TODOS', 'EN_ESPERA', 'EN_ATENCION', 'EN_OBSERVACION', 'ALTA', 'HOSPITALIZADO', 'REFERIDO'];
  readonly estadosBotones: string[] = ['EN_ESPERA', 'EN_ATENCION', 'EN_OBSERVACION', 'ALTA', 'HOSPITALIZADO', 'REFERIDO'];
  readonly nivelesTriageIngreso = ['I', 'II', 'III', 'IV', 'V'];

  /** Atajos de teclado (sugerencia 2.9): 1-5 triage, E/A estado, Enter detalle, Ctrl+N nuevo. */
  @HostListener('document:keydown', ['$event'])
  onKeyDown(e: KeyboardEvent): void {
    if (e.target && /^(INPUT|TEXTAREA|SELECT)$/.test((e.target as HTMLElement).tagName)) return;
    const key = e.key.toUpperCase();
    if (e.ctrlKey && key === 'N') {
      e.preventDefault();
      this.abrirIngreso();
      return;
    }
    if (key >= '1' && key <= '5') {
      const triage: NivelTriage = key === '1' ? 'I' : key === '2' ? 'II' : key === '3' ? 'III' : key === '4' ? 'IV' : 'V';
      this.setFiltroTriage(triage);
      return;
    }
    if (key === 'E') {
      this.setFiltroEstado('EN_ESPERA');
      return;
    }
    if (key === 'A') {
      this.setFiltroEstado('EN_ATENCION');
      return;
    }
    if (key === 'Enter') {
      const list = this.urgenciasFiltradas();
      if (list.length > 0 && this.seleccionada()?.id !== list[0].id) {
        this.seleccionar(list[0]);
      }
    }
  }

  setSonidoAlertas(val: boolean): void {
    this.sonidoAlertas.set(val);
  }

  /** S11: Generar y enviar RDA de Urgencias (Res. 1888/2025). */
  generarYEnviarRdaUrgencia(): void {
    const sel = this.seleccionada();
    if (!sel) return;
    this.rdaEnviando.set(true);
    this.rdaService.generarYEnviarUrgencia(sel.id).subscribe({
      next: (status) => {
        this.rdaEnviando.set(false);
        this.toast.success(
          `RDA ${this.rdaService.estadoLabel(status.estadoEnvio)} — ${status.idMinisterio ? 'ID Ministerio: ' + status.idMinisterio : ''}`,
          'RDA Urgencias'
        );
      },
      error: (err) => {
        this.rdaEnviando.set(false);
        this.toast.error(err?.error?.error || err?.message || 'Error al generar/enviar RDA', 'RDA Urgencias');
      },
    });
  }
}
