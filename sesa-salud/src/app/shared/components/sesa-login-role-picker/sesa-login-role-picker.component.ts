/**
 * Modal de selección de rol activo tras login (multi-rol).
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  computed,
  input,
  output,
  afterNextRender,
} from '@angular/core';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  IconDefinition,
  faBriefcaseMedical,
  faBuilding,
  faCapsules,
  faChartLine,
  faClipboardUser,
  faFileInvoiceDollar,
  faHeadset,
  faHospital,
  faHouseMedical,
  faKey,
  faMapLocationDot,
  faShieldHalved,
  faStethoscope,
  faTeeth,
  faUser,
  faUserDoctor,
  faUserGear,
  faUserNurse,
  faVial,
} from '@fortawesome/free-solid-svg-icons';
import { labelForRole, sortRolesForPicker } from '../../../core/constants/role-labels';

function normRole(r: string): string {
  return r.toUpperCase().replace(/^ROLE_/, '');
}

function iconForRole(code: string): IconDefinition {
  const c = normRole(code);
  const map: Record<string, IconDefinition> = {
    SUPERADMINISTRADOR: faKey,
    ADMIN: faUserGear,
    MEDICO: faUserDoctor,
    ODONTOLOGO: faTeeth,
    BACTERIOLOGO: faVial,
    ENFERMERO: faUserNurse,
    ENFERMERA: faUserNurse,
    JEFE_ENFERMERIA: faHospital,
    AUXILIAR_ENFERMERIA: faUserNurse,
    PSICOLOGO: faClipboardUser,
    REGENTE_FARMACIA: faCapsules,
    RECEPCIONISTA: faHeadset,
    FACTURACION: faFileInvoiceDollar,
    COORDINADOR_MEDICO: faStethoscope,
    EBS: faHouseMedical,
    COORDINADOR_TERRITORIAL: faMapLocationDot,
    SUPERVISOR_APS: faChartLine,
    PACIENTE: faUser,
  };
  return map[c] ?? faBriefcaseMedical;
}

@Component({
  standalone: true,
  selector: 'sesa-login-role-picker',
  imports: [CommonModule, FontAwesomeModule],
  templateUrl: './sesa-login-role-picker.component.html',
  styleUrl: './sesa-login-role-picker.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'sesa-login-role-picker-host',
  },
})
export class SesaLoginRolePickerComponent {
  readonly faBuilding = faBuilding;
  readonly faShieldIcon = faShieldHalved;

  /** Códigos de rol devueltos por el backend */
  readonly roles = input.required<string[]>();
  readonly userDisplayName = input<string>('');
  readonly empresaNombre = input<string | undefined>(undefined);
  /** Rol sugerido (p. ej. el primario del token) — se resalta la tarjeta */
  readonly suggestedRole = input<string | undefined>(undefined);

  readonly confirmed = output<string>();
  readonly cancelled = output<void>();

  readonly sortedRoles = computed(() => sortRolesForPicker(this.roles()));

  readonly suggestedNorm = computed(() => {
    const s = this.suggestedRole();
    return s ? normRole(s) : '';
  });

  constructor() {
    afterNextRender(() => {
      document.getElementById('srl-first-role-btn')?.focus();
    });
  }

  label(code: string): string {
    return labelForRole(code);
  }

  icon(code: string): IconDefinition {
    return iconForRole(code);
  }

  isSuggested(code: string): boolean {
    const sn = this.suggestedNorm();
    return !!sn && normRole(code) === sn;
  }

  pick(rol: string): void {
    this.confirmed.emit(rol);
  }

  /** Código sin prefijo ROLE_ para mostrar en UI */
  roleCodeForDisplay(code: string): string {
    return normRole(code);
  }

  @HostListener('document:keydown', ['$event'])
  onDocumentKeydown(ev: KeyboardEvent): void {
    if (ev.key === 'Escape') {
      ev.preventDefault();
      this.cancelled.emit();
    }
  }

  logoutClick(): void {
    this.cancelled.emit();
  }
}
