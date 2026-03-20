/**
 * Componente Consentimiento Informado (Ley 23/1981, Res. 3380/1981).
 * Autor: Ing. J Sebastian Vargas S
 */

import {
  Component, Input, OnInit, inject, signal, computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  ConsentimientoService, ConsentimientoInformadoDto,
} from '../../../core/services/consentimiento.service';
import { SesaToastService } from '../sesa-toast/sesa-toast.component';
import { AuthService } from '../../../core/services/auth.service';
import { SesaConfirmDialogService } from '../sesa-confirm-dialog/sesa-confirm-dialog.component';

const TIPOS_CONSENTIMIENTO = [
  { value: 'GENERAL',        label: 'General',                       icon: '📋' },
  { value: 'QUIRURGICO',     label: 'Quirúrgico / Procedimiento',    icon: '🔪' },
  { value: 'DIAGNOSTICO',    label: 'Diagnóstico (imágenes/lab)',    icon: '🔬' },
  { value: 'ODONTOLOGICO',   label: 'Odontológico',                  icon: '🦷' },
  { value: 'ANESTESIA',      label: 'Anestesia',                     icon: '💉' },
];

@Component({
  standalone: true,
  selector: 'sesa-consentimiento',
  imports: [CommonModule, FormsModule],
  templateUrl: './sesa-consentimiento.component.html',
  styleUrl: './sesa-consentimiento.component.scss',
})
export class SesaConsentimientoComponent implements OnInit {
  @Input({ required: true }) pacienteId!: number;
  @Input() profesionalId?: number;
  @Input() compact = false;

  private readonly svc  = inject(ConsentimientoService);
  private readonly toast = inject(SesaToastService);
  private readonly auth  = inject(AuthService);
  private readonly confirm = inject(SesaConfirmDialogService);

  readonly tiposConsentimiento = TIPOS_CONSENTIMIENTO;

  readonly lista     = signal<ConsentimientoInformadoDto[]>([]);
  readonly loading   = signal(false);
  readonly saving    = signal(false);
  readonly showForm  = signal(false);

  readonly form = signal<Partial<ConsentimientoInformadoDto>>({
    tipo: 'GENERAL',
    estado: 'PENDIENTE',
    procedimiento: '',
    observaciones: '',
  });

  readonly pendientes = computed(() => this.lista().filter(c => c.estado === 'PENDIENTE').length);
  readonly firmados   = computed(() => this.lista().filter(c => c.estado === 'FIRMADO').length);

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.svc.listByPaciente(this.pacienteId).subscribe({
      next: (data) => { this.lista.set(data); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  abrirFormulario(): void {
    const profId = this.profesionalId ?? this.auth.currentUser()?.personalId;
    if (profId == null || profId === undefined) {
      this.toast.warning(
        'No hay profesional vinculado a tu usuario. Un administrador debe asignar tu cuenta a un registro de personal para crear consentimientos.',
        'Profesional requerido'
      );
      return;
    }
    this.form.set({
      tipo: 'GENERAL',
      estado: 'PENDIENTE',
      procedimiento: '',
      observaciones: '',
      pacienteId: this.pacienteId,
      profesionalId: profId,
    });
    this.showForm.set(true);
  }

  cerrarFormulario(): void {
    this.showForm.set(false);
  }

  guardar(): void {
    const f = this.form();
    if (!f.tipo || !f.pacienteId || !f.profesionalId) {
      this.toast.warning('Tipo y profesional son obligatorios.', 'Campos requeridos');
      return;
    }
    this.saving.set(true);
    this.svc.create(f as ConsentimientoInformadoDto).subscribe({
      next: (c) => {
        this.lista.update(l => [c, ...l]);
        this.saving.set(false);
        this.showForm.set(false);
        this.toast.success('Consentimiento creado correctamente.', 'Creado');
      },
      error: (err) => {
        this.saving.set(false);
        const msg = err?.error?.message ?? err?.error?.error ?? 'No se pudo crear el consentimiento.';
        this.toast.error(msg, 'Error');
      },
    });
  }

  async firmar(c: ConsentimientoInformadoDto): Promise<void> {
    const ok = await this.confirm.confirm({
      title: 'Confirmar firma',
      message: `¿El paciente ha firmado el consentimiento "${this.tipoLabel(c.tipo)}"?`,
      type: 'info',
      confirmLabel: 'Sí, firmar',
      cancelLabel: 'Cancelar',
    });
    if (!ok) return;
    this.svc.firmar(c.id!).subscribe({
      next: (updated) => {
        this.lista.update(l => l.map(x => x.id === updated.id ? updated : x));
        this.toast.success('Consentimiento firmado.', 'Firmado');
      },
      error: () => this.toast.error('No se pudo firmar.', 'Error'),
    });
  }

  async rechazar(c: ConsentimientoInformadoDto): Promise<void> {
    const ok = await this.confirm.confirm({
      title: 'Rechazar consentimiento',
      message: `¿El paciente rechaza el consentimiento "${this.tipoLabel(c.tipo)}"?`,
      type: 'warning',
      confirmLabel: 'Rechazar',
      cancelLabel: 'Cancelar',
    });
    if (!ok) return;
    this.svc.rechazar(c.id!).subscribe({
      next: (updated) => {
        this.lista.update(l => l.map(x => x.id === updated.id ? updated : x));
        this.toast.warning('Consentimiento marcado como rechazado.', 'Rechazado');
      },
      error: () => this.toast.error('No se pudo rechazar.', 'Error'),
    });
  }

  async eliminar(c: ConsentimientoInformadoDto): Promise<void> {
    const ok = await this.confirm.confirm({
      title: 'Eliminar consentimiento',
      message: 'Esta acción es irreversible.',
      type: 'danger',
      confirmLabel: 'Eliminar',
      cancelLabel: 'Cancelar',
    });
    if (!ok) return;
    this.svc.delete(c.id!).subscribe({
      next: () => {
        this.lista.update(l => l.filter(x => x.id !== c.id));
        this.toast.success('Consentimiento eliminado.', 'Eliminado');
      },
      error: () => this.toast.error('No se pudo eliminar.', 'Error'),
    });
  }

  // ── Helpers ──────────────────────────────────────────────────────────
  tipoLabel(tipo?: string): string {
    return TIPOS_CONSENTIMIENTO.find(t => t.value === tipo)?.label ?? (tipo ?? '—');
  }

  tipoIcon(tipo?: string): string {
    return TIPOS_CONSENTIMIENTO.find(t => t.value === tipo)?.icon ?? '📋';
  }

  estadoClass(estado?: string): string {
    const map: Record<string, string> = {
      PENDIENTE: 'consent-badge--pending',
      FIRMADO:   'consent-badge--ok',
      RECHAZADO: 'consent-badge--danger',
      REVOCADO:  'consent-badge--secondary',
    };
    return map[estado ?? ''] ?? 'consent-badge--secondary';
  }

  estadoLabel(estado?: string): string {
    const map: Record<string, string> = {
      PENDIENTE: 'Pendiente',
      FIRMADO:   'Firmado',
      RECHAZADO: 'Rechazado',
      REVOCADO:  'Revocado',
    };
    return map[estado ?? ''] ?? (estado ?? '—');
  }

  formatFecha(iso?: string): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  setFormField(key: keyof ConsentimientoInformadoDto, val: unknown): void {
    this.form.update(f => ({ ...f, [key]: val }));
  }
}
