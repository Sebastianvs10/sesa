/**
 * Crear territorio EBS — formulario premium.
 * Autor: Ing. J Sebastian Vargas S
 */

import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { EbsService, EbsTerritoryCreate } from '../../core/services/ebs.service';
import { IgacService, IgacDepartamento, IgacMunicipio, IgacVereda } from '../../core/services/igac.service';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';

@Component({
  standalone: true,
  selector: 'sesa-ebs-territorio-crear',
  imports: [CommonModule, ReactiveFormsModule, RouterLink, SesaCardComponent],
  templateUrl: './ebs-territorio-crear.page.html',
  styleUrl: './ebs-territorio-crear.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EbsTerritorioCrearPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly ebs = inject(EbsService);
  private readonly igac = inject(IgacService);
  private readonly router = inject(Router);
  private readonly toast = inject(SesaToastService);

  readonly loading = signal(false);
  readonly departamentos = signal<IgacDepartamento[]>([]);
  readonly municipios = signal<IgacMunicipio[]>([]);
  readonly veredas = signal<IgacVereda[]>([]);

  form = this.fb.group({
    code: ['', [Validators.required, Validators.maxLength(50)]],
    name: ['', [Validators.required, Validators.maxLength(200)]],
    type: ['MICROTERRITORIO', Validators.maxLength(50)],
    igacDepartamentoCodigo: [''],
    igacMunicipioCodigo: [''],
    igacVeredaCodigo: [''],
  });

  constructor() {
    this.igac.listDepartamentos().subscribe({
      next: (list) => this.departamentos.set(list),
      error: () => this.toast.error('No se pudieron cargar departamentos'),
    });
  }

  onDepartamentoChange(): void {
    const cod = this.form.get('igacDepartamentoCodigo')?.value;
    this.form.patchValue({ igacMunicipioCodigo: '', igacVeredaCodigo: '' });
    this.municipios.set([]);
    this.veredas.set([]);
    if (cod) {
      this.igac.listMunicipios(cod).subscribe({ next: (list) => this.municipios.set(list) });
    }
  }

  onMunicipioChange(): void {
    const cod = this.form.get('igacMunicipioCodigo')?.value;
    this.form.patchValue({ igacVeredaCodigo: '' });
    this.veredas.set([]);
    if (cod) {
      this.igac.listVeredas(cod).subscribe({ next: (list) => this.veredas.set(list) });
    }
  }

  submit(): void {
    if (this.form.invalid || this.loading()) return;
    const v = this.form.getRawValue();
    const dto: EbsTerritoryCreate = {
      code: v.code!,
      name: v.name!,
      type: v.type ?? undefined,
      igacDepartamentoCodigo: v.igacDepartamentoCodigo || undefined,
      igacMunicipioCodigo: v.igacMunicipioCodigo || undefined,
      igacVeredaCodigo: v.igacVeredaCodigo || undefined,
    };
    this.loading.set(true);
    this.ebs.createTerritory(dto).subscribe({
      next: () => {
        this.toast.success('Territorio creado correctamente');
        this.router.navigate(['/ebs/territorios']);
      },
      error: (err) => {
        this.loading.set(false);
        this.toast.error(err?.error?.message || 'Error al crear territorio');
      },
    });
  }
}
