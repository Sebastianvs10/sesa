/**
 * Historia Clínica Premium — SOAP clínico, header médico, timeline, skeletons, spinners.
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';

import { PacienteService, PacienteDto } from '../../core/services/paciente.service';
import { HistoriaClinicaService, HistoriaClinicaDto } from '../../core/services/historia-clinica.service';
import { SesaJspdfService, SesaPdfPaciente, SesaPdfHistoriaClinica, SesaPdfBranding } from '../../core/services/sesa-jspdf.service';
import { EmpresaCurrentService } from '../../core/services/empresa-current.service';
import { EmpresaService } from '../../core/services/empresa.service';
import { AuthService } from '../../core/services/auth.service';
import { CitaDto, CitaService } from '../../core/services/cita.service';
import { ConsultaDto, ConsultaService } from '../../core/services/consulta.service';
import { EvolucionDto, EvolucionService } from '../../core/services/evolucion.service';
import { OrdenClinicaDto, OrdenClinicaService, OrdenClinicaItemDto } from '../../core/services/orden-clinica.service';
import { FacturaDto, FacturaService } from '../../core/services/factura.service';
import { ReporteResumenDto, ReporteService } from '../../core/services/reporte.service';
import { PersonalDto, PersonalService } from '../../core/services/personal.service';
import { DoloresPanelComponent } from './dolores-panel/dolores-panel.component';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';
import { PlantillaSoapService, PlantillaSoapDto } from '../../core/services/plantilla-soap.service';
import { SesaConfirmDialogService } from '../../shared/components/sesa-confirm-dialog/sesa-confirm-dialog.component';
import { SesaSkeletonComponent } from '../../shared/components/sesa-skeleton/sesa-skeleton.component';
import { SesaEmptyStateComponent } from '../../shared/components/sesa-empty-state/sesa-empty-state.component';
import { SesaRdaPanelComponent } from '../../shared/components/sesa-rda-panel/sesa-rda-panel.component';
import { SesaConsentimientoComponent } from '../../shared/components/sesa-consentimiento/sesa-consentimiento.component';
import { parseResultadoToItems } from '../../core/utils/resultado-display.util';
import { forkJoin } from 'rxjs';

export type HistoriaTab = 'historia' | 'soap' | 'ordenes' | 'documentos' | 'dolores' | 'consentimiento';
export type TipoOrden = 'LABORATORIO' | 'MEDICAMENTO' | 'PROCEDIMIENTO' | 'IMAGEN';

/** Ítem del timeline unificado: consulta (consultorio) o evolución (urgencias). */
export type TimelineItem = { tipo: 'consulta'; data: ConsultaDto } | { tipo: 'evolucion'; data: EvolucionDto };

/** Códigos CIE-10 frecuentes para búsqueda asistida (RIPS). */
const CIE10_FRECUENTES: { code: string; desc: string }[] = [
  { code: 'J06.9', desc: 'Infección aguda vías respiratorias superiores' },
  { code: 'J00', desc: 'Rinofaringitis aguda [resfriado común]' },
  { code: 'J02.9', desc: 'Faringitis aguda no especificada' },
  { code: 'I10', desc: 'Hipertensión esencial' },
  { code: 'E11.9', desc: 'Diabetes mellitus tipo 2 sin complicaciones' },
  { code: 'K59.0', desc: 'Constipación' },
  { code: 'K29.7', desc: 'Gastritis no especificada' },
  { code: 'R51', desc: 'Cefalea' },
  { code: 'M54.5', desc: 'Lumbago no especificado' },
  { code: 'A09', desc: 'Diarrea y gastroenteritis presunta infecciosa' },
  { code: 'R50.9', desc: 'Fiebre no especificada' },
  { code: 'Z00.00', desc: 'Control general de salud' },
  { code: 'Z23', desc: 'Encuentro para inmunización' },
  { code: 'F41.1', desc: 'Trastorno de ansiedad generalizada' },
  { code: 'N39.0', desc: 'Infección de vías urinarias no especificada' },
  { code: 'G43.9', desc: 'Migraña no especificada' },
];

@Component({
  standalone: true,
  selector: 'sesa-historia-clinica-page',
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    DoloresPanelComponent,
    SesaSkeletonComponent,
    SesaEmptyStateComponent,
    SesaRdaPanelComponent,
    SesaConsentimientoComponent,
  ],
  templateUrl: './historia-clinica.page.html',
  styleUrl: './historia-clinica.page.scss',
})
export class HistoriaClinicaPageComponent implements OnInit {
  /** Expuesto para la plantilla: resultado por ítem (etiqueta en negrita + valor). */
  protected readonly parseResultadoToItems = parseResultadoToItems;

  private readonly route          = inject(ActivatedRoute);
  private readonly router         = inject(Router);
  private readonly pacienteService   = inject(PacienteService);
  private readonly historiaService   = inject(HistoriaClinicaService);
  readonly authService               = inject(AuthService);
  private readonly citaService       = inject(CitaService);
  private readonly consultaService   = inject(ConsultaService);
  private readonly evolucionService  = inject(EvolucionService);
  private readonly ordenService      = inject(OrdenClinicaService);
  private readonly facturaService    = inject(FacturaService);
  private readonly reporteService    = inject(ReporteService);
  private readonly personalService   = inject(PersonalService);
  private readonly toast             = inject(SesaToastService);
  private readonly plantillaSoapService = inject(PlantillaSoapService);
  private readonly confirmDialog     = inject(SesaConfirmDialogService);
  private readonly sesaJspdf         = inject(SesaJspdfService);
  private readonly empresaCurrent   = inject(EmpresaCurrentService);
  private readonly empresaService   = inject(EmpresaService);

  descargandoPdf = signal(false);
  descargandoPdfEvolucion = signal(false);
  imprimiendoPdfEvolucion = signal(false);
  imprimiendoPdf = signal(false);

  /* ── Estado global ─────────────────────────────────────────────────── */
  loadingPatient  = signal(false);
  loadingFlujo    = signal(false);
  loadingMisConsultas = signal(false);

  selectedPatient    = signal<PacienteDto | null>(null);
  historiaClinica    = signal<HistoriaClinicaDto | null>(null);
  profesionales: PersonalDto[] = [];
  citasPaciente: CitaDto[]       = [];
  consultasPaciente: ConsultaDto[] = [];
  evolucionesPaciente: EvolucionDto[] = [];
  misConsultas: ConsultaDto[]    = [];
  ordenesPaciente: OrdenClinicaDto[] = [];
  ordenesFiltradas   = signal<OrdenClinicaDto[]>([]);
  facturasPaciente: FacturaDto[] = [];
  resumen: ReporteResumenDto | null = null;

  /** Se incrementa al cargar consultas/órdenes/facturas del paciente para que los computed de totales se re-ejecuten. */
  private flujoDataVersion = signal(0);

  /* Búsqueda de pacientes (vista sin paciente seleccionado) */
  searchPacienteQuery   = signal('');
  searchPacienteResults = signal<PacienteDto[]>([]);
  searchPacienteLoading = signal(false);
  searchPacienteSearched = signal(false);

  error: string | null = null;

  /** Si la navegación incluyó consultaRapida=true (p. ej. desde Consulta Médica), abrir tab SOAP al cargar. */
  private consultaRapidaRequested = false;

  /* ── Tabs ─────────────────────────────────────────────────────────── */
  activeTab = signal<HistoriaTab>('historia');

  /* ── Acciones en proceso ─────────────────────────────────────────── */
  savingConsulta   = signal(false);
  savingOrden      = signal(false);
  savingFactura    = signal(false);
  savingCerrarHC   = signal(false);
  editandoHC       = signal(false);
  savingUpdateHC   = signal(false);

  /* ── Filtro órdenes ───────────────────────────────────────────────── */
  filterTipoOrden = signal<string>('');

  /* ── Vista unificada: filtro por tipo de atención ─────────────────── */
  filterTipoAtencion = signal<string>('TODAS');

  /** Opciones de tipo de consulta para filtro y etiquetas (RIPS). */
  protected readonly TIPOS_ATENCION: { value: string; label: string }[] = [
    { value: 'TODAS', label: 'Todas' },
    { value: 'PRIMERA_VEZ', label: 'Primera vez' },
    { value: 'CONTROL', label: 'Control' },
    { value: 'URGENCIA', label: 'Urgencia' },
    { value: 'TELECONSULTA', label: 'Teleconsulta' },
  ];

  /* ── Subir resultado de orden ─────────────────────────────────────── */
  ordenResultadoEditId = signal<number | null>(null);
  resultadoOrdenTexto = '';
  savingResultadoOrden = signal(false);
  descargandoPdfOrdenes = signal(false);
  imprimiendoPdfOrdenes = signal(false);
  /** ID de la orden para la cual se está generando PDF/impresión individual (null = ninguna). */
  ordenPdfIndividualId = signal<number | null>(null);

  /** ID de la orden con detalle expandido (click para ver ítems). */
  ordenDetalleAbiertoId = signal<number | null>(null);
  /** ID de la orden mostrada en el modal de detalle (al hacer clic en "Ver detalle"). */
  ordenModalId = signal<number | null>(null);
  /** ID de la orden en modo edición (solo órdenes de un solo ítem, no compuestas). */
  ordenEditandoId = signal<number | null>(null);
  ordenEditandoForm = {
    tipo: '',
    detalle: '',
    cantidadPrescrita: null as number | null,
    unidadMedida: '',
    frecuencia: '',
    duracionDias: null as number | null,
    valorEstimado: null as number | null,
  };
  savingOrdenEdit = signal(false);

  /** Plantillas SOAP (catálogo) para rellenar nota clínica (Res. 1995/1999). */
  plantillasSoap: PlantillaSoapDto[] = [];
  /** ID de plantilla seleccionada en el selector "Usar plantilla" (null = ninguna). */
  selectedPlantillaSoapId: number | null = null;

  /* ── Formulario SOAP (Consulta) ────────────────────────────────────
     Sección S — Subjetivo                                              */
  soapS = {
    citaId:               null as number | null,
    profesionalId:        null as number | null,
    motivoConsulta:       '',
    enfermedadActual:     '',
    revisionSistemas:     '',
    antecedentesPersonales: '',
    antecedentesFamiliares: '',
    alergias:             '',
  };

  /* Sección O — Objetivo (Signos Vitales) */
  soapO = {
    presionArterial:       '',
    frecuenciaCardiaca:    '',
    frecuenciaRespiratoria:'',
    temperatura:           '',
    saturacionO2:          '',
    peso:                  '',
    talla:                 '',
    imc:                   '',
    dolorEva:              '',
    perimetroAbdominal:    '',
    perimetroCefalico:     '',
    hallazgosExamen:       '',
  };

  /* Sección A — Análisis */
  soapA = {
    tipoConsulta:           '',
    diagnostico:            '',
    codigoCie10:            '',
    codigoCie10Secundario:  '',
    observaciones:          '',
  };

  /* Sección P — Plan */
  soapP = {
    planTratamiento:        '',
    tratamientoFarmacologico: '',
    recomendaciones:        '',
  };

  /** Lista de códigos CIE-10 frecuentes para datalist (búsqueda asistida). */
  protected readonly cie10Frecuentes = CIE10_FRECUENTES;

  /** Normaliza valor CIE-10 si el usuario eligió "CODE - Descripción" del datalist. */
  onCie10Input(raw: string): void {
    const idx = raw.indexOf(' - ');
    this.soapA.codigoCie10 = (idx >= 0 ? raw.slice(0, idx).trim() : raw).toUpperCase();
  }

  soapSectionOpen = signal<'S' | 'O' | 'A' | 'P' | null>('S');

  /* ── Orden clínica ────────────────────────────────────────────────── */
  /** Ítems agregados a la orden actual (varios por emisión). */
  ordenItems: OrdenClinicaItemDto[] = [];

  ordenForm = {
    consultaId:        null as number | null,
    tipo:              'LABORATORIO' as string,
    plantilla:         '',
    detalle:           '',
    cantidadPrescrita: null as number | null,
    unidadMedida:      '',
    frecuencia:        '',
    duracionDias:      null as number | null,
    valorEstimado:     '',
  };

  /** Unidades de medida para órdenes de medicamento (presentación profesional). */
  readonly unidadesMedida = [
    { id: 'TAB', label: 'Tabletas' },
    { id: 'CAP', label: 'Cápsulas' },
    { id: 'ML', label: 'ml (líquido)' },
    { id: 'GOTAS', label: 'Gotas' },
    { id: 'FRASCO', label: 'Frasco(s)' },
    { id: 'SOBRE', label: 'Sobre(s)' },
    { id: 'UNIDAD', label: 'Unidad(es)' },
    { id: 'APLIC', label: 'Aplicación(es)' },
  ];

  /** Opciones de frecuencia habituales para prescripción. */
  readonly frecuenciasComunes = [
    'Cada 6 horas', 'Cada 8 horas', 'Cada 12 horas', 'Cada 24 horas (1 vez al día)',
    'Cada 12 h por 7 días', 'Cada 8 h por 5 días', 'En ayunas', 'Con las comidas',
    'Cada noche al acostarse', 'Cada 8 h si hay dolor',
  ];

  /* ── Factura ──────────────────────────────────────────────────────── */
  facturaForm = {
    ordenId:     null as number | null,
    valorTotal:  '',
    descripcion: '',
  };

  /* ── Edición HC ───────────────────────────────────────────────────── */
  hcEditForm = {
    grupoSanguineo:             '',
    alergiasGenerales:          '',
    antecedentesPersonales:     '',
    antecedentesQuirurgicos:    '',
    antecedentesFarmacologicos: '',
    antecedentesTraumaticos:    '',
    antecedentesGinecoobstetricos: '',
    antecedentesFamiliares:     '',
    habitosTabaco:    false,
    habitosAlcohol:   false,
    habitosSustancias:false,
    habitosDetalles:  '',
  };

  /* ── Catálogos ────────────────────────────────────────────────────── */
  readonly catalogoLaboratorios = [
    'Hemograma completo', 'Química sanguínea', 'Perfil lipídico',
    'Glicemia', 'TSH', 'Parcial de orina', 'PCR / VSG', 'Ferritina',
    'Creatinina / BUN', 'Electrolitos', 'Pruebas de función hepática',
  ];

  readonly catalogoMedicamentos = [
    'Acetaminofén 500 mg', 'Ibuprofeno 400 mg', 'Naproxeno 550 mg',
    'Amoxicilina 500 mg', 'Azitromicina 500 mg', 'Ciprofloxacina 500 mg',
    'Omeprazol 20 mg', 'Metoclopramida 10 mg', 'Losartán 50 mg',
    'Enalapril 10 mg', 'Metformina 850 mg', 'Levotiroxina 50 mcg',
    'Salbutamol inhalador', 'Prednisolona 5 mg', 'Tramadol 50 mg',
  ];

  readonly catalogoEspecialidades = [
    'Medicina interna', 'Pediatría', 'Ginecología', 'Cardiología',
    'Neurología', 'Ortopedia', 'Dermatología', 'Psicología',
    'Nutrición', 'Oftalmología', 'Otorrinolaringología',
  ];

  readonly gruposSanguineos = ['A+', 'A−', 'B+', 'B−', 'AB+', 'AB−', 'O+', 'O−'];

  /* ── Computed ─────────────────────────────────────────────────────── */
  get canCreateHistoria(): boolean {
    const role = this.authService.currentUser()?.role ?? '';
    return ['MEDICO', 'ADMIN', 'SUPERADMINISTRADOR'].includes(role);
  }

  get canStartNewHistoria(): boolean {
    return !this.historiaClinica() && this.canCreateHistoria;
  }

  totalConsultas = computed(() => {
    this.flujoDataVersion();
    return this.consultasPaciente.length + this.evolucionesPaciente.length;
  });
  totalOrdenes   = computed(() => {
    this.flujoDataVersion();
    return this.ordenesPaciente.length;
  });
  totalFacturas  = computed(() => {
    this.flujoDataVersion();
    return this.facturasPaciente.length;
  });

  /** Cantidad de ítems que se emitirán (lista + ítem actual del formulario si tiene detalle). */
  get totalOrdenItemsCount(): number {
    const enFormulario = !!(this.ordenForm.detalle?.trim() || this.ordenForm.plantilla?.trim());
    return this.ordenItems.length + (enFormulario ? 1 : 0);
  }

  hcEstadoBadge = computed(() => {
    const estado = (this.historiaClinica()?.estado ?? '').toUpperCase();
    if (estado === 'ACTIVA' || estado === 'ABIERTA') return 'badge--success';
    if (estado === 'CERRADA') return 'badge--danger';
    return 'badge--info';
  });

  /* ── Lifecycle ────────────────────────────────────────────────────── */
  ngOnInit(): void {
    this.personalService.list(0, 300).subscribe({
      next: (res) => (this.profesionales = res.content ?? []),
      error: () => (this.profesionales = []),
    });
    this.plantillaSoapService.listarActivas().subscribe({
      next: (list) => (this.plantillasSoap = list ?? []),
      error: () => (this.plantillasSoap = []),
    });

    this.route.queryParams.subscribe((params) => {
      const pacienteId = params['pacienteId'];
      this.consultaRapidaRequested = params['consultaRapida'] === 'true';
      if (pacienteId) {
        this.loadPaciente(parseInt(pacienteId, 10));
      } else {
        this.loadMisConsultas();
        this.loadResumenCuandoSinPaciente();
      }
    });
  }

  /* ── Carga de datos ───────────────────────────────────────────────── */
  private loadMisConsultas(): void {
    this.loadingMisConsultas.set(true);
    this.consultaService.listMisConsultas(0, 50).subscribe({
      next: (list) => {
        this.misConsultas = list ?? [];
        this.loadingMisConsultas.set(false);
      },
      error: (err) => {
        this.error = err?.error?.error || 'Error al cargar consultas';
        this.misConsultas = [];
        this.loadingMisConsultas.set(false);
      },
    });
  }

  /** Carga resumen global cuando no hay paciente seleccionado (datos del usuario/empresa). */
  private loadResumenCuandoSinPaciente(): void {
    this.reporteService.resumen().subscribe({
      next: (res) => (this.resumen = res),
      error: () => (this.resumen = null),
    });
  }

  buscarPacientes(): void {
    const q = this.searchPacienteQuery().trim();
    if (!q || q.length < 2) {
      this.searchPacienteResults.set([]);
      this.searchPacienteSearched.set(false);
      return;
    }
    this.searchPacienteLoading.set(true);
    this.searchPacienteSearched.set(true);
    this.pacienteService.list(0, 20, q).subscribe({
      next: (page) => {
        this.searchPacienteResults.set(page?.content ?? []);
        this.searchPacienteLoading.set(false);
      },
      error: () => {
        this.searchPacienteResults.set([]);
        this.searchPacienteLoading.set(false);
      },
    });
  }

  private loadPaciente(pacienteId: number): void {
    this.loadingPatient.set(true);
    this.error = null;
    this.pacienteService.get(pacienteId).subscribe({
      next: (paciente) => {
        this.selectedPatient.set(paciente);
        this.loadHistoria(pacienteId);
      },
      error: (err) => {
        this.error = err.error?.error || 'Error al cargar paciente';
        this.loadingPatient.set(false);
      },
    });
  }

  private loadHistoria(pacienteId: number): void {
    this.historiaService.getByPacienteOrNull(pacienteId).subscribe({
      next: (historia) => {
        this.historiaClinica.set(historia ?? null);
        if (historia) this._populateHcEditForm(historia);
        this.loadingPatient.set(false);
        this.loadFlujoClinico(pacienteId);
      },
      error: () => {
        this.historiaClinica.set(null);
        this.loadingPatient.set(false);
        this.loadFlujoClinico(pacienteId);
      },
    });
  }

  private loadFlujoClinico(pacienteId: number): void {
    this.loadingFlujo.set(true);
    this.citaService.list().subscribe({
      next: (citas: CitaDto[]) => {
        this.citasPaciente = (citas ?? []).filter((c: CitaDto) => c.pacienteId === pacienteId);
        forkJoin({
          consultas: this.consultaService.listByPaciente(pacienteId, 0, 100),
          evoluciones: this.evolucionService.listarPorPaciente(pacienteId),
        }).subscribe({
          next: ({ consultas, evoluciones }) => {
            this.consultasPaciente = Array.isArray(consultas) ? consultas : [];
            this.evolucionesPaciente = Array.isArray(evoluciones) ? evoluciones : [];
            this.ordenService.listByPaciente(pacienteId).subscribe({
              next: (ordenes) => {
                this.ordenesPaciente = ordenes ?? [];
                this._applyOrdenFilter();
                this.facturaService.listByPaciente(pacienteId).subscribe({
                  next: (facturas) => {
                    this.facturasPaciente = facturas ?? [];
                    this._autoSeleccionarFlujo();
                    this._aplicarVistaUnicaConsultaRapida();
                    this.refreshResumen();
                    this.flujoDataVersion.update((v) => v + 1);
                    this.loadingFlujo.set(false);
                    this.loadingPatient.set(false);
                  },
                  error: (err: unknown) => this._setLoadError(err, 'Error al cargar facturas'),
                });
              },
              error: (err: unknown) => this._setLoadError(err, 'Error al cargar órdenes'),
            });
          },
          error: (err: unknown) => this._setLoadError(err, 'Error al cargar consultas o evoluciones'),
        });
      },
      error: (err: unknown) => this._setLoadError(err, 'Error al cargar citas'),
    });
  }

  private _setLoadError(err: unknown, fallback: string): void {
    this.error = (err as { error?: { error?: string } })?.error?.error || fallback;
    this.loadingFlujo.set(false);
    this.loadingPatient.set(false);
    this.toast.error(this.error!, 'Error de carga');
  }

  private _autoSeleccionarFlujo(): void {
    if (this.citasPaciente.length > 0 && !this.soapS.citaId) {
      const pendiente = this.citasPaciente.find((c) => (c.estado ?? '').toUpperCase() !== 'ATENDIDO');
      const cita = pendiente ?? this.citasPaciente[0];
      this.soapS.citaId = cita.id;
      this.soapS.profesionalId = cita.profesionalId ?? null;
    }
    if (!this.soapS.profesionalId) {
      this.soapS.profesionalId = this.authService.currentUser()?.personalId ?? null;
    }
    if (this.consultasPaciente.length > 0 && !this.ordenForm.consultaId) {
      this.ordenForm.consultaId = this.consultasPaciente[0].id;
    }
    if (this.ordenesPaciente.length > 0 && !this.facturaForm.ordenId) {
      this.facturaForm.ordenId = this.ordenesPaciente[0].id;
    }
  }

  /** Vista única / consulta rápida: abre tab Nota SOAP y sección S si viene desde Consulta Médica o si no hay atención hoy. */
  private _aplicarVistaUnicaConsultaRapida(): void {
    if (this.consultaRapidaRequested) {
      this.consultaRapidaRequested = false;
      this.setTab('soap');
      this.soapSectionOpen.set('S');
      return;
    }
    if (this.historiaClinica() && !this.consultaHoy) {
      this.setTab('soap');
      this.soapSectionOpen.set('S');
    }
  }

  private _populateHcEditForm(hc: HistoriaClinicaDto): void {
    this.hcEditForm.grupoSanguineo         = hc.grupoSanguineo ?? '';
    this.hcEditForm.alergiasGenerales      = hc.alergiasGenerales ?? '';
    this.hcEditForm.antecedentesPersonales = hc.antecedentesPersonales ?? '';
    this.hcEditForm.antecedentesFamiliares = hc.antecedentesFamiliares ?? '';
  }

  /* ── Tabs ─────────────────────────────────────────────────────────── */
  setTab(tab: HistoriaTab): void {
    this.activeTab.set(tab);
    this.error = null;
    if (tab === 'soap') {
      if (!this.soapS.profesionalId) {
        this.soapS.profesionalId = this.authService.currentUser()?.personalId ?? null;
      }
    }
    if (tab === 'soap' && this.historiaClinica()) {
      const hc = this.historiaClinica()!;
      if (!this.soapS.alergias?.trim() && hc.alergiasGenerales?.trim())
        this.soapS.alergias = hc.alergiasGenerales;
      if (!this.soapS.antecedentesPersonales?.trim() && hc.antecedentesPersonales?.trim())
        this.soapS.antecedentesPersonales = hc.antecedentesPersonales;
      if (!this.soapS.antecedentesFamiliares?.trim() && hc.antecedentesFamiliares?.trim())
        this.soapS.antecedentesFamiliares = hc.antecedentesFamiliares;
    }
  }

  toggleSoapSection(sec: 'S' | 'O' | 'A' | 'P'): void {
    this.soapSectionOpen.set(this.soapSectionOpen() === sec ? null : sec);
  }

  /* ── SOAP — Guardar consulta ─────────────────────────────────────── */
  crearConsulta(): void {
    if (!this.selectedPatient()?.id || !this.soapS.profesionalId || !this.soapS.motivoConsulta.trim()) {
      this.toast.warning('Completa el profesional y motivo de consulta.', 'Campos requeridos');
      return;
    }
    const faltanRips = !this.soapA.codigoCie10?.trim() || !this.soapA.tipoConsulta?.trim();
    if (faltanRips) {
      this.confirmDialog.confirm({
        title: 'Campos recomendados para RIPS',
        message: 'Faltan código CIE-10 y/o tipo de consulta. Son necesarios para reportes y cobro. ¿Desea guardar de todos modos?',
        type: 'warning',
        confirmLabel: 'Guardar de todos modos',
      }).then((ok) => {
        if (ok) this._crearConsultaReal();
      });
      return;
    }
    this._crearConsultaReal();
  }

  private _crearConsultaReal(): void {
    const signosVitalesTexto = this._buildSignosVitalesTexto();
    const enfermedadCompleta  = [
      signosVitalesTexto,
      this.soapO.hallazgosExamen ? `Hallazgos examen físico: ${this.soapO.hallazgosExamen}` : '',
      this.soapA.diagnostico    ? `Diagnóstico: ${this.soapA.diagnostico}` : '',
      this.soapA.codigoCie10    ? `CIE-10: ${this.soapA.codigoCie10}` : '',
      this.soapP.planTratamiento ? `Plan: ${this.soapP.planTratamiento}` : '',
      this.soapP.tratamientoFarmacologico ? `Farmacológico: ${this.soapP.tratamientoFarmacologico}` : '',
      this.soapP.recomendaciones ? `Recomendaciones: ${this.soapP.recomendaciones}` : '',
      this.soapS.enfermedadActual,
    ].filter(Boolean).join('\n\n');

    this.savingConsulta.set(true);
    this.consultaService.create({
      pacienteId:             this.selectedPatient()!.id,
      profesionalId:          this.soapS.profesionalId ?? undefined,
      citaId:                 this.soapS.citaId || undefined,
      motivoConsulta:         this.soapS.motivoConsulta.trim(),
      enfermedadActual:       enfermedadCompleta.trim() || undefined,
      antecedentesPersonales: this.soapS.antecedentesPersonales.trim() || undefined,
      antecedentesFamiliares: this.soapS.antecedentesFamiliares.trim() || undefined,
      alergias:               this.soapS.alergias.trim() || undefined,
      tipoConsulta:           this.soapA.tipoConsulta || undefined,
      codigoCie10:            this.soapA.codigoCie10.trim() || undefined,
      codigoCie10Secundario:  this.soapA.codigoCie10Secundario.trim() || undefined,
      diagnostico:            this.soapA.diagnostico.trim() || undefined,
      observacionesClincias:  this.soapA.observaciones.trim() || undefined,
      dolorEva:               this.soapO.dolorEva || undefined,
      perimetroAbdominal:     this.soapO.perimetroAbdominal || undefined,
      perimetroCefalico:      this.soapO.perimetroCefalico || undefined,
      saturacionO2:           this.soapO.saturacionO2 ? String(this.soapO.saturacionO2) : undefined,
      presionArterial:        this.soapO.presionArterial || undefined,
      frecuenciaCardiaca:     this.soapO.frecuenciaCardiaca ? String(this.soapO.frecuenciaCardiaca) : undefined,
      frecuenciaRespiratoria: this.soapO.frecuenciaRespiratoria ? String(this.soapO.frecuenciaRespiratoria) : undefined,
      temperatura:            this.soapO.temperatura ? String(this.soapO.temperatura) : undefined,
      peso:                   this.soapO.peso ? String(this.soapO.peso) : undefined,
      talla:                  this.soapO.talla ? String(this.soapO.talla) : undefined,
      imc:                    this.soapO.imc || undefined,
      hallazgosExamen:        this.soapO.hallazgosExamen.trim() || undefined,
      planTratamiento:        this.soapP.planTratamiento.trim() || undefined,
      tratamientoFarmacologico: this.soapP.tratamientoFarmacologico.trim() || undefined,
      recomendaciones:        this.soapP.recomendaciones.trim() || undefined,
    }).subscribe({
      next: (consulta) => {
        this.savingConsulta.set(false);
        this.toast.success('Nota SOAP creada y guardada en el flujo clínico.', 'Consulta registrada');
        this._resetSoap();
        this.ordenForm.consultaId = consulta.id;
        this.loadFlujoClinico(this.selectedPatient()!.id);
      },
      error: (err) => {
        this.savingConsulta.set(false);
        this.toast.error((err as { error?: { error?: string } })?.error?.error || 'No se pudo guardar la consulta.', 'Error');
      },
    });
  }

  private _buildSignosVitalesTexto(): string {
    const sv = this.soapO;
    const partes: string[] = [];
    if (sv.presionArterial)        partes.push(`TA: ${sv.presionArterial} mmHg`);
    if (sv.frecuenciaCardiaca)     partes.push(`FC: ${sv.frecuenciaCardiaca} lpm`);
    if (sv.frecuenciaRespiratoria) partes.push(`FR: ${sv.frecuenciaRespiratoria} rpm`);
    if (sv.temperatura)            partes.push(`Temp: ${sv.temperatura} °C`);
    if (sv.saturacionO2)           partes.push(`SpO₂: ${sv.saturacionO2}%`);
    if (sv.peso)                   partes.push(`Peso: ${sv.peso} kg`);
    if (sv.talla)                  partes.push(`Talla: ${sv.talla} cm`);
    if (sv.imc)                    partes.push(`IMC: ${sv.imc}`);
    if (sv.dolorEva)               partes.push(`Dolor EVA: ${sv.dolorEva}/10`);
    if (sv.perimetroAbdominal)     partes.push(`Perímetro abd.: ${sv.perimetroAbdominal} cm`);
    if (sv.perimetroCefalico)      partes.push(`Perímetro cef.: ${sv.perimetroCefalico} cm`);
    return partes.length > 0 ? `Signos vitales — ${partes.join(' · ')}` : '';
  }

  private _resetSoap(): void {
    this.soapS.motivoConsulta = '';
    this.soapS.enfermedadActual = '';
    this.soapS.revisionSistemas = '';
    this.soapS.antecedentesPersonales = '';
    this.soapS.antecedentesFamiliares = '';
    this.soapS.alergias = '';
    Object.assign(this.soapO, {
      presionArterial:'', frecuenciaCardiaca:'', frecuenciaRespiratoria:'',
      temperatura:'', saturacionO2:'', peso:'', talla:'', imc:'',
      dolorEva:'', perimetroAbdominal:'', perimetroCefalico:'', hallazgosExamen:'',
    });
    Object.assign(this.soapA, { tipoConsulta:'', diagnostico:'', codigoCie10:'', codigoCie10Secundario:'', observaciones:'' });
    Object.assign(this.soapP, { planTratamiento:'', tratamientoFarmacologico:'', recomendaciones:'' });
    this.selectedPlantillaSoapId = null;
    this.soapSectionOpen.set('S');
  }

  /** Aplica una plantilla SOAP al formulario (motivo, S/O/A/P y CIE-10 sugerido). Res. 1995/1999. */
  aplicarPlantillaSoap(plantilla: PlantillaSoapDto | null): void {
    if (!plantilla) return;
    if (plantilla.motivoTipo) this.soapS.motivoConsulta = plantilla.nombre;
    if (plantilla.contenidoSubjetivo) this.soapS.enfermedadActual = plantilla.contenidoSubjetivo;
    if (plantilla.contenidoObjetivo) this.soapO.hallazgosExamen = plantilla.contenidoObjetivo;
    if (plantilla.contenidoAnalisis) this.soapA.observaciones = plantilla.contenidoAnalisis;
    if (plantilla.contenidoPlan) this.soapP.planTratamiento = plantilla.contenidoPlan;
    if (plantilla.codigoCie10Sugerido) this.soapA.codigoCie10 = plantilla.codigoCie10Sugerido;
    if (plantilla.motivoTipo && this.TIPOS_ATENCION.some(t => t.value === plantilla.motivoTipo))
      this.soapA.tipoConsulta = plantilla.motivoTipo;
    this.soapSectionOpen.set('S');
    this.toast.success('Plantilla aplicada. Revise y complete los campos.');
  }

  onPlantillaSoapChange(id: number | null): void {
    if (id == null) return;
    const p = this.plantillasSoap.find((x) => x.id === id) ?? null;
    this.aplicarPlantillaSoap(p);
  }

  /* ── IMC automático ─────────────────────────────────────────────── */
  calcularIMC(): void {
    const peso  = parseFloat(this.soapO.peso);
    const talla = parseFloat(this.soapO.talla);
    if (peso > 0 && talla > 0) {
      const tallaMt = talla / 100;
      this.soapO.imc = (peso / (tallaMt * tallaMt)).toFixed(1);
    }
  }

  /* ── Órdenes ─────────────────────────────────────────────────────── */
  setFilterTipoOrden(tipo: string): void {
    this.filterTipoOrden.set(tipo === this.filterTipoOrden() ? '' : tipo);
    this._applyOrdenFilter();
  }

  private _applyOrdenFilter(): void {
    const f = this.filterTipoOrden();
    this.ordenesFiltradas.set(
      f
        ? this.ordenesPaciente.filter((o) => {
            if (o.tipo === f) return true;
            if (o.tipo === 'COMPUESTA' && o.items?.some((it) => it.tipo === f)) return true;
            return false;
          })
        : [...this.ordenesPaciente]
    );
  }

  onTipoOrdenChange(): void {
    this.ordenForm.plantilla = '';
    this.ordenForm.detalle = '';
    if (this.ordenForm.tipo !== 'MEDICAMENTO') {
      this.ordenForm.cantidadPrescrita = null;
      this.ordenForm.unidadMedida = '';
      this.ordenForm.frecuencia = '';
      this.ordenForm.duracionDias = null;
    }
  }

  /** Texto resumido para orden tipo MEDICAMENTO: cantidad + unidad + frecuencia + duración. */
  formatoOrdenMedicamento(o: OrdenClinicaDto): string {
    if (o.tipo !== 'MEDICAMENTO') return '';
    const parts: string[] = [];
    if (o.cantidadPrescrita != null) {
      parts.push(o.unidadMedida ? `${o.cantidadPrescrita} ${o.unidadMedida}` : `${o.cantidadPrescrita}`);
    }
    if (o.frecuencia?.trim()) parts.push(o.frecuencia.trim());
    if (o.duracionDias != null) parts.push(`${o.duracionDias} días`);
    return parts.join(' · ');
  }

  /** Ítems a mostrar para una orden: si tiene items[] los usa, si no (legacy) uno sintético desde la cabecera. */
  getOrdenDisplayItems(o: OrdenClinicaDto): OrdenClinicaItemDto[] {
    if (o.items && o.items.length > 0) return o.items;
    return [{
      tipo: o.tipo,
      detalle: o.detalle,
      cantidadPrescrita: o.cantidadPrescrita,
      unidadMedida: o.unidadMedida,
      frecuencia: o.frecuencia,
      duracionDias: o.duracionDias,
      valorEstimado: o.valorEstimado,
    }];
  }

  /** Texto resumido para ítem tipo MEDICAMENTO. */
  formatoOrdenMedicamentoFromItem(item: OrdenClinicaItemDto): string {
    if (item.tipo !== 'MEDICAMENTO') return '';
    const parts: string[] = [];
    if (item.cantidadPrescrita != null) {
      parts.push(item.unidadMedida ? `${item.cantidadPrescrita} ${item.unidadMedida}` : `${item.cantidadPrescrita}`);
    }
    if (item.frecuencia?.trim()) parts.push(item.frecuencia.trim());
    if (item.duracionDias != null) parts.push(`${item.duracionDias} días`);
    return parts.join(' · ');
  }

  aplicarPlantillaOrden(): void {
    if (!this.ordenForm.plantilla) return;
    const label: Record<string, string> = {
      LABORATORIO: 'Solicitar laboratorio',
      MEDICAMENTO: 'Medicamento indicado',
      PROCEDIMIENTO: 'Procedimiento',
      IMAGEN: 'Imagen diagnóstica solicitada',
    };
    this.ordenForm.detalle = `${label[this.ordenForm.tipo] ?? 'Orden'}: ${this.ordenForm.plantilla}`;
  }

  /** Añade el ítem actual del formulario a la lista de la orden (sin emitir aún). */
  agregarItemOrden(): void {
    if (!this.ordenForm.consultaId) {
      this.toast.warning('Selecciona una consulta para asociar la orden.', 'Campo requerido');
      return;
    }
    const detalle = (this.ordenForm.detalle?.trim() || this.ordenForm.plantilla?.trim()) || '';
    if (!detalle) {
      this.toast.warning('Indica al menos el detalle o elige una plantilla antes de agregar.', 'Falta detalle');
      return;
    }
    if (this.ordenForm.tipo === 'MEDICAMENTO' && this.historiaClinica()?.alergiasGenerales?.trim()) {
      const textoOrden = `${this.ordenForm.detalle ?? ''} ${this.ordenForm.plantilla ?? ''}`.toLowerCase();
      const alergiasLista = this.alergiasTags(this.historiaClinica()!.alergiasGenerales!);
      const coincideAlergia = alergiasLista.some((a) => textoOrden.includes(a.toLowerCase()));
      if (coincideAlergia) {
        this.confirmDialog
          .confirm({
            title: 'Alerta de alergia',
            message: `El paciente tiene alergias registradas (${alergiasLista.join(', ')}). La prescripción podría contener una sustancia relacionada. ¿Desea agregar el ítem de todos modos?`,
            type: 'danger',
            confirmLabel: 'Sí, agregar',
          })
          .then((ok) => {
            if (ok) this.ejecutarAgregarItemOrden();
          });
        return;
      }
    }
    this.ejecutarAgregarItemOrden();
  }

  private ejecutarAgregarItemOrden(): void {
    const valorNum = this.ordenForm.valorEstimado ? Number(this.ordenForm.valorEstimado) : undefined;
    this.ordenItems.push({
      tipo:             this.ordenForm.tipo,
      detalle:          (this.ordenForm.detalle?.trim() || this.ordenForm.plantilla?.trim()) || undefined,
      cantidadPrescrita: this.ordenForm.cantidadPrescrita ?? undefined,
      unidadMedida:     this.ordenForm.unidadMedida?.trim() || undefined,
      frecuencia:       this.ordenForm.frecuencia?.trim() || undefined,
      duracionDias:     this.ordenForm.duracionDias ?? undefined,
      valorEstimado:    valorNum,
    });
    this.ordenForm.plantilla = '';
    this.ordenForm.detalle = '';
    this.ordenForm.cantidadPrescrita = null;
    this.ordenForm.unidadMedida = '';
    this.ordenForm.frecuencia = '';
    this.ordenForm.duracionDias = null;
    this.ordenForm.valorEstimado = '';
    this.toast.success('Ítem agregado a la orden. Puedes añadir más o emitir.', 'Ítem agregado');
  }

  quitarOrdenItem(index: number): void {
    this.ordenItems.splice(index, 1);
  }

  crearOrden(): void {
    if (!this.selectedPatient()?.id || !this.ordenForm.consultaId) {
      this.toast.warning('Selecciona una consulta para asociar la orden.', 'Campo requerido');
      return;
    }
    const detalleActual = (this.ordenForm.detalle?.trim() || this.ordenForm.plantilla?.trim()) || '';
    const hayItemsEnLista = this.ordenItems.length > 0;
    const hayItemEnFormulario = !!detalleActual;

    if (!hayItemsEnLista && !hayItemEnFormulario) {
      this.toast.warning('Agrega al menos un ítem (detalle o plantilla) o usa "Agregar ítem" y luego "Emitir orden".', 'Sin ítems');
      return;
    }
    if (this.ordenForm.tipo === 'MEDICAMENTO' && this.historiaClinica()?.alergiasGenerales?.trim() && hayItemEnFormulario) {
      const textoOrden = `${this.ordenForm.detalle ?? ''} ${this.ordenForm.plantilla ?? ''}`.toLowerCase();
      const alergiasLista = this.alergiasTags(this.historiaClinica()!.alergiasGenerales!);
      const coincideAlergia = alergiasLista.some(
        (a) => textoOrden.includes(a.toLowerCase())
      );
      if (coincideAlergia) {
        this.confirmDialog
          .confirm({
            title: 'Alerta de alergia',
            message: `El paciente tiene alergias registradas (${alergiasLista.join(', ')}). La prescripción podría contener una sustancia relacionada. ¿Desea emitir la orden de todos modos?`,
            type: 'danger',
            confirmLabel: 'Sí, emitir orden',
          })
          .then((ok) => {
            if (ok) this.ejecutarCrearOrden();
          });
        return;
      }
    }
    this.ejecutarCrearOrden();
  }

  private ejecutarCrearOrden(): void {
    if (!this.selectedPatient()?.id || !this.ordenForm.consultaId) return;

    const detalleActual = (this.ordenForm.detalle?.trim() || this.ordenForm.plantilla?.trim()) || '';
    const itemsParaEnviar: OrdenClinicaItemDto[] = [...this.ordenItems];
    if (detalleActual) {
      itemsParaEnviar.push({
        tipo:             this.ordenForm.tipo,
        detalle:          detalleActual,
        cantidadPrescrita: this.ordenForm.cantidadPrescrita ?? undefined,
        unidadMedida:     this.ordenForm.unidadMedida?.trim() || undefined,
        frecuencia:       this.ordenForm.frecuencia?.trim() || undefined,
        duracionDias:     this.ordenForm.duracionDias ?? undefined,
        valorEstimado:    this.ordenForm.valorEstimado ? Number(this.ordenForm.valorEstimado) : undefined,
      });
    }

    if (itemsParaEnviar.length === 0) return;

    this.savingOrden.set(true);
    const pacienteId = this.selectedPatient()!.id;
    const consultaId = this.ordenForm.consultaId;

    if (itemsParaEnviar.length === 1) {
      this.ordenService.create({
        pacienteId,
        consultaId,
        tipo:             itemsParaEnviar[0].tipo,
        detalle:          itemsParaEnviar[0].detalle,
        cantidadPrescrita: itemsParaEnviar[0].cantidadPrescrita,
        unidadMedida:     itemsParaEnviar[0].unidadMedida,
        frecuencia:       itemsParaEnviar[0].frecuencia,
        duracionDias:     itemsParaEnviar[0].duracionDias,
        estado:           'PENDIENTE',
        valorEstimado:    itemsParaEnviar[0].valorEstimado,
      }).subscribe({
        next: (orden) => {
          this.savingOrden.set(false);
          this.toast.success('Orden clínica creada correctamente.', 'Orden creada');
          this.facturaForm.ordenId = orden.id;
          this.limpiarOrdenYItems();
          this.loadFlujoClinico(pacienteId);
        },
        error: (err) => {
          this.savingOrden.set(false);
          this.toast.error((err as { error?: { error?: string } })?.error?.error || 'No se pudo crear la orden.', 'Error');
        },
      });
    } else {
      this.ordenService.createBatch({
        pacienteId,
        consultaId,
        items: itemsParaEnviar,
      }).subscribe({
        next: (orden) => {
          this.savingOrden.set(false);
          this.toast.success(`Se creó una orden con ${itemsParaEnviar.length} ítem(s) correctamente.`, 'Orden creada');
          this.facturaForm.ordenId = orden.id;
          this.limpiarOrdenYItems();
          this.loadFlujoClinico(pacienteId);
        },
        error: (err) => {
          this.savingOrden.set(false);
          this.toast.error((err as { error?: { error?: string } })?.error?.error || 'No se pudo crear la orden.', 'Error');
        },
      });
    }
  }

  private limpiarOrdenYItems(): void {
    this.ordenItems.length = 0;
    this.ordenForm.plantilla = '';
    this.ordenForm.detalle = '';
    this.ordenForm.cantidadPrescrita = null;
    this.ordenForm.unidadMedida = '';
    this.ordenForm.frecuencia = '';
    this.ordenForm.duracionDias = null;
    this.ordenForm.valorEstimado = '';
  }

  /* ── Factura ─────────────────────────────────────────────────────── */
  crearFactura(): void {
    if (!this.selectedPatient()?.id || !this.facturaForm.valorTotal) {
      this.toast.warning('Ingresa el valor total para facturar.', 'Campo requerido');
      return;
    }
    this.savingFactura.set(true);
    this.facturaService.create({
      pacienteId:  this.selectedPatient()!.id,
      ordenId:     this.facturaForm.ordenId || undefined,
      valorTotal:  Number(this.facturaForm.valorTotal),
      estado:      'PENDIENTE',
      descripcion: this.facturaForm.descripcion.trim() || undefined,
    }).subscribe({
      next: () => {
        this.savingFactura.set(false);
        this.toast.success('Factura creada correctamente.', 'Factura creada');
        this.facturaForm.valorTotal = '';
        this.facturaForm.descripcion = '';
        this.loadFlujoClinico(this.selectedPatient()!.id);
      },
      error: (err) => {
        this.savingFactura.set(false);
        this.toast.error((err as { error?: { error?: string } })?.error?.error || 'No se pudo crear la factura.', 'Error');
      },
    });
  }

  /* ── Editar HC ───────────────────────────────────────────────────── */
  abrirEditarHC(): void {
    const hc = this.historiaClinica();
    if (hc) this._populateHcEditForm(hc);
    this.editandoHC.set(true);
  }

  cancelarEditarHC(): void { this.editandoHC.set(false); }

  guardarHC(): void {
    if (!this.historiaClinica()?.id) return;
    this.savingUpdateHC.set(true);
    this.historiaService.update(this.historiaClinica()!.id, {
      grupoSanguineo:             this.hcEditForm.grupoSanguineo  || undefined,
      alergiasGenerales:          this.hcEditForm.alergiasGenerales || undefined,
      antecedentesPersonales:     this.hcEditForm.antecedentesPersonales || undefined,
      antecedentesQuirurgicos:    this.hcEditForm.antecedentesQuirurgicos || undefined,
      antecedentesFarmacologicos: this.hcEditForm.antecedentesFarmacologicos || undefined,
      antecedentesTraumaticos:    this.hcEditForm.antecedentesTraumaticos || undefined,
      antecedentesGinecoobstetricos: this.hcEditForm.antecedentesGinecoobstetricos || undefined,
      antecedentesFamiliares:     this.hcEditForm.antecedentesFamiliares || undefined,
      habitosTabaco:    this.hcEditForm.habitosTabaco,
      habitosAlcohol:   this.hcEditForm.habitosAlcohol,
      habitosSustancias:this.hcEditForm.habitosSustancias,
      habitosDetalles:  this.hcEditForm.habitosDetalles || undefined,
    }).subscribe({
      next: (hc) => {
        this.historiaClinica.set(hc);
        this.savingUpdateHC.set(false);
        this.editandoHC.set(false);
        this.toast.success('Historia clínica actualizada.', 'HC actualizada');
      },
      error: (err) => {
        this.savingUpdateHC.set(false);
        this.toast.error((err as { error?: { error?: string } })?.error?.error || 'No se pudo actualizar la HC.', 'Error');
      },
    });
  }

  /* ── Cerrar HC ───────────────────────────────────────────────────── */
  async cerrarHC(): Promise<void> {
    if (!this.historiaClinica()?.id) return;
    const ok = await this.confirmDialog.confirm({
      title:        'Cerrar historia clínica',
      message:      '¿Estás seguro de cerrar esta historia clínica? Esta acción es irreversible.',
      type:         'danger',
      confirmLabel: 'Cerrar HC',
    });
    if (!ok) return;
    this.savingCerrarHC.set(true);
    this.historiaService.update(this.historiaClinica()!.id, { estado: 'CERRADA' }).subscribe({
      next: (hc) => {
        this.historiaClinica.set(hc);
        this.savingCerrarHC.set(false);
        this.toast.success('Historia clínica cerrada correctamente.', 'HC Cerrada');
      },
      error: (err) => {
        this.savingCerrarHC.set(false);
        this.toast.error((err as { error?: { error?: string } })?.error?.error || 'No se pudo cerrar la HC.', 'Error');
      },
    });
  }

  /* ── Navegación ──────────────────────────────────────────────────── */
  iniciarNuevaHistoria(): void {
    const p = this.selectedPatient();
    if (p) this.router.navigate(['/historia-clinica', p.id, 'nueva']);
  }

  imprimirExportar(): void { window.print(); }

  refreshResumen(): void {
    this.reporteService.resumen().subscribe({
      next: (res) => (this.resumen = res),
      error: () => (this.resumen = null),
    });
  }

  onCitaChange(citaId: number | null): void {
    const cita = this.citasPaciente.find((c) => c.id === citaId);
    if (cita) {
      this.soapS.profesionalId = cita.profesionalId ?? null;
    } else {
      this.soapS.profesionalId = this.authService.currentUser()?.personalId ?? null;
    }
  }

  /* ── Helpers de display ──────────────────────────────────────────── */
  calculateAge(fechaNacimiento: string): number {
    const hoy = new Date();
    const nac = new Date(fechaNacimiento);
    let edad  = hoy.getFullYear() - nac.getFullYear();
    if (hoy.getMonth() < nac.getMonth() ||
        (hoy.getMonth() === nac.getMonth() && hoy.getDate() < nac.getDate())) {
      edad--;
    }
    return edad;
  }

  formatFecha(fecha?: string): string {
    if (!fecha) return '—';
    const f = new Date(fecha);
    if (isNaN(f.getTime())) return '—';
    return f.toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  formatAnio(fecha?: string): string {
    if (!fecha) return new Date().getFullYear().toString();
    const f = new Date(fecha);
    if (isNaN(f.getTime())) return new Date().getFullYear().toString();
    return f.getFullYear().toString();
  }

  padId(id?: number): string {
    return String(id ?? 0).padStart(6, '0');
  }

  formatFechaLarga(fecha?: string): string {
    if (!fecha) return '—';
    const f = new Date(fecha);
    if (isNaN(f.getTime())) return '—';
    return f.toLocaleDateString('es-CO', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
  }

  formatFechaHora(fecha?: string): string {
    if (!fecha) return '—';
    const f = new Date(fecha);
    if (isNaN(f.getTime())) return '—';
    return f.toLocaleString('es-CO', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  }

  getServicioCita(citaId?: number): string {
    if (!citaId) return 'Consulta directa';
    const cita = this.citasPaciente.find((c) => c.id === citaId);
    return cita?.servicio || 'Consulta general';
  }

  estadoBadge(estado?: string): string {
    const e = (estado ?? '').toUpperCase();
    if (e === 'PENDIENTE')    return 'badge--warning';
    if (e === 'COMPLETADO' || e === 'COMPLETADA' || e === 'ACTIVA' || e === 'APROBADA' || e === 'PAGADA') return 'badge--success';
    if (e === 'CANCELADA' || e === 'CERRADA') return 'badge--danger';
    if (e === 'EN_PROCESO' || e === 'PARCIAL') return 'badge--info';
    return 'badge--secondary';
  }

  ordenCompletada(o: OrdenClinicaDto): boolean {
    const e = (o.estado ?? '').toUpperCase();
    return e === 'COMPLETADO' || e === 'COMPLETADA';
  }

  /** True solo si la orden admite resultado (laboratorio). No mostrar "Subir resultado" en medicamentos ni procedimientos. */
  ordenAceptaResultado(o: OrdenClinicaDto): boolean {
    if (o.tipo === 'LABORATORIO') return true;
    if (o.tipo === 'COMPUESTA' && o.items?.some((it) => it.tipo === 'LABORATORIO')) return true;
    return false;
  }

  openSubirResultado(orden: OrdenClinicaDto): void {
    this.ordenDetalleAbiertoId.set(orden.id);
    this.ordenResultadoEditId.set(orden.id);
    this.resultadoOrdenTexto = orden.resultado ?? '';
  }

  cancelarSubirResultado(): void {
    this.ordenResultadoEditId.set(null);
    this.resultadoOrdenTexto = '';
  }

  /** Alterna la vista detallada de la orden (ver ítems). */
  toggleOrdenDetalle(ordenId: number): void {
    this.ordenDetalleAbiertoId.set(this.ordenDetalleAbiertoId() === ordenId ? null : ordenId);
    this.ordenEditandoId.set(null);
  }

  /** Abre el modal con la información completa de la orden. */
  abrirModalOrden(orden: OrdenClinicaDto): void {
    this.ordenModalId.set(orden.id);
  }

  cerrarModalOrden(): void {
    this.ordenModalId.set(null);
  }

  /** Orden actual mostrada en el modal (null si el modal está cerrado). */
  ordenParaModal = computed(() => {
    const id = this.ordenModalId();
    if (id == null) return null;
    return this.ordenesFiltradas().find((o) => o.id === id) ?? null;
  });

  /** Órdenes de un solo ítem (no COMPUESTA) se pueden editar desde la lista. */
  ordenEsEditable(o: OrdenClinicaDto): boolean {
    return o.tipo !== 'COMPUESTA';
  }

  abrirEditarOrden(orden: OrdenClinicaDto): void {
    const items = this.getOrdenDisplayItems(orden);
    const first = items[0];
    if (!first) return;
    this.ordenEditandoId.set(orden.id);
    this.ordenEditandoForm = {
      tipo: first.tipo ?? orden.tipo,
      detalle: first.detalle ?? orden.detalle ?? '',
      cantidadPrescrita: first.cantidadPrescrita ?? orden.cantidadPrescrita ?? null,
      unidadMedida: first.unidadMedida ?? orden.unidadMedida ?? '',
      frecuencia: first.frecuencia ?? orden.frecuencia ?? '',
      duracionDias: first.duracionDias ?? orden.duracionDias ?? null,
      valorEstimado: first.valorEstimado ?? (orden.valorEstimado != null ? Number(orden.valorEstimado) : null),
    };
  }

  cancelarEditarOrden(): void {
    this.ordenEditandoId.set(null);
  }

  guardarEditarOrden(): void {
    const id = this.ordenEditandoId();
    const pacienteId = this.selectedPatient()?.id;
    if (id == null || pacienteId == null) return;
    const orden = this.ordenesPaciente.find((o) => o.id === id);
    if (!orden?.consultaId) {
      this.toast.warning('No se puede editar la orden.', 'Error');
      return;
    }
    this.savingOrdenEdit.set(true);
    this.ordenService.update(id, {
      pacienteId,
      consultaId: orden.consultaId,
      tipo: this.ordenEditandoForm.tipo,
      detalle: this.ordenEditandoForm.detalle || undefined,
      cantidadPrescrita: this.ordenEditandoForm.cantidadPrescrita ?? undefined,
      unidadMedida: this.ordenEditandoForm.unidadMedida || undefined,
      frecuencia: this.ordenEditandoForm.frecuencia || undefined,
      duracionDias: this.ordenEditandoForm.duracionDias ?? undefined,
      valorEstimado: this.ordenEditandoForm.valorEstimado ?? undefined,
    }).subscribe({
      next: (updated) => {
        this.savingOrdenEdit.set(false);
        this.ordenEditandoId.set(null);
        const idx = this.ordenesPaciente.findIndex((o) => o.id === id);
        if (idx >= 0) this.ordenesPaciente[idx] = updated;
        this._applyOrdenFilter();
        this.toast.success('Orden actualizada.', 'Guardado');
      },
      error: (err) => {
        this.savingOrdenEdit.set(false);
        this.toast.error((err as { error?: { error?: string } })?.error?.error ?? 'No se pudo actualizar la orden.', 'Error');
      },
    });
  }

  guardarResultadoOrden(): void {
    const id = this.ordenResultadoEditId();
    if (id == null || !this.resultadoOrdenTexto.trim()) {
      this.toast.warning('Escribe el resultado de la orden.', 'Campo requerido');
      return;
    }
    this.savingResultadoOrden.set(true);
    this.ordenService.registrarResultado(id, this.resultadoOrdenTexto.trim()).subscribe({
      next: () => {
        this.savingResultadoOrden.set(false);
        this.ordenResultadoEditId.set(null);
        this.resultadoOrdenTexto = '';
        this.toast.success('Resultado registrado. Orden marcada como completada.', 'Orden actualizada');
        const pid = this.selectedPatient()?.id;
        if (pid) this.loadFlujoClinico(pid);
      },
      error: (err) => {
        this.savingResultadoOrden.set(false);
        this.toast.error((err as { error?: { message?: string } })?.error?.message ?? 'No se pudo guardar el resultado.', 'Error');
      },
    });
  }

  descargarPdfOrdenes(): void {
    const paciente = this.selectedPatient();
    if (!paciente?.id) {
      this.toast.error('No hay un paciente seleccionado.', 'Sin paciente');
      return;
    }
    this.descargandoPdfOrdenes.set(true);
    this.withBranding((branding) => {
      try {
        const blob = this.sesaJspdf.generarOrdenesPacientePdf(this.toSesaPaciente(paciente), this.ordenesPaciente, branding);
        const nombre = (paciente.nombres ?? 'paciente').replace(/\s+/g, '-');
        this.sesaJspdf.triggerDownload(blob, `ordenes-resultados-${nombre}.pdf`);
        this.toast.success('PDF de órdenes y resultados descargado.', 'PDF generado');
      } catch (e) {
        this.toast.error('No se pudo generar el PDF.', 'Error PDF');
      }
      this.descargandoPdfOrdenes.set(false);
    });
  }

  imprimirPdfOrdenes(): void {
    const paciente = this.selectedPatient();
    if (!paciente?.id) {
      this.toast.error('No hay un paciente seleccionado.', 'Sin paciente');
      return;
    }
    if (this.ordenesPaciente.length === 0) {
      this.toast.warning('No hay órdenes para imprimir.', 'Órdenes');
      return;
    }
    this.imprimiendoPdfOrdenes.set(true);
    this.withBranding((branding) => {
      try {
        const blob = this.sesaJspdf.generarOrdenesPacientePdf(this.toSesaPaciente(paciente), this.ordenesPaciente, branding);
        this.sesaJspdf.openForPrint(blob);
        this.toast.success('Use el diálogo de impresión para imprimir órdenes y resultados.', 'Imprimir');
      } catch (e) {
        this.toast.error('No se pudo generar el documento para imprimir.', 'Error');
      }
      this.imprimiendoPdfOrdenes.set(false);
    });
  }

  /** Resumen de alta / referencia desde la última consulta (Res. 1995/1999). */
  descargarResumenAltaConsulta(): void {
    const paciente = this.selectedPatient();
    const ultima = this.consultasOrdenadas[0];
    if (!paciente?.id) {
      this.toast.error('No hay paciente seleccionado.', 'Error');
      return;
    }
    if (!ultima) {
      this.toast.warning('No hay consultas registradas para generar el resumen de alta.', 'Resumen de alta');
      return;
    }
    const blob = this.sesaJspdf.generarResumenAltaConsultaPdf({
      pacienteNombre: `${paciente.nombres ?? ''} ${paciente.apellidos ?? ''}`.trim(),
      pacienteDocumento: paciente.tipoDocumento && paciente.documento ? `${paciente.tipoDocumento} ${paciente.documento}` : (paciente.documento ?? ''),
      fechaConsulta: this.formatFechaLarga(ultima.fechaConsulta),
      profesionalNombre: ultima.profesionalNombre,
      motivoConsulta: ultima.motivoConsulta ?? undefined,
      diagnostico: ultima.diagnostico ?? undefined,
      codigoCie10: ultima.codigoCie10 ?? undefined,
      planTratamiento: ultima.planTratamiento ?? undefined,
      tratamientoFarmacologico: ultima.tratamientoFarmacologico ?? undefined,
      recomendaciones: ultima.recomendaciones ?? undefined,
    });
    const nombre = (paciente.nombres ?? 'paciente').replace(/\s+/g, '-');
    this.sesaJspdf.triggerDownload(blob, `resumen-alta-consulta-${nombre}.pdf`);
    this.toast.success('Resumen de alta / referencia descargado.', 'PDF generado');
  }

  descargarPdfOrdenIndividual(o: OrdenClinicaDto): void {
    const paciente = this.selectedPatient();
    if (!paciente) {
      this.toast.error('No hay paciente seleccionado.', 'Error');
      return;
    }
    this.ordenPdfIndividualId.set(o.id);
    this.withBranding((branding) => {
      try {
        const blob = this.sesaJspdf.generarOrdenIndividualPdf(this.toSesaPaciente(paciente), o, branding);
        const nombre = (o.detalle ?? 'orden').replace(/\s+/g, '-').slice(0, 40);
        this.sesaJspdf.triggerDownload(blob, `orden-${o.id}-${nombre}.pdf`);
        this.toast.success('PDF de la orden descargado.', 'PDF');
      } catch (e) {
        this.toast.error('No se pudo generar el PDF de la orden.', 'Error');
      }
      this.ordenPdfIndividualId.set(null);
    });
  }

  imprimirPdfOrdenIndividual(o: OrdenClinicaDto): void {
    const paciente = this.selectedPatient();
    if (!paciente) {
      this.toast.error('No hay paciente seleccionado.', 'Error');
      return;
    }
    this.ordenPdfIndividualId.set(o.id);
    this.withBranding((branding) => {
      try {
        const blob = this.sesaJspdf.generarOrdenIndividualPdf(this.toSesaPaciente(paciente), o, branding);
        this.sesaJspdf.openForPrint(blob);
        this.toast.success('Use el diálogo de impresión para esta orden.', 'Imprimir');
      } catch (e) {
        this.toast.error('No se pudo generar el documento para imprimir.', 'Error');
      }
      this.ordenPdfIndividualId.set(null);
    });
  }

  /** Obtiene branding (logo + datos empresa: nombre, NIT, dirección) y ejecuta el callback; si falla la carga del logo o empresa, pasa branding con lo disponible. */
  private withBranding(fn: (branding: SesaPdfBranding) => void): void {
    forkJoin({
      logoDataUrl: this.empresaCurrent.getLogoDataUrl(),
      empresa: this.empresaService.getCurrent(),
    }).subscribe({
      next: ({ logoDataUrl, empresa }) => {
        const branding: SesaPdfBranding = {
          empresaNombre: empresa?.razonSocial?.trim() || this.empresaCurrent.displayName(),
          empresaIdentificacion: empresa?.identificacion?.trim(),
          empresaDireccion: empresa?.direccionEmpresa?.trim(),
          logoDataUrl: logoDataUrl ?? undefined,
        };
        fn(branding);
      },
      error: () => {
        fn({
          empresaNombre: this.empresaCurrent.displayName(),
          empresaIdentificacion: undefined,
          empresaDireccion: undefined,
        });
      },
    });
  }

  private toSesaPaciente(p: PacienteDto): SesaPdfPaciente {
    return {
      id: p.id,
      nombres: p.nombres,
      apellidos: p.apellidos,
      documento: p.documento,
      tipoDocumento: p.tipoDocumento,
      fechaNacimiento: p.fechaNacimiento,
      sexo: p.sexo,
      telefono: p.telefono,
      email: p.email,
      direccion: p.direccion,
      grupoSanguineo: p.grupoSanguineo,
      epsNombre: p.epsNombre,
      municipioResidencia: p.municipioResidencia,
      departamentoResidencia: p.departamentoResidencia,
      zonaResidencia: p.zonaResidencia,
      regimenAfiliacion: p.regimenAfiliacion,
      tipoUsuario: p.tipoUsuario,
      contactoEmergenciaNombre: p.contactoEmergenciaNombre,
      contactoEmergenciaTelefono: p.contactoEmergenciaTelefono,
      estadoCivil: p.estadoCivil,
      escolaridad: p.escolaridad,
      ocupacion: p.ocupacion,
      pertenenciaEtnica: p.pertenenciaEtnica,
    };
  }

  tipoOrdenIcon(tipo: string): string {
    const map: Record<string, string> = {
      LABORATORIO: '🧪',
      MEDICAMENTO: '💊',
      PROCEDIMIENTO: '🩺',
      IMAGEN: '🔬',
    };
    return map[tipo] ?? '📋';
  }

  alergiasTags(texto?: string): string[] {
    if (!texto?.trim()) return [];
    return texto.split(/[,;\n]+/).map(t => t.trim()).filter(Boolean);
  }

  /** Expuesto para la plantilla (skeleton de carga). */
  protected readonly skeletonRows: number[] = [1, 2, 3, 4];

  get consultasOrdenadas(): ConsultaDto[] {
    return [...this.consultasPaciente].sort((a, b) =>
      (b.fechaConsulta ?? '').localeCompare(a.fechaConsulta ?? '')
    );
  }

  /** Timeline unificado: consultas + evoluciones (urgencias) ordenados por fecha descendente. */
  get timelineItems(): TimelineItem[] {
    const consultas: TimelineItem[] = this.consultasOrdenadas.map((c) => ({ tipo: 'consulta', data: c }));
    const evoluciones: TimelineItem[] = [...this.evolucionesPaciente]
      .sort((a, b) => (b.fecha ?? '').localeCompare(a.fecha ?? ''))
      .map((e) => ({ tipo: 'evolucion', data: e }));
    return [...consultas, ...evoluciones].sort((a, b) => {
      const dateA = a.tipo === 'consulta' ? (a.data as ConsultaDto).fechaConsulta : (a.data as EvolucionDto).fecha;
      const dateB = b.tipo === 'consulta' ? (b.data as ConsultaDto).fechaConsulta : (b.data as EvolucionDto).fecha;
      return (dateB ?? '').localeCompare(dateA ?? '');
    });
  }

  /** Timeline filtrado por tipo de atención: TODAS = todo; URGENCIA = consultas urgencia + evoluciones; resto = solo consultas. */
  get timelineItemsFiltrados(): TimelineItem[] {
    const tipo = this.filterTipoAtencion();
    if (!tipo || tipo === 'TODAS') return this.timelineItems;
    if (tipo === 'URGENCIA') {
      return this.timelineItems.filter(
        (it) => it.tipo === 'evolucion' || (it.tipo === 'consulta' && (it.data as ConsultaDto).tipoConsulta?.toUpperCase() === 'URGENCIA')
      );
    }
    return this.timelineItems.filter(
      (it) => it.tipo === 'consulta' && (it.data as ConsultaDto).tipoConsulta?.toUpperCase() === tipo
    );
  }

  /** Consultas filtradas por tipo de atención (vista unificada). */
  get consultasFiltradasPorTipo(): ConsultaDto[] {
    const tipo = this.filterTipoAtencion();
    if (!tipo || tipo === 'TODAS') return this.consultasOrdenadas;
    return this.consultasOrdenadas.filter((c) => (c.tipoConsulta ?? '').toUpperCase() === tipo);
  }

  /** Primera atención registrada hoy (misma fecha local). */
  get consultaHoy(): ConsultaDto | null {
    const hoy = new Date().toISOString().slice(0, 10);
    return this.consultasOrdenadas.find((c) => (c.fechaConsulta ?? '').slice(0, 10) === hoy) ?? null;
  }

  /** Resumen por tipo para vista unificada: "3 consultas, 1 urgencia, 2 evoluciones". */
  get resumenTipoAtencion(): string {
    const counts: Record<string, number> = {};
    for (const c of this.consultasPaciente) {
      const t = (c.tipoConsulta ?? 'OTRA').toUpperCase();
      counts[t] = (counts[t] ?? 0) + 1;
    }
    const partes: string[] = [];
    if (counts['PRIMERA_VEZ']) partes.push(`${counts['PRIMERA_VEZ']} primera vez`);
    if (counts['CONTROL']) partes.push(`${counts['CONTROL']} control`);
    if (counts['URGENCIA']) partes.push(`${counts['URGENCIA']} urgencia (consulta)`);
    if (this.evolucionesPaciente.length) partes.push(`${this.evolucionesPaciente.length} evolución(es) urgencia`);
    if (counts['TELECONSULTA']) partes.push(`${counts['TELECONSULTA']} teleconsulta`);
    const otras = (counts['OTRA'] ?? 0) + (counts[''] ?? 0);
    if (otras) partes.push(`${otras} otras`);
    return partes.length ? partes.join(', ') : 'Sin atenciones';
  }

  tipoConsultaLabel(tipo?: string): string {
    if (!tipo?.trim()) return 'Consulta';
    const t = tipo.toUpperCase();
    const found = this.TIPOS_ATENCION.find((x) => x.value === t);
    return found ? found.label : tipo;
  }

  /** Navega al tab SOAP y opcionalmente abre una sección (flujo rápido). */
  setTabAndSoapSection(tab: HistoriaTab, section?: 'S' | 'O' | 'A' | 'P'): void {
    this.setTab(tab);
    if (tab === 'soap' && section) this.soapSectionOpen.set(section);
  }

  /** Órdenes sin resultado registrado (continuidad — Res. 1995/1999). */
  get ordenesPendientesResultadoCount(): number {
    return this.ordenesPaciente.filter((o) => !(o.resultado?.trim())).length;
  }

  /** Medicación reciente según última consulta (para verificar interacciones). */
  get medicacionRecientePaciente(): string {
    const c = this.consultasOrdenadas[0];
    return c?.tratamientoFarmacologico?.trim() || '';
  }

  /** Descarga el PDF premium de la historia clínica del paciente actual (jsPDF). */
  descargarPdf(): void {
    const paciente = this.selectedPatient();
    if (!paciente?.id) {
      this.toast.error('No hay un paciente seleccionado.', 'Sin paciente');
      return;
    }
    this.descargandoPdf.set(true);
    this.withBranding((branding) => {
      try {
        const ultimaConsulta = this.consultasOrdenadas[0] ?? null;
        const historia = this.historiaClinica();
        const user = this.authService.currentUser();
        const brandingHC: SesaPdfBranding = {
          ...branding,
          printedBy: user?.nombreCompleto,
          profesionalNombre: ultimaConsulta?.profesionalNombre,
          profesionalRol: (ultimaConsulta as { profesionalRol?: string })?.profesionalRol,
          profesionalTarjeta: ultimaConsulta?.profesionalTarjetaProfesional,
        };
        const blob = this.sesaJspdf.generarHistoriaClinicaPdf(
          this.toSesaPaciente(paciente),
          historia ? this.toSesaHistoria(historia) : null,
          ultimaConsulta,
          this.ordenesPaciente,
          brandingHC
        );
        const nombre = (paciente.nombres ?? 'paciente').replace(/\s+/g, '-');
        this.sesaJspdf.triggerDownload(blob, `historia-clinica-${nombre}.pdf`);
        this.toast.success('Historia clínica descargada en PDF.', 'PDF generado');
      } catch (e) {
        this.toast.error('No se pudo generar el PDF.', 'Error PDF');
      }
      this.descargandoPdf.set(false);
    });
  }

  /** Abre el PDF de Historia Clínica en otra ventana para imprimir. */
  imprimirPdf(): void {
    const paciente = this.selectedPatient();
    if (!paciente?.id) {
      this.toast.error('No hay un paciente seleccionado.', 'Sin paciente');
      return;
    }
    this.imprimiendoPdf.set(true);
    this.withBranding((branding) => {
      try {
        const ultimaConsulta = this.consultasOrdenadas[0] ?? null;
        const historia = this.historiaClinica();
        const user = this.authService.currentUser();
        const brandingHC: SesaPdfBranding = {
          ...branding,
          printedBy: user?.nombreCompleto,
          profesionalNombre: ultimaConsulta?.profesionalNombre,
          profesionalRol: (ultimaConsulta as { profesionalRol?: string })?.profesionalRol,
          profesionalTarjeta: ultimaConsulta?.profesionalTarjetaProfesional,
        };
        const blob = this.sesaJspdf.generarHistoriaClinicaPdf(
          this.toSesaPaciente(paciente),
          historia ? this.toSesaHistoria(historia) : null,
          ultimaConsulta,
          this.ordenesPaciente,
          brandingHC
        );
        this.sesaJspdf.openForPrint(blob);
        this.toast.success('Use el diálogo de impresión en la ventana del PDF.', 'Imprimir');
      } catch (e) {
        this.toast.error('No se pudo generar el PDF para imprimir.', 'Error PDF');
      }
      this.imprimiendoPdf.set(false);
    });
  }

  /** Descarga el PDF de Evolución Clínica (solo consultas/notas de evolución). */
  descargarPdfEvolucion(): void {
    const paciente = this.selectedPatient();
    if (!paciente?.id) {
      this.toast.error('No hay un paciente seleccionado.', 'Sin paciente');
      return;
    }
    this.descargandoPdfEvolucion.set(true);
    this.withBranding((branding) => {
      try {
        const blob = this.sesaJspdf.generarEvolucionClinicaPdf(
          this.toSesaPaciente(paciente),
          this.consultasOrdenadas,
          { ...branding, printedBy: this.authService.currentUser()?.nombreCompleto }
        );
        const nombre = (paciente.nombres ?? 'paciente').replace(/\s+/g, '-');
        this.sesaJspdf.triggerDownload(blob, `evolucion-clinica-${nombre}.pdf`);
        this.toast.success('Evolución clínica descargada en PDF.', 'PDF generado');
      } catch (e) {
        this.toast.error('No se pudo generar el PDF de evolución.', 'Error PDF');
      }
      this.descargandoPdfEvolucion.set(false);
    });
  }

  /** Abre el PDF de Evolución Clínica en ventana de impresión. */
  imprimirPdfEvolucion(): void {
    const paciente = this.selectedPatient();
    if (!paciente?.id) {
      this.toast.error('No hay un paciente seleccionado.', 'Sin paciente');
      return;
    }
    this.imprimiendoPdfEvolucion.set(true);
    this.withBranding((branding) => {
      try {
        const blob = this.sesaJspdf.generarEvolucionClinicaPdf(
          this.toSesaPaciente(paciente),
          this.consultasOrdenadas,
          { ...branding, printedBy: this.authService.currentUser()?.nombreCompleto }
        );
        this.sesaJspdf.openForPrint(blob);
        this.toast.success('Use el diálogo de impresión para la evolución clínica.', 'Imprimir');
      } catch (e) {
        this.toast.error('No se pudo generar el documento para imprimir.', 'Error');
      }
      this.imprimiendoPdfEvolucion.set(false);
    });
  }

  private toSesaHistoria(h: HistoriaClinicaDto): SesaPdfHistoriaClinica {
    return {
      id: h.id,
      pacienteNombre: h.pacienteNombre,
      pacienteDocumento: h.pacienteDocumento,
      fechaApertura: h.fechaApertura,
      estado: h.estado,
      grupoSanguineo: h.grupoSanguineo,
      alergiasGenerales: h.alergiasGenerales,
      antecedentesPersonales: h.antecedentesPersonales,
      antecedentesFamiliares: h.antecedentesFamiliares,
      antecedentesQuirurgicos: h.antecedentesQuirurgicos,
      antecedentesFarmacologicos: h.antecedentesFarmacologicos,
      antecedentesTraumaticos: h.antecedentesTraumaticos,
      antecedentesGinecoobstetricos: h.antecedentesGinecoobstetricos,
      habitosTabaco: h.habitosTabaco,
      habitosAlcohol: h.habitosAlcohol,
      habitosSustancias: h.habitosSustancias,
      habitosDetalles: h.habitosDetalles,
    };
  }
}
