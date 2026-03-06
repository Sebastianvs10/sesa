/**
 * Formulario de Paciente — wizard por pasos, skeleton, toast CRUD. Estilo SaaS premium.
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { PacienteService, PacienteRequestDto } from '../../core/services/paciente.service';
import { EpsService, EpsDto } from '../../core/services/eps.service';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { SesaFormFieldComponent } from '../../shared/components/sesa-form-field/sesa-form-field.component';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';

const STEPS = [
  { id: 1, label: 'Identificación', short: 'Datos' },
  { id: 2, label: 'Afiliación', short: 'EPS' },
  { id: 3, label: 'Residencia', short: 'Ubicación' },
  { id: 4, label: 'Contacto', short: 'Contacto' },
  { id: 5, label: 'Sociodemográficos', short: 'Más datos' },
] as const;

@Component({
  standalone: true,
  selector: 'sesa-paciente-form-page',
  imports: [CommonModule, ReactiveFormsModule, RouterLink, SesaCardComponent, SesaFormFieldComponent],
  templateUrl: './paciente-form.page.html',
  styleUrl: './paciente-form.page.scss',
})
export class PacienteFormPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly pacienteService = inject(PacienteService);
  private readonly epsService = inject(EpsService);
  private readonly toast = inject(SesaToastService);

  readonly steps = STEPS;
  readonly totalSteps = STEPS.length;
  currentStep = signal(1);
  form!: FormGroup;
  epsList: EpsDto[] = [];
  isEdit = false;
  id: number | null = null;
  loadingPatient = signal(false);
  saving = signal(false);
  error: string | null = null;

  isFirstStep = computed(() => this.currentStep() === 1);
  isLastStep = computed(() => this.currentStep() === this.totalSteps);
  progressPercent = computed(() => (this.currentStep() / this.totalSteps) * 100);

  // Datos de municipios/departamentos (DIVIPOLA simplificado - principales)
  readonly departamentos = [
    { codigo: '05', nombre: 'Antioquia' }, { codigo: '08', nombre: 'Atlántico' },
    { codigo: '11', nombre: 'Bogotá D.C.' }, { codigo: '13', nombre: 'Bolívar' },
    { codigo: '15', nombre: 'Boyacá' }, { codigo: '17', nombre: 'Caldas' },
    { codigo: '18', nombre: 'Caquetá' }, { codigo: '19', nombre: 'Cauca' },
    { codigo: '20', nombre: 'Cesar' }, { codigo: '23', nombre: 'Córdoba' },
    { codigo: '25', nombre: 'Cundinamarca' }, { codigo: '27', nombre: 'Chocó' },
    { codigo: '41', nombre: 'Huila' }, { codigo: '44', nombre: 'La Guajira' },
    { codigo: '47', nombre: 'Magdalena' }, { codigo: '50', nombre: 'Meta' },
    { codigo: '52', nombre: 'Nariño' }, { codigo: '54', nombre: 'Norte de Santander' },
    { codigo: '63', nombre: 'Quindío' }, { codigo: '66', nombre: 'Risaralda' },
    { codigo: '68', nombre: 'Santander' }, { codigo: '70', nombre: 'Sucre' },
    { codigo: '73', nombre: 'Tolima' }, { codigo: '76', nombre: 'Valle del Cauca' },
    { codigo: '81', nombre: 'Arauca' }, { codigo: '85', nombre: 'Casanare' },
    { codigo: '86', nombre: 'Putumayo' }, { codigo: '88', nombre: 'San Andrés y Providencia' },
    { codigo: '91', nombre: 'Amazonas' }, { codigo: '94', nombre: 'Guainía' },
    { codigo: '95', nombre: 'Guaviare' }, { codigo: '97', nombre: 'Vaupés' },
    { codigo: '99', nombre: 'Vichada' },
  ];

  ngOnInit(): void {
    this.form = this.fb.group({
      tipoDocumento: ['CC'],
      documento: ['', [Validators.required]],
      nombres: ['', [Validators.required]],
      apellidos: [''],
      fechaNacimiento: [''],
      sexo: [''],
      grupoSanguineo: [''],
      telefono: [''],
      email: [''],
      direccion: [''],
      epsId: [null],
      activo: [true],
      // Campos normativos RIPS
      departamentoResidencia: [''],
      municipioResidencia: [''],
      zonaResidencia: ['URBANA'],
      regimenAfiliacion: [''],
      tipoUsuario: [''],
      contactoEmergenciaNombre: [''],
      contactoEmergenciaTelefono: [''],
      estadoCivil: [''],
      escolaridad: [''],
      ocupacion: [''],
      pertenenciaEtnica: [''],
    });

    this.epsService.list().subscribe({ next: (list) => (this.epsList = list) });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam && idParam !== 'nuevo') {
      this.isEdit = true;
      this.id = +idParam;
      this.loadingPatient.set(true);
      this.pacienteService.get(this.id).subscribe({
        next: (p) => {
          this.form.patchValue({
            tipoDocumento: p.tipoDocumento ?? 'CC',
            documento: p.documento,
            nombres: p.nombres,
            apellidos: p.apellidos ?? '',
            fechaNacimiento: p.fechaNacimiento ? String(p.fechaNacimiento).slice(0, 10) : '',
            sexo: p.sexo ?? '',
            grupoSanguineo: p.grupoSanguineo ?? '',
            telefono: p.telefono ?? '',
            email: p.email ?? '',
            direccion: p.direccion ?? '',
            epsId: p.epsId ?? null,
            activo: p.activo ?? true,
            departamentoResidencia: p.departamentoResidencia ?? '',
            municipioResidencia: p.municipioResidencia ?? '',
            zonaResidencia: p.zonaResidencia ?? 'URBANA',
            regimenAfiliacion: p.regimenAfiliacion ?? '',
            tipoUsuario: p.tipoUsuario ?? '',
            contactoEmergenciaNombre: p.contactoEmergenciaNombre ?? '',
            contactoEmergenciaTelefono: p.contactoEmergenciaTelefono ?? '',
            estadoCivil: p.estadoCivil ?? '',
            escolaridad: p.escolaridad ?? '',
            ocupacion: p.ocupacion ?? '',
            pertenenciaEtnica: p.pertenenciaEtnica ?? '',
          });
          this.loadingPatient.set(false);
        },
        error: () => {
          this.loadingPatient.set(false);
          this.toast.error('No se pudo cargar la información del paciente.', 'Error');
          this.router.navigate(['/pacientes']);
        },
      });
    }
  }

  nextStep(): void {
    if (!this.validateCurrentStep()) return;
    if (this.currentStep() < this.totalSteps) {
      this.currentStep.update((s) => s + 1);
      this.error = null;
    }
  }

  prevStep(): void {
    if (this.currentStep() > 1) {
      this.currentStep.update((s) => s - 1);
      this.error = null;
    }
  }

  goToStep(stepIndex: number): void {
    const step = stepIndex + 1;
    if (step >= 1 && step <= this.totalSteps && step <= this.currentStep()) {
      this.currentStep.set(step);
      this.error = null;
    }
  }

  /** Valida solo los campos del paso actual. Paso 1 = documento y nombres obligatorios. */
  private validateCurrentStep(): boolean {
    const step = this.currentStep();
    if (step === 1) {
      const doc = this.form.get('documento');
      const nom = this.form.get('nombres');
      doc?.markAsTouched();
      nom?.markAsTouched();
      if (doc?.invalid || nom?.invalid) {
        this.error = 'Complete documento y nombres para continuar';
        return false;
      }
    }
    this.error = null;
    return true;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error = 'Complete los campos obligatorios';
      return;
    }
    this.error = null;
    this.saving.set(true);
    const val = this.form.getRawValue();
    const dto: PacienteRequestDto = {
      documento: val.documento,
      nombres: val.nombres,
      apellidos: val.apellidos || undefined,
      activo: val.activo ?? true,
      tipoDocumento: val.tipoDocumento || undefined,
      fechaNacimiento: val.fechaNacimiento || undefined,
      sexo: val.sexo || undefined,
      grupoSanguineo: val.grupoSanguineo || undefined,
      telefono: val.telefono || undefined,
      email: val.email || undefined,
      direccion: val.direccion || undefined,
      epsId: val.epsId || undefined,
      departamentoResidencia: val.departamentoResidencia || undefined,
      municipioResidencia: val.municipioResidencia || undefined,
      zonaResidencia: val.zonaResidencia || undefined,
      regimenAfiliacion: val.regimenAfiliacion || undefined,
      tipoUsuario: val.tipoUsuario || undefined,
      contactoEmergenciaNombre: val.contactoEmergenciaNombre || undefined,
      contactoEmergenciaTelefono: val.contactoEmergenciaTelefono || undefined,
      estadoCivil: val.estadoCivil || undefined,
      escolaridad: val.escolaridad || undefined,
      ocupacion: val.ocupacion || undefined,
      pertenenciaEtnica: val.pertenenciaEtnica || undefined,
    };
    const obs = this.isEdit && this.id
      ? this.pacienteService.update(this.id, dto)
      : this.pacienteService.create(dto);
    obs.subscribe({
      next: () => {
        this.saving.set(false);
        this.toast.success(
          this.isEdit ? 'Paciente actualizado correctamente.' : 'Paciente creado correctamente.',
          this.isEdit ? 'Actualizado' : 'Creado'
        );
        this.router.navigate(['/pacientes']);
      },
      error: (e) => {
        this.saving.set(false);
        this.error = e.error?.error || e.error?.message || e.message || 'Error al guardar';
        this.toast.error(this.error!, 'Error al guardar');
      },
    });
  }
}
