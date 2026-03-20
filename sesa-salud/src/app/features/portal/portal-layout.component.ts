/**
 * Autor: Ing. J Sebastian Vargas S
 */

import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  computed,
  HostListener,
} from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';
import { ThemeService } from '../../core/services/theme.service';

interface NavItem {
  label: string;
  route: string;
  icon: string;
  activeIcon: string;
}

@Component({
  selector: 'sesa-portal-layout',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule],
  template: `
    <div class="portal-shell" [class.ios-device]="isIos()">

      <!-- Top Bar -->
      <header class="portal-topbar">
        <div class="portal-topbar__inner">
          <div class="portal-topbar__brand">
            <div class="portal-topbar__logo-wrap">
              <svg width="28" height="28" viewBox="0 0 32 32" fill="none">
                <rect width="32" height="32" rx="10" fill="var(--portal-accent)"/>
                <path d="M16 7v18M7 16h18" stroke="white" stroke-width="3" stroke-linecap="round"/>
              </svg>
            </div>
            <div class="portal-topbar__title-group">
              <span class="portal-topbar__name">SESA Salud</span>
              <span class="portal-topbar__sub">Portal del Paciente</span>
            </div>
          </div>
          <div class="portal-topbar__actions">
            <button class="portal-topbar__icon-btn" (click)="toggleTheme()" [attr.aria-label]="isDark() ? 'Cambiar a tema claro' : 'Cambiar a tema oscuro'">
              @if (isDark()) {
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/>
                  <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
                  <line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/>
                  <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
                </svg>
              } @else {
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
                </svg>
              }
            </button>
            <button class="portal-topbar__icon-btn portal-topbar__icon-btn--notif" aria-label="Notificaciones">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/>
              </svg>
            </button>
          </div>
        </div>
      </header>

      <!-- Contenido principal -->
      <main class="portal-main">
        <router-outlet />
      </main>

      <!-- Bottom Navigation -->
      <nav class="portal-bottomnav" role="navigation" aria-label="Navegación principal">
        @for (item of navItems; track item.route) {
          <a
            [routerLink]="item.route"
            routerLinkActive="portal-bottomnav__item--active"
            class="portal-bottomnav__item"
            [attr.aria-label]="item.label"
          >
            <span class="portal-bottomnav__icon" [innerHTML]="item.icon"></span>
            <span class="portal-bottomnav__label">{{ item.label }}</span>
            <span class="portal-bottomnav__indicator"></span>
          </a>
        }
      </nav>

    </div>
  `,
  styleUrl: './portal-layout.component.scss',
})
export class PortalLayoutComponent {
  private readonly auth = inject(AuthService);
  private readonly themeService = inject(ThemeService);
  private readonly router = inject(Router);

  readonly isDark = computed(() => this.themeService.isDark());
  readonly isIos = signal(this.detectIos());

  readonly navItems: NavItem[] = [
    {
      label: 'Inicio',
      route: '/portal/inicio',
      icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>`,
      activeIcon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor"><path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/></svg>`,
    },
    {
      label: 'Mi HC',
      route: '/portal/historia-clinica',
      icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="12" y1="18" x2="12" y2="12"/><line x1="9" y1="15" x2="15" y2="15"/></svg>`,
      activeIcon: '',
    },
    {
      label: 'Laboratorios',
      route: '/portal/laboratorios',
      icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 3H5a2 2 0 0 0-2 2v4m6-6h10a2 2 0 0 1 2 2v4M9 3v11l-2 3a1 1 0 0 0 .85 1.5h8.3A1 1 0 0 0 17 17l-2-3V3"/></svg>`,
      activeIcon: '',
    },
    {
      label: 'Resultados',
      route: '/portal/resultados',
      icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>`,
      activeIcon: '',
    },
    {
      label: 'Órdenes',
      route: '/portal/ordenes',
      icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2"/><rect x="9" y="3" width="6" height="4" rx="2"/><path d="M9 12h6M9 16h4"/></svg>`,
      activeIcon: '',
    },
    {
      label: 'Consentimientos',
      route: '/portal/consentimientos',
      icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/></svg>`,
      activeIcon: '',
    },
    {
      label: 'Perfil',
      route: '/portal/perfil',
      icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>`,
      activeIcon: '',
    },
  ];

  toggleTheme(): void {
    this.themeService.toggle();
  }

  private detectIos(): boolean {
    if (typeof navigator === 'undefined') return false;
    return /iPad|iPhone|iPod/.test(navigator.userAgent);
  }
}
