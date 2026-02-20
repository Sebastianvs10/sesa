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
import { AuthService } from '../../core/services/auth.service';
import { CitaDto, CitaService } from '../../core/services/cita.service';
import { ConsultaDto, ConsultaService } from '../../core/services/consulta.service';
import { OrdenClinicaDto, OrdenClinicaService } from '../../core/services/orden-clinica.service';
import { FacturaDto, FacturaService } from '../../core/services/factura.service';
import { ReporteResumenDto, ReporteService } from '../../core/services/reporte.service';
import { PersonalDto, PersonalService } from '../../core/services/personal.service';
import { DoloresPanelComponent } from './dolores-panel/dolores-panel.component';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';
import { SesaConfirmDialogService } from '../../shared/components/sesa-confirm-dialog/sesa-confirm-dialog.component';
import { SesaSkeletonComponent } from '../../shared/components/sesa-skeleton/sesa-skeleton.component';
import { SesaEmptyStateComponent } from '../../shared/components/sesa-empty-state/sesa-empty-state.component';

export type HistoriaTab = 'historia' | 'soap' | 'ordenes' | 'documentos' | 'dolores';
export type TipoOrden = 'LABORATORIO' | 'MEDICAMENTO' | 'PROCEDIMIENTO' | 'IMAGEN';

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
  ],
  templateUrl: './historia-clinica.page.html',
  styleUrl: './historia-clinica.page.scss',
})
export class HistoriaClinicaPageComponent implements OnInit {
  private readonly route          = inject(ActivatedRoute);
  private readonly router         = inject(Router);
  private readonly pacienteService   = inject(PacienteService);
  private readonly historiaService   = inject(HistoriaClinicaService);
  private readonly authService       = inject(AuthService);
  private readonly citaService       = inject(CitaService);
  private readonly consultaService   = inject(ConsultaService);
  private readonly ordenService      = inject(OrdenClinicaService);
  private readonly facturaService    = inject(FacturaService);
  private readonly reporteService    = inject(ReporteService);
  private readonly personalService   = inject(PersonalService);
  private readonly toast             = inject(SesaToastService);
  private readonly confirmDialog     = inject(SesaConfirmDialogService);

  /* ── Estado global ─────────────────────────────────────────────────── */
  loadingPatient  = signal(false);
  loadingFlujo    = signal(false);
  loadingMisConsultas = signal(false);

  selectedPatient    = signal<PacienteDto | null>(null);
  historiaClinica    = signal<HistoriaClinicaDto | null>(null);
  profesionales: PersonalDto[] = [];
  citasPaciente: CitaDto[]       = [];
  consultasPaciente: ConsultaDto[] = [];
  misConsultas: ConsultaDto[]    = [];
  ordenesPaciente: OrdenClinicaDto[] = [];
  ordenesFiltradas   = signal<OrdenClinicaDto[]>([]);
  facturasPaciente: FacturaDto[] = [];
  resumen: ReporteResumenDto | null = null;

  error: string | null = null;

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

  /* Sección O — Objetivo (Signos Vitales — se combinan en el texto) */
  soapO = {
    presionArterial:       '',
    frecuenciaCardiaca:    '',
    frecuenciaRespiratoria:'',
    temperatura:           '',
    saturacionO2:          '',
    peso:                  '',
    talla:                 '',
    imc:                   '',
    hallazgosExamen:       '',
  };

  /* Sección A — Análisis */
  soapA = {
    diagnostico:   '',
    codigoCie10:   '',
    observaciones: '',
  };

  /* Sección P — Plan */
  soapP = {
    planTratamiento:        '',
    tratamientoFarmacologico: '',
    recomendaciones:        '',
  };

  soapSectionOpen = signal<'S' | 'O' | 'A' | 'P' | null>('S');

  /* ── Orden clínica ────────────────────────────────────────────────── */
  ordenForm = {
    consultaId:    null as number | null,
    tipo:          'LABORATORIO' as string,
    plantilla:     '',
    detalle:       '',
    valorEstimado: '',
  };

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

  totalConsultas = computed(() => this.consultasPaciente.length);
  totalOrdenes   = computed(() => this.ordenesPaciente.length);
  totalFacturas  = computed(() => this.facturasPaciente.length);

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

    this.route.queryParams.subscribe((params) => {
      const pacienteId = params['pacienteId'];
      if (pacienteId) {
        this.loadPaciente(parseInt(pacienteId, 10));
      } else {
        this.loadMisConsultas();
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
        this.historiaClinica.set(historia);
        if (historia) this._populateHcEditForm(historia);
        this.loadFlujoClinico(pacienteId);
      },
      error: () => {
        this.historiaClinica.set(null);
        this.loadFlujoClinico(pacienteId);
      },
    });
  }

  private loadFlujoClinico(pacienteId: number): void {
    this.loadingFlujo.set(true);
    this.citaService.list().subscribe({
      next: (citas) => {
        this.citasPaciente = (citas ?? []).filter((c) => c.pacienteId === pacienteId);
        this.consultaService.listByPaciente(pacienteId, 0, 100).subscribe({
          next: (consultas) => {
            this.consultasPaciente = Array.isArray(consultas) ? consultas : [];
            this.ordenService.listByPaciente(pacienteId).subscribe({
              next: (ordenes) => {
                this.ordenesPaciente = ordenes ?? [];
                this._applyOrdenFilter();
                this.facturaService.listByPaciente(pacienteId).subscribe({
                  next: (facturas) => {
                    this.facturasPaciente = facturas ?? [];
                    this._autoSeleccionarFlujo();
                    this.refreshResumen();
                    this.loadingFlujo.set(false);
                    this.loadingPatient.set(false);
                  },
                  error: (err) => this._setLoadError(err, 'Error al cargar facturas'),
                });
              },
              error: (err) => this._setLoadError(err, 'Error al cargar órdenes'),
            });
          },
          error: (err) => this._setLoadError(err, 'Error al cargar consultas'),
        });
      },
      error: (err) => this._setLoadError(err, 'Error al cargar citas'),
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
    if (this.consultasPaciente.length > 0 && !this.ordenForm.consultaId) {
      this.ordenForm.consultaId = this.consultasPaciente[0].id;
    }
    if (this.ordenesPaciente.length > 0 && !this.facturaForm.ordenId) {
      this.facturaForm.ordenId = this.ordenesPaciente[0].id;
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
      profesionalId:          this.soapS.profesionalId,
      citaId:                 this.soapS.citaId || undefined,
      motivoConsulta:         this.soapS.motivoConsulta.trim(),
      enfermedadActual:       enfermedadCompleta.trim() || undefined,
      antecedentesPersonales: this.soapS.antecedentesPersonales.trim() || undefined,
      antecedentesFamiliares: this.soapS.antecedentesFamiliares.trim() || undefined,
      alergias:               this.soapS.alergias.trim() || undefined,
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
      temperatura:'', saturacionO2:'', peso:'', talla:'', imc:'', hallazgosExamen:'',
    });
    Object.assign(this.soapA, { diagnostico:'', codigoCie10:'', observaciones:'' });
    Object.assign(this.soapP, { planTratamiento:'', tratamientoFarmacologico:'', recomendaciones:'' });
    this.soapSectionOpen.set('S');
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
      f ? this.ordenesPaciente.filter(o => o.tipo === f) : [...this.ordenesPaciente]
    );
  }

  onTipoOrdenChange(): void {
    this.ordenForm.plantilla = '';
    this.ordenForm.detalle = '';
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

  crearOrden(): void {
    if (!this.selectedPatient()?.id || !this.ordenForm.consultaId) {
      this.toast.warning('Selecciona una consulta para asociar la orden.', 'Campo requerido');
      return;
    }
    this.savingOrden.set(true);
    this.ordenService.create({
      pacienteId:     this.selectedPatient()!.id,
      consultaId:     this.ordenForm.consultaId,
      tipo:           this.ordenForm.tipo,
      detalle:        (this.ordenForm.detalle.trim() || this.ordenForm.plantilla.trim()) || undefined,
      estado:         'PENDIENTE',
      valorEstimado:  this.ordenForm.valorEstimado ? Number(this.ordenForm.valorEstimado) : undefined,
    }).subscribe({
      next: (orden) => {
        this.savingOrden.set(false);
        this.toast.success('Orden clínica creada correctamente.', 'Orden creada');
        this.facturaForm.ordenId = orden.id;
        this.ordenForm.plantilla = '';
        this.ordenForm.detalle = '';
        this.ordenForm.valorEstimado = '';
        this.loadFlujoClinico(this.selectedPatient()!.id);
      },
      error: (err) => {
        this.savingOrden.set(false);
        this.toast.error((err as { error?: { error?: string } })?.error?.error || 'No se pudo crear la orden.', 'Error');
      },
    });
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
    if (cita) this.soapS.profesionalId = cita.profesionalId ?? null;
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

  formatFechaLarga(fecha?: string): string {
    if (!fecha) return '—';
    const f = new Date(fecha);
    if (isNaN(f.getTime())) return '—';
    return f.toLocaleDateString('es-CO', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
  }

  getServicioCita(citaId?: number): string {
    if (!citaId) return 'Consulta directa';
    const cita = this.citasPaciente.find((c) => c.id === citaId);
    return cita?.servicio || 'Consulta general';
  }

  estadoBadge(estado?: string): string {
    const e = (estado ?? '').toUpperCase();
    if (e === 'PENDIENTE')    return 'badge--warning';
    if (e === 'ACTIVA' || e === 'APROBADA' || e === 'PAGADA') return 'badge--success';
    if (e === 'CANCELADA' || e === 'CERRADA') return 'badge--danger';
    if (e === 'EN_PROCESO' || e === 'PARCIAL') return 'badge--info';
    return 'badge--secondary';
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

  get skeletonRows(): number[] { return [1,2,3,4]; }

  get consultasOrdenadas(): ConsultaDto[] {
    return [...this.consultasPaciente].sort((a, b) =>
      (b.fechaConsulta ?? '').localeCompare(a.fechaConsulta ?? '')
    );
  }
}
