/**
 * Layout del módulo EBS — navegación por rol (Profesional EBS, Coordinador Territorial, Supervisor APS).
 * Web: sidebar; móvil: tabs inferiores con menú "Más". Mismos endpoints en web y móvil.
 * Autor: Ing. J Sebastian Vargas S
 */

import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';
import { EbsSyncService } from '../../core/services/ebs-sync.service';

export interface EbsNavItem {
  label: string;
  route: string;
  icon: string;
  /** Etiqueta corta para barra móvil */
  labelShort?: string;
  roles: string[];
  roleOnly?: boolean;
}

@Component({
  standalone: true,
  selector: 'sesa-ebs-layout',
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './ebs-layout.component.html',
  styleUrl: './ebs-layout.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EbsLayoutComponent {
  private readonly auth = inject(AuthService);
  readonly ebsSync = inject(EbsSyncService);

  readonly showMoreMenu = signal(false);

  readonly navItems: EbsNavItem[] = [
    { label: 'Inicio', route: '/ebs/inicio', icon: 'home', labelShort: 'Inicio', roles: ['EBS', 'COORDINADOR_TERRITORIAL', 'SUPERVISOR_APS'] },
    { label: 'Territorios y hogares', route: '/ebs/territorios', icon: 'map', labelShort: 'Mapa', roles: ['EBS', 'COORDINADOR_TERRITORIAL', 'SUPERVISOR_APS'] },
    { label: 'Brigadas', route: '/ebs/brigadas', icon: 'calendar-check', labelShort: 'Brigadas', roles: ['EBS', 'COORDINADOR_TERRITORIAL', 'SUPERVISOR_APS'] },
    { label: 'Visitas', route: '/ebs/visitas', icon: 'clipboard-list', labelShort: 'Visitas', roles: ['EBS', 'COORDINADOR_TERRITORIAL', 'SUPERVISOR_APS'] },
    { label: 'Nueva visita', route: '/ebs/visita/nueva', icon: 'plus-circle', labelShort: 'Nueva', roles: ['EBS', 'COORDINADOR_TERRITORIAL'] },
    { label: 'Asignación territorial', route: '/ebs/asignacion', icon: 'users-cog', labelShort: 'Asignar', roles: ['COORDINADOR_TERRITORIAL'], roleOnly: true },
    { label: 'Reportes', route: '/ebs/reportes', icon: 'file-chart', labelShort: 'Reportes', roles: ['COORDINADOR_TERRITORIAL', 'SUPERVISOR_APS'] },
    { label: 'Alertas', route: '/ebs/alertas', icon: 'alert-triangle', labelShort: 'Alertas', roles: ['EBS', 'COORDINADOR_TERRITORIAL', 'SUPERVISOR_APS'] },
    { label: 'Dashboard APS', route: '/ebs/dashboard-supervisor', icon: 'chart-bar', labelShort: 'Dashboard', roles: ['SUPERVISOR_APS'], roleOnly: true },
  ];

  readonly currentRoles = computed(() => this.auth.currentRoles());
  readonly isCoordinador = computed(() => this.currentRoles().includes('COORDINADOR_TERRITORIAL'));
  readonly isSupervisor = computed(() => this.currentRoles().includes('SUPERVISOR_APS'));

  visibleNavItems = computed(() => {
    const roles = this.currentRoles();
    const hasEbs = roles.some(r => ['EBS', 'COORDINADOR_TERRITORIAL', 'SUPERVISOR_APS', 'ADMIN', 'SUPERADMINISTRADOR'].includes(r));
    return this.navItems.filter(item => {
      const hasRole = item.roles.some(r => roles.includes(r));
      if (item.roleOnly) return hasRole;
      return hasEbs && hasRole;
    });
  });

  /** En móvil: primeros 3 ítems fijos + "Más" con el resto */
  mobileMainTabs = computed(() => this.visibleNavItems().slice(0, 3));
  mobileMoreTabs = computed(() => this.visibleNavItems().slice(3));

  toggleMoreMenu(): void {
    this.showMoreMenu.update(v => !v);
  }

  closeMoreMenu(): void {
    this.showMoreMenu.set(false);
  }

  syncNow(): void {
    this.ebsSync.syncNow();
  }
}
