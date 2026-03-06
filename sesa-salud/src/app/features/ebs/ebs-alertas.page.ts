/**
 * Alertas EBS — listado y creación (carga manual + integración futura).
 * Autor: Ing. J Sebastian Vargas S
 */

import { ChangeDetectionStrategy, Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EbsService, EbsAlertDto } from '../../core/services/ebs.service';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { SesaSkeletonComponent } from '../../shared/components/sesa-skeleton/sesa-skeleton.component';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';
import { FormsModule } from '@angular/forms';

@Component({
  standalone: true,
  selector: 'sesa-ebs-alertas',
  imports: [CommonModule, FormsModule, SesaCardComponent, SesaSkeletonComponent],
  templateUrl: './ebs-alertas.page.html',
  styleUrl: './ebs-alertas.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EbsAlertasPageComponent {
  private readonly ebs = inject(EbsService);
  private readonly toast = inject(SesaToastService);

  readonly alerts = signal<EbsAlertDto[]>([]);
  readonly loading = signal(true);
  readonly filterStatus = signal<string>('');
  readonly filterType = signal<string>('');
  readonly showForm = signal(false);
  readonly saving = signal(false);

  form = {
    type: 'EPIDEMIOLOGICA',
    title: '',
    description: '',
    alertDate: new Date().toISOString().slice(0, 10),
    status: 'ACTIVA',
  };

  filteredAlerts = computed(() => {
    let list = this.alerts();
    const status = this.filterStatus();
    const type = this.filterType();
    if (status) list = list.filter((a) => (a.status || '').toUpperCase() === status.toUpperCase());
    if (type) list = list.filter((a) => (a.type || '').toUpperCase() === type.toUpperCase());
    return list;
  });

  readonly ALERT_TYPE_OPTIONS: { value: string; label: string }[] = [
    { value: '', label: 'Todos los tipos' },
    { value: 'EPIDEMIOLOGICA', label: 'Epidemiológica' },
    { value: 'GEOGRAFICA', label: 'Geográfica' },
    { value: 'VIGILANCIA', label: 'Vigilancia' },
  ];

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.ebs.listAlerts(undefined).subscribe({
      next: (list) => {
        this.alerts.set(list ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.toast.error('Error al cargar alertas');
      },
    });
  }

  openForm(): void {
    this.form = {
      type: 'EPIDEMIOLOGICA',
      title: '',
      description: '',
      alertDate: new Date().toISOString().slice(0, 10),
      status: 'ACTIVA',
    };
    this.showForm.set(true);
  }

  submitAlert(): void {
    if (!this.form.title.trim() || this.saving()) return;
    this.saving.set(true);
    const dto: EbsAlertDto = {
      type: this.form.type,
      title: this.form.title.trim(),
      description: this.form.description.trim() || undefined,
      alertDate: this.form.alertDate,
      status: this.form.status,
    };
    this.ebs.createAlert(dto).subscribe({
      next: () => {
        this.saving.set(false);
        this.showForm.set(false);
        this.toast.success('Alerta registrada');
        this.load();
      },
      error: () => {
        this.saving.set(false);
        this.toast.error('Error al guardar');
      },
    });
  }

  closeForm(): void {
    this.showForm.set(false);
  }

  markResolved(id: number): void {
    this.ebs.updateAlertStatus(id, 'RESUELTA').subscribe({
      next: () => {
        this.toast.success('Alerta marcada como resuelta');
        this.load();
      },
    });
  }
}
