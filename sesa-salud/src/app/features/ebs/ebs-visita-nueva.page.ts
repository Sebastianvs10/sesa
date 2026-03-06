/**
 * Formulario Nueva Visita Domiciliaria EBS.
 * Autor: Ing. J Sebastian Vargas S
 */

import { ChangeDetectionStrategy, Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { EbsService, EbsHouseholdSummary, EbsTerritorySummary, EbsBrigadeDto } from '../../core/services/ebs.service';
import { IgacService, IgacVereda } from '../../core/services/igac.service';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';

@Component({
  standalone: true,
  selector: 'sesa-ebs-visita-nueva',
  imports: [CommonModule, FormsModule, RouterLink, SesaCardComponent],
  templateUrl: './ebs-visita-nueva.page.html',
  styleUrl: './ebs-visita-nueva.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EbsVisitaNuevaPageComponent implements OnInit {
  private readonly ebs = inject(EbsService);
  private readonly igac = inject(IgacService);
  private readonly toast = inject(SesaToastService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly territories = signal<EbsTerritorySummary[]>([]);
  readonly households = signal<EbsHouseholdSummary[]>([]);
  readonly brigades = signal<EbsBrigadeDto[]>([]);
  readonly veredas = signal<IgacVereda[]>([]);
  readonly loadingTerritories = signal(false);
  readonly loadingHouseholds = signal(false);
  readonly saving = signal(false);

  selectedTerritoryId: number | null = null;
  selectedHouseholdId: number | null = null;
  visitDate = '';
  visitType = 'DOMICILIARIA_APS';
  tipoIntervencion = '';
  veredaCodigo = '';
  diagnosticoCie10 = '';
  planCuidado = '';
  brigadeId: number | null = null;
  motivo = '';
  notes = '';
  riskCardiovascular = false;
  riskMaterno = false;
  riskCronico = false;

  /** Intervenciones por dependencia (se concatenan a notes) */
  intervenciones: { tipo: string; detalle: string }[] = [];
  nuevaIntervencionTipo = 'ENFERMERIA';
  nuevaIntervencionDetalle = '';

  /** Errores de validación (clave = nombre del campo) */
  errors: { territorio?: string; hogar?: string; fecha?: string; cie10?: string } = {};

  ngOnInit(): void {
    this.visitDate = new Date().toISOString().slice(0, 16);
    this.route.queryParams.subscribe((qp) => {
      const territorio = qp['territorio'];
      const hogar = qp['hogar'];
      if (territorio != null) {
        const tid = Number(territorio);
        if (!isNaN(tid)) this.selectedTerritoryId = tid;
      }
      if (hogar != null) {
        const id = Number(hogar);
        if (!isNaN(id)) this.selectedHouseholdId = id;
      }
    });
    this.loadTerritories();
  }

  loadTerritories(): void {
    this.loadingTerritories.set(true);
    this.ebs.listTerritories({}).subscribe({
      next: (list) => {
        this.territories.set(list ?? []);
        this.loadingTerritories.set(false);
        if (this.selectedTerritoryId != null) {
          this.loadHouseholds();
          this.loadBrigades();
          this.loadVeredasForTerritory();
        } else if ((list?.length ?? 0) > 0) {
          this.selectedTerritoryId = list![0].id;
          this.loadHouseholds();
          this.loadBrigades();
          this.loadVeredasForTerritory();
        }
      },
      error: () => this.loadingTerritories.set(false),
    });
  }

  onTerritoryChange(): void {
    this.selectedHouseholdId = null;
    this.veredaCodigo = '';
    this.veredas.set([]);
    this.loadHouseholds();
    this.loadBrigades();
    this.loadVeredasForTerritory();
  }

  private loadVeredasForTerritory(): void {
    const t = this.territories().find((x) => x.id === this.selectedTerritoryId);
    if (t?.igacMunicipioCodigo) {
      this.igac.listVeredas(t.igacMunicipioCodigo).subscribe({ next: (list) => this.veredas.set(list ?? []) });
    } else {
      this.veredas.set([]);
    }
  }

  private loadBrigades(): void {
    if (this.selectedTerritoryId == null) {
      this.ebs.listBrigades().subscribe({ next: (list) => this.brigades.set(list ?? []) });
      return;
    }
    this.ebs.listBrigades(this.selectedTerritoryId).subscribe({ next: (list) => this.brigades.set(list ?? []) });
  }

  loadHouseholds(): void {
    if (this.selectedTerritoryId == null) return;
    this.loadingHouseholds.set(true);
    this.ebs.listHouseholds(this.selectedTerritoryId, {}).subscribe({
      next: (list) => {
        this.households.set(list ?? []);
        this.loadingHouseholds.set(false);
      },
      error: () => this.loadingHouseholds.set(false),
    });
  }

  addIntervencion(): void {
    if (!this.nuevaIntervencionDetalle.trim()) return;
    this.intervenciones.push({ tipo: this.nuevaIntervencionTipo, detalle: this.nuevaIntervencionDetalle.trim() });
    this.nuevaIntervencionDetalle = '';
  }

  removeIntervencion(i: number): void {
    this.intervenciones.splice(i, 1);
  }

  /** Valida formato CIE-10 (opcional: vacío o letra + 2 dígitos, opcional . + 1-2 dígitos). */
  validateCie10(value: string): boolean {
    const v = (value || '').trim();
    if (!v) return true;
    return /^[A-Za-z]\d{2}(\.\d{1,2})?$/.test(v);
  }

  onCie10Blur(): void {
    if (this.diagnosticoCie10?.trim() && !this.validateCie10(this.diagnosticoCie10)) {
      this.errors = { ...this.errors, cie10: 'Formato no válido (letra + 2 dígitos, opcional .XX)' };
    } else {
      const { cie10: _, ...rest } = this.errors;
      this.errors = rest;
    }
  }

  clearError(field: 'territorio' | 'hogar' | 'fecha' | 'cie10'): void {
    const { [field]: _, ...rest } = this.errors;
    this.errors = rest;
  }

  private setValidationErrors(): boolean {
    this.errors = {};
    if (this.selectedTerritoryId == null) this.errors['territorio'] = 'Selecciona un microterritorio';
    if (this.selectedHouseholdId == null) this.errors['hogar'] = 'Selecciona un hogar';
    if (!this.visitDate?.trim()) this.errors['fecha'] = 'Indica la fecha y hora de la visita';
    if (this.diagnosticoCie10?.trim() && !this.validateCie10(this.diagnosticoCie10)) {
      this.errors['cie10'] = 'Formato no válido (ej.: A00, Z00.0, O80)';
    }
    return Object.keys(this.errors).length === 0;
  }

  submit(): void {
    if (!this.setValidationErrors()) {
      this.toast.warning('Revisa los campos marcados.', 'Nueva visita');
      return;
    }
    const householdId = this.selectedHouseholdId!;
    this.saving.set(true);
    let notesFinal = this.notes || '';
    if (this.intervenciones.length > 0) {
      const parts = this.intervenciones.map((i) => `[${i.tipo}]: ${i.detalle}`);
      notesFinal = notesFinal ? `${notesFinal}\n\n${parts.join('\n')}` : parts.join('\n');
    }
    const payload = {
      householdId,
      visitDate: (this.visitDate ? new Date(this.visitDate) : new Date()).toISOString(),
      visitType: this.visitType,
      tipoIntervencion: this.tipoIntervencion || undefined,
      veredaCodigo: this.veredaCodigo || undefined,
      diagnosticoCie10: this.diagnosticoCie10 || undefined,
      planCuidado: this.planCuidado || undefined,
      brigadeId: this.brigadeId ?? undefined,
      motivo: this.motivo || undefined,
      notes: notesFinal || undefined,
      riskFlags: {
        cardiovascular: this.riskCardiovascular || undefined,
        materno: this.riskMaterno || undefined,
        cronico: this.riskCronico || undefined,
      },
    };
    this.ebs.createHomeVisit(payload).subscribe({
      next: (res: unknown) => {
        const body = res as { offline?: boolean };
        this.saving.set(false);
        if (body?.offline) {
          this.toast.info('Visita encolada. Se sincronizará con conexión.', 'EBS');
        } else {
          this.toast.success('Visita domiciliaria registrada.', 'EBS');
          this.router.navigate(['/ebs/visitas']);
        }
      },
      error: (err) => {
        this.saving.set(false);
        this.toast.error(err?.error?.message ?? 'No se pudo guardar la visita.', 'EBS');
      },
    });
  }
}
