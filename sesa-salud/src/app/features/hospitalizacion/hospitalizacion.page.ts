/**
 * Hospitalización — Censo hospitalario con órdenes médicas estructuradas.
 * Autor: Ing. J Sebastian Vargas S
 */
import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PacienteDto, PacienteService } from '../../core/services/paciente.service';
import { HospitalizacionDto, HospitalizacionService } from '../../core/services/hospitalizacion.service';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';
import { RdaService } from '../../core/services/rda.service';

export type PanelMode = 'ingresar' | 'detalle';
export type PanelTab  = 'evolucion' | 'ordenes' | 'egreso';

export interface MedicamentoOrden {
  nombre:     string;
  dosis:      string;
  via:        string;
  frecuencia: string;
  duracion:   string;
  notas:      string;
}

export interface EvolucionData {
  estadoGeneral: string;
  ta_s:          string;
  ta_d:          string;
  fc:            string;
  fr:            string;
  temp:          string;
  spo2:          string;
  observaciones: string;
}

@Component({
  standalone: true,
  selector: 'sesa-hospitalizacion-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule],
  templateUrl: './hospitalizacion.page.html',
  styleUrl:    './hospitalizacion.page.scss',
})
export class HospitalizacionPageComponent implements OnInit {

  // ── Servicios ────────────────────────────────────────────────────────────
  private readonly pacienteService        = inject(PacienteService);
  private readonly hospitalizacionService = inject(HospitalizacionService);
  private readonly toast                  = inject(SesaToastService);
  private readonly rdaService             = inject(RdaService);
  private readonly cdr                    = inject(ChangeDetectorRef);

  // ── Catálogos clínicos ────────────────────────────────────────────────────
  readonly VIAS = ['VO', 'IV', 'IM', 'SC', 'SL', 'Inhalatoria', 'Tópica', 'Rectal', 'SNG'];

  readonly FRECUENCIAS = [
    'STAT (dosis única)', 'Cada 4h', 'Cada 6h', 'Cada 8h',
    'Cada 12h', 'Cada 24h', 'Diario', 'BID', 'TID', 'QID',
    'PRN (a necesidad)', 'Cada 48h', 'Semanal',
  ];

  readonly DURACIONES = [
    '1 día', '2 días', '3 días', '5 días', '7 días',
    '10 días', '14 días', '21 días', 'Hasta nueva orden', 'Indefinido',
  ];

  readonly MOTIVOS_RAPIDOS = [
    'Dolor abdominal', 'Síndrome febril', 'Dificultad respiratoria',
    'Infección urinaria', 'HTA descompensada', 'DM descompensada',
    'Post-quirúrgico', 'TCE', 'ACV', 'Dolor torácico',
    'Neumonía', 'Sepsis', 'Cetoacidosis', 'Ins. cardíaca',
  ];

  readonly MEDICAMENTOS_COMUNES = [
    'Acetaminofén', 'Ibuprofeno', 'Naproxeno', 'Diclofenaco', 'Ketorolaco',
    'Ampicilina', 'Ampicilina/Sulbactam', 'Amoxicilina', 'Amoxicilina/Clavulanato',
    'Ceftriaxona', 'Cefazolina', 'Cefuroxima', 'Clindamicina',
    'Ciprofloxacino', 'Levofloxacino', 'Metronidazol', 'Vancomicina',
    'Piperacilina/Tazobactam', 'Meropenem', 'Imipenem', 'Gentamicina',
    'Omeprazol', 'Pantoprazol', 'Ranitidina', 'Metoclopramida', 'Ondansetrón',
    'Insulina Regular', 'Insulina NPH', 'Metformina', 'Glibenclamida',
    'Enalapril', 'Losartán', 'Amlodipino', 'Metoprolol', 'Hidralazina',
    'Furosemida', 'Espironolactona', 'Atorvastatina',
    'Salbutamol', 'Ipratropio', 'Aminofilina', 'Dexametasona', 'Prednisolona',
    'Morfina', 'Tramadol', 'Midazolam', 'Fenitoína', 'Valproato',
    'Heparina', 'Enoxaparina', 'Warfarina',
    'SSN 0.9% 500mL', 'Lactato de Ringer 500mL', 'Dextrosa 5% 500mL',
    'KCl 20 mEq', 'NaHCO3 8.4%', 'Sulfato de Magnesio',
  ];

  readonly ESTADOS_GENERALES = [
    'ESTABLE', 'MEJORIA', 'SIN_CAMBIOS', 'DETERIORO_LEVE', 'DETERIORO_CRITICO',
  ];

  // ── Estado principal ──────────────────────────────────────────────────────
  hospitalizaciones = signal<HospitalizacionDto[]>([]);
  pacientes         = signal<PacienteDto[]>([]);
  loading           = signal(false);
  saving            = signal(false);

  // ── Búsqueda y filtros ────────────────────────────────────────────────────
  searchQuery    = signal('');
  selectedEstado = signal('');

  // ── Panel lateral ─────────────────────────────────────────────────────────
  showPanel    = signal(false);
  panelMode    = signal<PanelMode>('ingresar');
  panelTab     = signal<PanelTab>('evolucion');
  selectedHosp = signal<HospitalizacionDto | null>(null);

  // ── RDA ───────────────────────────────────────────────────────────────────
  rdaEnviandoId = signal<number | null>(null);

  // ── Formulario nuevo ingreso ──────────────────────────────────────────────
  ingresoForm = {
    pacienteId:     null as number | null,
    pacienteSearch: '',
    servicio:       '',
    cama:           '',
  };

  // ── Formulario detalle/edición ────────────────────────────────────────────
  detalleForm = {
    servicio:  '',
    cama:      '',
    estado:    '',
    epicrisis: '',
    // Textos raw acumulados (historial backend)
    evolucionDiariaRaw: '',
    ordenesMedicasRaw:  '',
  };

  // ── Builder: Medicamentos (ingreso) ──────────────────────────────────────
  medicamentosIngreso: MedicamentoOrden[] = [];
  newMedIngreso: MedicamentoOrden = this.emptyMed();
  showMedDropIngreso = false;

  // ── Builder: Medicamentos (detalle) ──────────────────────────────────────
  medicamentosDetalle: MedicamentoOrden[] = [];
  newMedDetalle: MedicamentoOrden = this.emptyMed();
  showMedDropDetalle = false;

  // ── Builder: Evolución (ingreso) ─────────────────────────────────────────
  evolucionIngreso: EvolucionData = this.emptyEvolucion();

  // ── Builder: Evolución (detalle - nueva nota) ─────────────────────────────
  evolucionDetalle: EvolucionData = this.emptyEvolucion();

  // ── Búsqueda paciente (signal para computed reactivo) ─────────────────────
  ingrPacienteSearch = signal('');

  // ── Computed: lista filtrada ──────────────────────────────────────────────
  filteredHosp = computed(() => {
    const q   = this.searchQuery().toLowerCase().trim();
    const est = this.selectedEstado();
    return this.hospitalizaciones().filter(h => {
      const matchQ = !q
        || h.pacienteNombre.toLowerCase().includes(q)
        || (h.pacienteDocumento?.toLowerCase().includes(q) ?? false)
        || (h.cama?.toLowerCase().includes(q) ?? false)
        || (h.servicio?.toLowerCase().includes(q) ?? false);
      const matchEst = !est || h.estado === est;
      return matchQ && matchEst;
    });
  });

  // ── Computed: KPIs ────────────────────────────────────────────────────────
  totalIngresados = computed(() =>
    this.hospitalizaciones().filter(h => h.estado === 'INGRESADO').length
  );

  ingresosHoy = computed(() => {
    const hoy = this.todayStr();
    return this.hospitalizaciones().filter(h =>
      h.fechaIngreso && h.fechaIngreso.startsWith(hoy)
    ).length;
  });

  egresosHoy = computed(() => {
    const hoy = this.todayStr();
    return this.hospitalizaciones().filter(h =>
      h.fechaEgreso && h.fechaEgreso.startsWith(hoy)
    ).length;
  });

  avgEstancia = computed(() => {
    const activos = this.hospitalizaciones().filter(
      h => h.estado === 'INGRESADO' && h.fechaIngreso
    );
    if (!activos.length) return 0;
    const now = Date.now();
    const total = activos.reduce((sum, h) => {
      const ms = now - new Date(h.fechaIngreso!).getTime();
      return sum + ms / (1000 * 60 * 60 * 24);
    }, 0);
    return +(total / activos.length).toFixed(1);
  });

  // ── Pacientes filtrados por búsqueda (reactivo con signal) ────────────────
  pacientesFiltrados = computed(() => {
    const q = this.ingrPacienteSearch().toLowerCase().trim();
    if (!q) return this.pacientes().slice(0, 30);
    return this.pacientes().filter(p =>
      p.nombres?.toLowerCase().includes(q) ||
      p.apellidos?.toLowerCase().includes(q) ||
      p.documento?.toLowerCase().includes(q)
    ).slice(0, 30);
  });

  // ── Lifecycle ─────────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.cargar();
    this.pacienteService.list(0, 500).subscribe({
      next: res => { this.pacientes.set(res.content ?? []); this.cdr.markForCheck(); },
    });
  }

  cargar(): void {
    this.loading.set(true);
    this.hospitalizacionService.listAll().subscribe({
      next: data => {
        this.hospitalizaciones.set(data ?? []);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: err => {
        this.loading.set(false);
        this.toast.error(err?.error?.error || 'No se pudo cargar el censo hospitalario');
        this.cdr.markForCheck();
      },
    });
  }

  // ── Panel: abrir / cerrar ─────────────────────────────────────────────────
  abrirNuevoIngreso(): void {
    this.ingresoForm        = { pacienteId: null, pacienteSearch: '', servicio: '', cama: '' };
    this.medicamentosIngreso  = [];
    this.newMedIngreso        = this.emptyMed();
    this.evolucionIngreso     = this.emptyEvolucion();
    this.showMedDropIngreso   = false;
    this.ingrPacienteSearch.set('');
    this.panelMode.set('ingresar');
    this.showPanel.set(true);
  }

  verDetalle(h: HospitalizacionDto): void {
    this.selectedHosp.set(h);
    this.detalleForm = {
      servicio:            h.servicio        ?? '',
      cama:                h.cama            ?? '',
      estado:              h.estado          ?? 'INGRESADO',
      epicrisis:           h.epicrisis       ?? '',
      evolucionDiariaRaw:  h.evolucionDiaria ?? '',
      ordenesMedicasRaw:   h.ordenesMedicas  ?? '',
    };
    this.medicamentosDetalle  = [];
    this.newMedDetalle        = this.emptyMed();
    this.evolucionDetalle     = this.emptyEvolucion();
    this.showMedDropDetalle   = false;
    this.panelTab.set('evolucion');
    this.panelMode.set('detalle');
    this.showPanel.set(true);
  }

  cerrarPanel(): void {
    this.showPanel.set(false);
    this.selectedHosp.set(null);
  }

  // ── Combobox medicamentos ─────────────────────────────────────────────────
  filteredMedsIngreso(): string[] {
    const q = this.newMedIngreso.nombre.toLowerCase().trim();
    if (!q) return this.MEDICAMENTOS_COMUNES.slice(0, 20);
    return this.MEDICAMENTOS_COMUNES.filter(m => m.toLowerCase().includes(q)).slice(0, 30);
  }

  filteredMedsDetalle(): string[] {
    const q = this.newMedDetalle.nombre.toLowerCase().trim();
    if (!q) return this.MEDICAMENTOS_COMUNES.slice(0, 20);
    return this.MEDICAMENTOS_COMUNES.filter(m => m.toLowerCase().includes(q)).slice(0, 30);
  }

  seleccionarMedIngreso(nombre: string): void {
    this.newMedIngreso.nombre = nombre;
    this.showMedDropIngreso   = false;
    this.cdr.markForCheck();
  }

  seleccionarMedDetalle(nombre: string): void {
    this.newMedDetalle.nombre = nombre;
    this.showMedDropDetalle   = false;
    this.cdr.markForCheck();
  }

  onMedBlurIngreso(): void {
    setTimeout(() => { this.showMedDropIngreso = false; this.cdr.markForCheck(); }, 180);
  }

  onMedBlurDetalle(): void {
    setTimeout(() => { this.showMedDropDetalle = false; this.cdr.markForCheck(); }, 180);
  }

  toggleMedDropIngreso(): void {
    this.showMedDropIngreso = !this.showMedDropIngreso;
    this.cdr.markForCheck();
  }

  toggleMedDropDetalle(): void {
    this.showMedDropDetalle = !this.showMedDropDetalle;
    this.cdr.markForCheck();
  }

  // ── Medication builder: Ingreso ───────────────────────────────────────────
  agregarMedIngreso(): void {
    if (!this.newMedIngreso.nombre.trim()) {
      this.toast.error('Escribe el nombre del medicamento.');
      return;
    }
    if (!this.newMedIngreso.dosis.trim()) {
      this.toast.error('Indica la dosis del medicamento.');
      return;
    }
    this.medicamentosIngreso = [...this.medicamentosIngreso, { ...this.newMedIngreso }];
    this.newMedIngreso = this.emptyMed();
    this.cdr.markForCheck();
  }

  quitarMedIngreso(i: number): void {
    this.medicamentosIngreso = this.medicamentosIngreso.filter((_, idx) => idx !== i);
    this.cdr.markForCheck();
  }

  // ── Medication builder: Detalle ───────────────────────────────────────────
  agregarMedDetalle(): void {
    if (!this.newMedDetalle.nombre.trim()) {
      this.toast.error('Escribe el nombre del medicamento.');
      return;
    }
    if (!this.newMedDetalle.dosis.trim()) {
      this.toast.error('Indica la dosis del medicamento.');
      return;
    }
    this.medicamentosDetalle = [...this.medicamentosDetalle, { ...this.newMedDetalle }];
    this.newMedDetalle = this.emptyMed();
    this.cdr.markForCheck();
  }

  quitarMedDetalle(i: number): void {
    this.medicamentosDetalle = this.medicamentosDetalle.filter((_, idx) => idx !== i);
    this.cdr.markForCheck();
  }

  // ── Evolución: motivo rápido (ingreso) ───────────────────────────────────
  aplicarMotivoRapido(motivo: string): void {
    const prev = this.evolucionIngreso.observaciones.trim();
    this.evolucionIngreso = {
      ...this.evolucionIngreso,
      observaciones: prev
        ? prev + '. ' + motivo
        : 'Motivo de ingreso: ' + motivo + '.',
    };
    this.cdr.markForCheck();
  }

  // ── Acciones ──────────────────────────────────────────────────────────────
  registrarIngreso(): void {
    if (!this.ingresoForm.pacienteId) {
      this.toast.error('Selecciona un paciente antes de registrar el ingreso.');
      return;
    }
    this.saving.set(true);
    const evolucionText = this.serializeEvolucion(this.evolucionIngreso);
    const ordenesText   = this.serializeMeds(this.medicamentosIngreso);

    this.hospitalizacionService.create({
      pacienteId:      this.ingresoForm.pacienteId,
      servicio:        this.ingresoForm.servicio  || undefined,
      cama:            this.ingresoForm.cama       || undefined,
      estado:          'INGRESADO',
      evolucionDiaria: evolucionText || undefined,
      ordenesMedicas:  ordenesText   || undefined,
    }).subscribe({
      next: () => {
        this.saving.set(false);
        this.toast.success('Ingreso hospitalario registrado correctamente.');
        this.cerrarPanel();
        this.cargar();
      },
      error: err => {
        this.saving.set(false);
        this.toast.error(err?.error?.error || 'No se pudo registrar el ingreso.');
        this.cdr.markForCheck();
      },
    });
  }

  guardarDetalle(): void {
    const h = this.selectedHosp();
    if (!h) return;
    this.saving.set(true);

    // Evolución: nueva nota APPEND al historial existente
    const nuevaNota = this.serializeEvolucion(this.evolucionDetalle);
    const evolucionFinal = nuevaNota
      ? (this.detalleForm.evolucionDiariaRaw
          ? this.detalleForm.evolucionDiariaRaw + '\n\n────────────────────\n' + nuevaNota
          : nuevaNota)
      : this.detalleForm.evolucionDiariaRaw;

    // Órdenes: si hay nuevas, REEMPLAZA el listado anterior
    const nuevasOrdenes   = this.serializeMeds(this.medicamentosDetalle);
    const ordenesFinal    = nuevasOrdenes || this.detalleForm.ordenesMedicasRaw;

    this.hospitalizacionService.update(h.id, {
      pacienteId:      h.pacienteId,
      servicio:        this.detalleForm.servicio  || undefined,
      cama:            this.detalleForm.cama       || undefined,
      estado:          this.detalleForm.estado     || undefined,
      evolucionDiaria: evolucionFinal              || undefined,
      ordenesMedicas:  ordenesFinal                || undefined,
      epicrisis:       this.detalleForm.epicrisis  || undefined,
    }).subscribe({
      next: () => {
        this.saving.set(false);
        this.toast.success('Cambios guardados correctamente.');
        this.cerrarPanel();
        this.cargar();
      },
      error: err => {
        this.saving.set(false);
        this.toast.error(err?.error?.error || 'No se pudo guardar los cambios.');
        this.cdr.markForCheck();
      },
    });
  }

  darEgreso(): void {
    const h = this.selectedHosp();
    if (!h) return;
    this.saving.set(true);
    this.hospitalizacionService.update(h.id, {
      pacienteId: h.pacienteId,
      estado:     'EGRESADO',
      epicrisis:  this.detalleForm.epicrisis || undefined,
    }).subscribe({
      next: () => {
        this.saving.set(false);
        this.toast.success(`${h.pacienteNombre} egresado correctamente.`);
        this.cerrarPanel();
        this.cargar();
      },
      error: err => {
        this.saving.set(false);
        this.toast.error(err?.error?.error || 'No se pudo registrar el egreso.');
        this.cdr.markForCheck();
      },
    });
  }

  generarYEnviarRda(h: HospitalizacionDto): void {
    this.rdaEnviandoId.set(h.id);
    this.rdaService.generarYEnviarHospitalizacion(h.id).subscribe({
      next: status => {
        this.rdaEnviandoId.set(null);
        this.toast.success(
          `RDA ${this.rdaService.estadoLabel(status.estadoEnvio)}${status.idMinisterio ? ' — ID: ' + status.idMinisterio : ''}`,
          'RDA Hospitalización'
        );
        this.cdr.markForCheck();
      },
      error: err => {
        this.rdaEnviandoId.set(null);
        this.toast.error(err?.error?.error || 'Error al generar/enviar RDA', 'RDA');
        this.cdr.markForCheck();
      },
    });
  }

  seleccionarPaciente(p: PacienteDto): void {
    this.ingresoForm.pacienteId = p.id;
    const label = `${p.nombres} ${p.apellidos ?? ''} — ${p.documento ?? ''}`.trim();
    this.ingresoForm.pacienteSearch = label;
    this.ingrPacienteSearch.set('__SELECTED__'); // cierra el autocomplete
    this.cdr.markForCheck();
  }

  limpiarPacienteSeleccionado(): void {
    this.ingresoForm.pacienteId     = null;
    this.ingresoForm.pacienteSearch = '';
    this.ingrPacienteSearch.set('');
    this.cdr.markForCheck();
  }

  onPacienteSearchInput(value: string): void {
    this.ingresoForm.pacienteSearch = value;
    this.ingrPacienteSearch.set(value);
  }

  // ── Serialización ──────────────────────────────────────────────────────────
  serializeMeds(meds: MedicamentoOrden[]): string {
    if (!meds.length) return '';
    const ts   = this.nowLabel();
    const lines = meds.map((m, i) => {
      let line = `${i + 1}. ${m.nombre} ${m.dosis} | ${m.via} | ${m.frecuencia}`;
      if (m.duracion) line += ` | ${m.duracion}`;
      if (m.notas)    line += ` — ${m.notas}`;
      return line;
    });
    return `ÓRDENES MÉDICAS — ${ts}\n${lines.join('\n')}`;
  }

  serializeEvolucion(ev: EvolucionData): string {
    const hasData = ev.estadoGeneral || ev.ta_s || ev.fc || ev.fr || ev.temp || ev.spo2 || ev.observaciones;
    if (!hasData) return '';
    const ts    = this.nowLabel();
    const parts: string[] = [`EVOLUCIÓN — ${ts}`];
    if (ev.estadoGeneral) parts.push(`Estado: ${this.estadoGeneralLabel(ev.estadoGeneral)}`);
    const sv: string[] = [];
    if (ev.ta_s && ev.ta_d) sv.push(`TA ${ev.ta_s}/${ev.ta_d} mmHg`);
    if (ev.fc)   sv.push(`FC ${ev.fc} lpm`);
    if (ev.fr)   sv.push(`FR ${ev.fr} rpm`);
    if (ev.temp) sv.push(`T° ${ev.temp}°C`);
    if (ev.spo2) sv.push(`SpO2 ${ev.spo2}%`);
    if (sv.length) parts.push(sv.join(' · '));
    if (ev.observaciones) parts.push(ev.observaciones);
    return parts.join('\n');
  }

  // ── Helpers de presentación ────────────────────────────────────────────────
  estadoGeneralLabel(e: string): string {
    const m: Record<string, string> = {
      ESTABLE:            'Estable',
      MEJORIA:            'En mejoría',
      SIN_CAMBIOS:        'Sin cambios',
      DETERIORO_LEVE:     'Deterioro leve',
      DETERIORO_CRITICO:  'Deterioro crítico',
    };
    return m[e] ?? e;
  }

  estadoGeneralClass(e: string): string {
    const m: Record<string, string> = {
      ESTABLE:           'eg--estable',
      MEJORIA:           'eg--mejoria',
      SIN_CAMBIOS:       'eg--sin-cambios',
      DETERIORO_LEVE:    'eg--deterioro-leve',
      DETERIORO_CRITICO: 'eg--deterioro-critico',
    };
    return m[e] ?? '';
  }

  diasEstancia(h: HospitalizacionDto): number {
    if (!h.fechaIngreso) return 0;
    const from = new Date(h.fechaIngreso);
    const to   = h.fechaEgreso ? new Date(h.fechaEgreso) : new Date();
    return Math.floor((to.getTime() - from.getTime()) / (1000 * 60 * 60 * 24));
  }

  diasClass(h: HospitalizacionDto): string {
    if (h.estado === 'EGRESADO') return 'dias--neutral';
    const d = this.diasEstancia(h);
    if (d <= 3)  return 'dias--ok';
    if (d <= 7)  return 'dias--warn';
    return 'dias--danger';
  }

  estadoLabel(estado?: string): string {
    const map: Record<string, string> = {
      INGRESADO: 'Ingresado', EGRESADO: 'Egresado', OBSERVACION: 'Observación',
    };
    return map[estado ?? ''] ?? (estado ?? '—');
  }

  estadoBadgeClass(estado?: string): string {
    const map: Record<string, string> = {
      INGRESADO: 'badge--ingresado', EGRESADO: 'badge--egresado', OBSERVACION: 'badge--observacion',
    };
    return map[estado ?? ''] ?? 'badge--default';
  }

  formatFecha(iso?: string): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  formatFechaHora(iso?: string): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleString('es-CO', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' });
  }

  contarEstado(estado: string): number {
    return this.hospitalizaciones().filter(h => h.estado === estado).length;
  }

  trackById(_: number, h: HospitalizacionDto): number { return h.id; }

  // ── Helpers privados ──────────────────────────────────────────────────────
  private emptyMed(): MedicamentoOrden {
    return { nombre: '', dosis: '', via: 'VO', frecuencia: 'Cada 8h', duracion: '', notas: '' };
  }

  private emptyEvolucion(): EvolucionData {
    return { estadoGeneral: '', ta_s: '', ta_d: '', fc: '', fr: '', temp: '', spo2: '', observaciones: '' };
  }

  private nowLabel(): string {
    return new Date().toLocaleString('es-CO', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  }

  private todayStr(): string {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }
}
