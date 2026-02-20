/**
 * Formulario de Paciente — skeleton modo editar, spinner guardar, toast CRUD.
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { PacienteService, PacienteRequestDto } from '../../core/services/paciente.service';
import { EpsService, EpsDto } from '../../core/services/eps.service';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { SesaFormFieldComponent } from '../../shared/components/sesa-form-field/sesa-form-field.component';
import { SesaSkeletonComponent } from '../../shared/components/sesa-skeleton/sesa-skeleton.component';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';

@Component({
  standalone: true,
  selector: 'sesa-paciente-form-page',
  imports: [CommonModule, ReactiveFormsModule, RouterLink, SesaCardComponent, SesaFormFieldComponent, SesaSkeletonComponent],
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

  form!: FormGroup;
  epsList: EpsDto[] = [];
  isEdit = false;
  id: number | null = null;
  loadingPatient = signal(false);
  saving = signal(false);
  error: string | null = null;

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
