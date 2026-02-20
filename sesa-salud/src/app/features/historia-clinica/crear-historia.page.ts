/**
 * Crear Historia Clínica Premium — Stepper visual, signos vitales, SOAP completo.
 * Autor: Ing. J Sebastian Vargas S
 */
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { PacienteService, PacienteDto } from '../../core/services/paciente.service';
import { HistoriaClinicaService, CrearHistoriaCompletaRequestDto } from '../../core/services/historia-clinica.service';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';
import { SesaConfirmDialogService } from '../../shared/components/sesa-confirm-dialog/sesa-confirm-dialog.component';
import { SesaSkeletonComponent } from '../../shared/components/sesa-skeleton/sesa-skeleton.component';

export type CrearHCStep = 1 | 2 | 3 | 4 | 5 | 6;

export interface StepDef {
  id:    CrearHCStep;
  label: string;
  icon:  string;
  desc:  string;
}

@Component({
  standalone: true,
  selector: 'sesa-crear-historia-page',
  imports: [CommonModule, FormsModule, SesaSkeletonComponent],
  templateUrl: './crear-historia.page.html',
  styleUrl: './crear-historia.page.scss',
})
export class CrearHistoriaPageComponent implements OnInit {
  private readonly route           = inject(ActivatedRoute);
  private readonly router          = inject(Router);
  private readonly pacienteService = inject(PacienteService);
  private readonly historiaService = inject(HistoriaClinicaService);
  private readonly toast           = inject(SesaToastService);
  private readonly confirmDialog   = inject(SesaConfirmDialogService);

  /* ── Estado general ─────────────────────────────────────────────── */
  paciente         = signal<PacienteDto | null>(null);
  cargandoPaciente = signal(false);
  guardando        = signal(false);
  exito            = signal(false);
  errorGuardado    = signal<string | null>(null);

  /* ── Stepper ─────────────────────────────────────────────────────── */
  currentStep = signal<CrearHCStep>(1);

  readonly steps: StepDef[] = [
    { id: 1, label: 'Paciente',        icon: '👤', desc: 'Identificación del paciente' },
    { id: 2, label: 'Motivo',          icon: '📋', desc: 'Motivo y enfermedad actual' },
    { id: 3, label: 'Antecedentes',    icon: '🗂️', desc: 'Historia patológica previa' },
    { id: 4, label: 'Rev. Sistemas',   icon: '🔍', desc: 'Revisión por sistemas' },
    { id: 5, label: 'Examen Físico',   icon: '🩺', desc: 'Signos vitales y examen' },
    { id: 6, label: 'Diagnóstico',     icon: '📊', desc: 'Diagnóstico y plan de manejo' },
  ];

  stepProgress = computed(() => ((this.currentStep() - 1) / (this.steps.length - 1)) * 100);

  /* ════════════════════ DATOS DEL FORMULARIO ════════════════════════ */

  /* Paso 2 — Motivo y Enfermedad */
  motivoConsulta   = '';
  enfermedad       = '';
  versionEnfermedad= '';
  sintomasAsociados= '';
  factoresMejoran  = '';
  factoresEmpeoran = '';

  /* Paso 3 — Antecedentes */
  antecedentesPatologicos      = '';
  antecedentesQuirurgicos      = '';
  antecedentesFarmacologicos   = '';
  antecedentesAlergicos        = '';
  antecedentesTraumaticos      = '';
  antecedentesGinecoobstetricos= '';
  antecedentesFamiliares       = '';
  habitosTabaco    = false;
  habitosAlcohol   = false;
  habitosSustancias= false;
  habitosDetalles  = '';

  /* Paso 4 — Revisión por sistemas */
  sistemaRespiratoria  = '';
  sistemaCardiovascular= '';
  sistemaDigestivo     = '';
  sistemaUrinario      = '';
  sistemaNervioso      = '';
  sistemaEndocrino     = '';
  sistemaMusculos      = '';
  sistemaPsiquiatrico  = '';
  sistemaDermatologico = '';
  sistemaHematologico  = '';

  /* Paso 5 — Examen Físico */
  presionArterial       = '';
  frecuenciaCardiaca    = '';
  frecuenciaRespiratoria= '';
  temperatura           = '';
  saturacionO2          = '';
  peso                  = '';
  talla                 = '';
  imc                   = '';
  perimetroCintura      = '';
  evaluacionGeneral     = '';
  hallazgosCardiovascular = '';
  hallazgosPulmones     = '';
  hallazgosAbdomen      = '';
  hallazgosNeurologico  = '';
  hallazgosPiel         = '';

  /* Paso 6 — Diagnóstico y plan */
  diagnostico              = '';
  codigoCIE10              = '';
  diagnosticoSecundario    = '';
  planTratamiento          = '';
  tratamientoFarmacologico = '';
  ordenesMedicas           = '';
  examenesSolicitados      = '';
  incapacidad              = '';
  recomendaciones          = '';
  proximaCita              = '';

  /* ── Catálogos ──────────────────────────────────────────────────── */
  readonly gruposSanguineos = ['A+','A−','B+','B−','AB+','AB−','O+','O−'];

  /* ─────────────────────────────────────────────────────────────────── */
  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('pacienteId');
    if (!id) { this.router.navigate(['/historia-clinica/nueva']); return; }

    this.cargandoPaciente.set(true);
    this.pacienteService.get(parseInt(id, 10)).subscribe({
      next: (p) => { this.paciente.set(p); this.cargandoPaciente.set(false); },
      error: (err) => {
        this.cargandoPaciente.set(false);
        this.toast.error(err?.error?.error || 'Error al cargar paciente.', 'Error');
        this.router.navigate(['/historia-clinica/nueva']);
      },
    });
  }

  /* ── Navegación del stepper ─────────────────────────────────────── */
  goStep(step: CrearHCStep): void {
    if (!this._validateStep(this.currentStep())) return;
    this.currentStep.set(step);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  next(): void {
    if (!this._validateStep(this.currentStep())) return;
    if (this.currentStep() < 6) {
      this.currentStep.set((this.currentStep() + 1) as CrearHCStep);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  prev(): void {
    if (this.currentStep() > 1) {
      this.currentStep.set((this.currentStep() - 1) as CrearHCStep);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  private _validateStep(step: CrearHCStep): boolean {
    if (step === 2 && !this.motivoConsulta.trim()) {
      this.toast.warning('El motivo de consulta es obligatorio.', 'Campo requerido');
      return false;
    }
    return true;
  }

  /* ── IMC automático ─────────────────────────────────────────────── */
  calcularIMC(): void {
    const p = parseFloat(this.peso);
    const t = parseFloat(this.talla) / 100;
    if (p > 0 && t > 0) this.imc = (p / (t * t)).toFixed(1);
  }

  get imcCategoria(): { label: string; color: string } {
    const v = parseFloat(this.imc);
    if (isNaN(v)) return { label: '', color: '' };
    if (v < 18.5) return { label: 'Bajo peso',      color: '#0ea5e9' };
    if (v < 25)   return { label: 'Peso normal',    color: '#22c55e' };
    if (v < 30)   return { label: 'Sobrepeso',      color: '#f59e0b' };
    return              { label: 'Obesidad',        color: '#ef4444' };
  }

  /* ── Guardar ─────────────────────────────────────────────────────── */
  guardarHistoria(): void {
    if (!this.paciente()) return;
    if (!this.motivoConsulta.trim() || !this.enfermedad.trim()) {
      this.toast.warning('Completa el motivo de consulta y la descripción de la enfermedad actual.', 'Campos requeridos');
      this.currentStep.set(2);
      return;
    }

    const revisionSistemas = [
      this.sistemaRespiratoria   && `Respiratorio: ${this.sistemaRespiratoria}`,
      this.sistemaCardiovascular && `Cardiovascular: ${this.sistemaCardiovascular}`,
      this.sistemaDigestivo      && `Digestivo: ${this.sistemaDigestivo}`,
      this.sistemaUrinario       && `Urinario: ${this.sistemaUrinario}`,
      this.sistemaNervioso       && `Nervioso: ${this.sistemaNervioso}`,
      this.sistemaEndocrino      && `Endocrino: ${this.sistemaEndocrino}`,
      this.sistemaMusculos       && `Musculoesquelético: ${this.sistemaMusculos}`,
      this.sistemaPsiquiatrico   && `Psiquiátrico: ${this.sistemaPsiquiatrico}`,
      this.sistemaDermatologico  && `Dermatológico: ${this.sistemaDermatologico}`,
      this.sistemaHematologico   && `Hematológico: ${this.sistemaHematologico}`,
    ].filter(Boolean).join('\n');

    const hallazgos = [
      this.hallazgosCardiovascular && `Cardiovascular: ${this.hallazgosCardiovascular}`,
      this.hallazgosPulmones       && `Pulmones: ${this.hallazgosPulmones}`,
      this.hallazgosAbdomen        && `Abdomen: ${this.hallazgosAbdomen}`,
      this.hallazgosNeurologico    && `Neurológico: ${this.hallazgosNeurologico}`,
      this.hallazgosPiel           && `Piel: ${this.hallazgosPiel}`,
    ].filter(Boolean).join('\n');

    const dto: CrearHistoriaCompletaRequestDto = {
      grupoSanguineo:              this.paciente()!.grupoSanguineo || undefined,
      alergiasGenerales:           this.antecedentesAlergicos || undefined,
      antecedentesPersonales:      this.antecedentesPatologicos || undefined,
      antecedentesQuirurgicos:     this.antecedentesQuirurgicos || undefined,
      antecedentesFarmacologicos:  this.antecedentesFarmacologicos || undefined,
      antecedentesTraumaticos:     this.antecedentesTraumaticos || undefined,
      antecedentesGinecoobstetricos: this.antecedentesGinecoobstetricos || undefined,
      antecedentesFamiliares:      this.antecedentesFamiliares || undefined,
      habitosTabaco:               this.habitosTabaco || undefined,
      habitosAlcohol:              this.habitosAlcohol || undefined,
      habitosSustancias:           this.habitosSustancias || undefined,
      habitosDetalles:             this.habitosDetalles || undefined,
      motivoConsulta:              this.motivoConsulta,
      enfermedadActual:            this.enfermedad,
      versionEnfermedad:           this.versionEnfermedad || undefined,
      sintomasAsociados:           this.sintomasAsociados || undefined,
      factoresMejoran:             this.factoresMejoran || undefined,
      factoresEmpeoran:            this.factoresEmpeoran || undefined,
      revisionSistemas:            revisionSistemas || undefined,
      presionArterial:             this.presionArterial || undefined,
      frecuenciaCardiaca:          this.frecuenciaCardiaca || undefined,
      frecuenciaRespiratoria:      this.frecuenciaRespiratoria || undefined,
      temperatura:                 this.temperatura || undefined,
      peso:                        this.peso || undefined,
      talla:                       this.talla || undefined,
      imc:                         this.imc || undefined,
      evaluacionGeneral:           this.evaluacionGeneral || undefined,
      hallazgos:                   hallazgos || undefined,
      diagnostico:                 this.diagnostico || undefined,
      codigoCie10:                 this.codigoCIE10 || undefined,
      planTratamiento:             this.planTratamiento || undefined,
      tratamientoFarmacologico:    this.tratamientoFarmacologico || undefined,
      ordenesMedicas:              this.ordenesMedicas || undefined,
      examenesSolicitados:         this.examenesSolicitados || undefined,
      incapacidad:                 this.incapacidad || undefined,
      recomendaciones:             this.recomendaciones || undefined,
    };

    this.guardando.set(true);
    this.errorGuardado.set(null);
    this.historiaService.createCompleta(this.paciente()!.id, dto).subscribe({
      next: () => {
        this.guardando.set(false);
        this.exito.set(true);
        this.toast.success('Historia clínica creada exitosamente.', '¡Listo!');
        setTimeout(() => {
          this.router.navigate(['/historia-clinica'], {
            queryParams: { pacienteId: this.paciente()!.id },
          });
        }, 1800);
      },
      error: (err) => {
        this.guardando.set(false);
        const msg = err?.error?.error || 'Error al guardar la historia clínica.';
        this.errorGuardado.set(msg);
        this.toast.error(msg, 'Error al guardar');
      },
    });
  }

  async cancelar(): Promise<void> {
    const ok = await this.confirmDialog.confirm({
      title:        'Descartar historia clínica',
      message:      '¿Deseas salir? Se perderán todos los datos ingresados.',
      type:         'warning',
      confirmLabel: 'Sí, salir',
      cancelLabel:  'Continuar editando',
    });
    if (ok) this.router.navigate(['/historia-clinica/nueva']);
  }

  /* ── Binding dinámico para revisión por sistemas ───────────────── */
  getSistema(key: string): string {
    return (this as unknown as Record<string, string>)[key] ?? '';
  }

  setSistema(key: string, value: string): void {
    (this as unknown as Record<string, string>)[key] = value;
  }

  /* ── Helpers ─────────────────────────────────────────────────────── */
  calculateAge(fechaNacimiento: string): number {
    const hoy = new Date();
    const nac = new Date(fechaNacimiento);
    let edad  = hoy.getFullYear() - nac.getFullYear();
    if (hoy.getMonth() < nac.getMonth() ||
        (hoy.getMonth() === nac.getMonth() && hoy.getDate() < nac.getDate())) edad--;
    return edad;
  }

  isStepDone(step: CrearHCStep): boolean {
    return this.currentStep() > step;
  }

  get completedPercent(): number {
    let filled = 0; let total = 4;
    if (this.motivoConsulta.trim()) filled++;
    if (this.enfermedad.trim())     filled++;
    if (this.diagnostico.trim())    filled++;
    if (this.presionArterial.trim() || this.peso.trim()) filled++;
    return Math.round((filled / total) * 100);
  }
}
