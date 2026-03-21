import { CommonModule } from '@angular/common';
import { Component, HostListener, inject, OnInit, OnDestroy, ViewChild, ElementRef, signal, computed } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  faChartLine,
  faUsers,
  faClipboardList,
  faMagnifyingGlass,
  faPlus,
  faFlask,
  faImage,
  faTruckMedical,
  faBed,
  faPills,
  faFileInvoice,
  faCalendarDays,
  faCalendarCheck,
  faHeartPulse,
  faBuilding,
  faBuildingColumns,
  faShieldHalved,
  faBell,
  faSun,
  faMoon,
  faChartBar,
  faUserDoctor,
  faTooth,
  faUserTie,
  faMapLocationDot,
  faKey,
} from '@fortawesome/free-solid-svg-icons';
import { AuthService } from './core/services/auth.service';
import { EmpresaCurrentService } from './core/services/empresa-current.service';
import { IdleTimeoutService } from './core/services/idle-timeout.service';
import { PermissionsService } from './core/services/permissions.service';
import { ThemeService } from './core/services/theme.service';
import { ConnectionService } from './core/offline/connection.service';
import { NotificacionService, NotificacionDto } from './core/services/notificacion.service';
import { interval, Subscription, forkJoin } from 'rxjs';
import { SesaBreadcrumbComponent } from './shared/components/sesa-breadcrumb/sesa-breadcrumb.component';
import { OfflineStatusComponent } from './shared/components/offline-status/offline-status.component';
import { SesaToastContainerComponent } from './shared/components/sesa-toast/sesa-toast.component';
import { SesaLoadingOverlayComponent } from './shared/components/sesa-loading-overlay/sesa-loading-overlay.component';
import { SesaConfirmDialogOutletComponent } from './shared/components/sesa-confirm-dialog/sesa-confirm-dialog.component';
import { SesaPerfilModalOutletComponent, SesaPerfilModalService } from './shared/components/sesa-perfil-modal/sesa-perfil-modal.component';

// ── Tipos del catálogo de sidebar ────────────────────────────────────────────

export interface SidebarChild {
  label: string;
  route: string;
  icon: unknown;
  /** Si se define, el enlace solo aparece para estos roles. */
  requiresRoles?: string[];
}

export interface SidebarEntry {
  /** Código(s) de módulo. Cualquiera que el usuario tenga → el ítem es visible. */
  codigo: string | string[];
  label: string;
  route?: string;
  icon: unknown;
  exactRoute?: boolean;
  /** Solo visible para SUPERADMINISTRADOR, sin importar los módulos. */
  superAdminOnly?: boolean;
  /** Si tiene children es un grupo expandible (sin ruta propia). */
  children?: SidebarChild[];
}

export interface SidebarSection {
  title: string;
  entries: SidebarEntry[];
}

/** Entrada ya resuelta para el template (con hijos filtrados por rol). */
export interface ResolvedEntry {
  codigo: string | string[];
  label: string;
  route?: string;
  icon: unknown;
  exactRoute?: boolean;
  superAdminOnly?: boolean;
  children?: SidebarChild[];
  visibleChildren: SidebarChild[];
}

export interface ResolvedSection {
  title: string;
  entries: ResolvedEntry[];
}

export interface CmdItem {
  label: string;
  route: string;
  section: string;
  icon: unknown;
}

// ── Catálogo del sidebar (metadatos UI, no permisos) ─────────────────────────
// Las RUTAS, ICONOS y ETIQUETAS son constantes de la UI.
// Los PERMISOS (qué rol ve qué módulo) vienen exclusivamente de la BD.

const SIDEBAR_CATALOG: SidebarSection[] = [
  {
    title: 'Principal',
    entries: [
      {
        codigo: 'DASHBOARD', label: 'Dashboard',
        route: '/dashboard', icon: faChartLine, exactRoute: true,
      },
    ],
  },
  {
    title: 'Atención',
    entries: [
      { codigo: 'PACIENTES', label: 'Pacientes', route: '/pacientes', icon: faUsers },
      {
        codigo: 'HISTORIA_CLINICA', label: 'Historia clínica', icon: faClipboardList,
        children: [
          { label: 'Consultar Historia clínica', route: '/historia-clinica', icon: faMagnifyingGlass },
          {
            label: 'Crear Historia Clínica', route: '/historia-clinica/nueva', icon: faPlus,
            requiresRoles: ['MEDICO', 'ADMIN', 'SUPERADMINISTRADOR'],
          },
        ],
      },
      { codigo: 'LABORATORIOS',        label: 'Laboratorios',           route: '/laboratorios',         icon: faFlask },
      { codigo: 'IMAGENES',            label: 'Imágenes diagnósticas',  route: '/imagenes-diagnosticas', icon: faImage },
      { codigo: 'URGENCIAS',           label: 'Urgencias',              route: '/urgencias',             icon: faTruckMedical },
      { codigo: 'HOSPITALIZACION',     label: 'Hospitalización',        route: '/hospitalizacion',       icon: faBed },
      { codigo: 'FARMACIA',            label: 'Farmacia',               route: '/farmacia',              icon: faPills },
      { codigo: 'FACTURACION',         label: 'Facturación',            route: '/facturacion',           icon: faFileInvoice },
      { codigo: 'CITAS',               label: 'Citas',                  route: '/citas',                 icon: faCalendarDays },
      { codigo: 'AGENDA',              label: 'Agenda',                 route: '/agenda',                icon: faCalendarCheck },
      { codigo: 'EVOLUCION_ENFERMERIA',label: 'Evolución Enfermería',   route: '/evolucion-enfermeria',  icon: faHeartPulse },
      { codigo: 'CONSULTA_MEDICA',     label: 'Consulta Médica',        route: '/consulta-medica',       icon: faUserDoctor },
      { codigo: 'ODONTOLOGIA',         label: 'Odontología',            route: '/odontologia',           icon: faTooth },
      { codigo: 'EBS',                 label: 'Equipos Básicos de Salud (EBS)', route: '/ebs', icon: faMapLocationDot },
    ],
  },
  {
    title: 'Administración',
    entries: [
      // Mi empresa: visible si tiene EMPRESAS o PERSONAL
      { codigo: ['EMPRESAS', 'PERSONAL'], label: 'Mi empresa',       route: '/mi-empresa',  icon: faBuilding },
      // S12: API Keys para integradores (laboratorio, PACS, signos vitales)
      { codigo: 'EMPRESAS', label: 'API Keys (Integradores)', route: '/api-keys', icon: faKey },
      // Gestión empresas: solo SUPERADMINISTRADOR
      { codigo: 'EMPRESAS', label: 'Gestión empresas', route: '/empresas',    icon: faBuildingColumns, superAdminOnly: true },
      { codigo: 'PERSONAL',       label: 'Personal',        route: '/personal',      icon: faUserTie },
      { codigo: 'USUARIOS',       label: 'Usuarios Adm',    route: '/usuarios',      icon: faShieldHalved },
      { codigo: 'REPORTES',       label: 'Reportes',        route: '/reportes',      icon: faChartBar },
      { codigo: 'NOTIFICACIONES', label: 'Notificaciones',  route: '/notificaciones',icon: faBell },
      { codigo: 'ROLES',          label: 'Roles del sistema',route: '/roles',        icon: faShieldHalved },
    ],
  },
];

// ── Componente raíz ───────────────────────────────────────────────────────────

@Component({
  selector: 'sesa-root',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    FontAwesomeModule,
    SesaBreadcrumbComponent,
    OfflineStatusComponent,
    SesaToastContainerComponent,
    SesaLoadingOverlayComponent,
    SesaConfirmDialogOutletComponent,
    SesaPerfilModalOutletComponent,
  ],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent implements OnInit, OnDestroy {
  pageTitle = 'Dashboard clínico';
  showProfileMenu = false;
  showNotificationsMenu = false;
  @ViewChild('profileRef') profileRef!: ElementRef<HTMLElement>;
  @ViewChild('notificationsRef') notificationsRef!: ElementRef<HTMLElement>;
  @ViewChild('cmdInput') cmdInputRef?: ElementRef<HTMLInputElement>;

  /* Iconos topbar */
  faSun  = faSun;
  faMoon = faMoon;
  faBell = faBell;

  authService         = inject(AuthService);
  perfilModal         = inject(SesaPerfilModalService);
  themeService        = inject(ThemeService);
  permissions         = inject(PermissionsService);
  empresaCurrent      = inject(EmpresaCurrentService);
  connectionService   = inject(ConnectionService);
  notificacionService = inject(NotificacionService);
  private idleTimeout = inject(IdleTimeoutService);
  private router      = inject(Router);

  isSuperAdmin = this.authService.isSuperAdmin;

  /** Estado de pin del sidebar (fijado abierto o colapsado auto). */
  sidebarPinned = signal(false);
  toggleSidebarPin() { this.sidebarPinned.update(v => !v); }

  /* ── Command palette ── */
  showCmdPalette  = signal(false);
  cmdQuery        = signal('');
  selectedCmdIdx  = signal(0);

  unreadCount         = signal(0);
  recentNotifications = signal<NotificacionDto[]>([]);
  loadingNotifications= signal(false);
  private notificationsSubscription?: Subscription;

  /** Esqueleto de carga (8 items mientras llegan los permisos del backend). */
  readonly skeletonItems = [1, 2, 3, 4, 5, 6, 7, 8];

  /**
   * Secciones del sidebar calculadas dinámicamente a partir del catálogo
   * filtrado por los módulos que el backend asignó al rol del usuario.
   * Se recalcula automáticamente cada vez que cambian las señales de permisos.
   */
  readonly sidebarSections = computed<ResolvedSection[]>(() => {
    const isSuperAdmin = this.authService.isSuperAdmin();
    const userRoles = this.authService.currentRoles();   // todos los roles del usuario

    return SIDEBAR_CATALOG
      .map(section => ({
        title: section.title,
        entries: section.entries
          .filter(entry => {
            if (entry.superAdminOnly) return isSuperAdmin;
            const codes = Array.isArray(entry.codigo) ? entry.codigo : [entry.codigo];
            return codes.some(code => this.permissions.canAccess(code));
          })
          .map(entry => ({
            ...entry,
            // Un hijo es visible si no requiere rol especial, o si el usuario
            // tiene AL MENOS UNO de los roles requeridos (multi-rol).
            visibleChildren: (entry.children ?? []).filter(child =>
              !child.requiresRoles || child.requiresRoles.some(r => userRoles.includes(r))
            ),
          }))
          .filter(entry => !entry.children || entry.visibleChildren.length > 0),
      }))
      .filter(section => section.entries.length > 0);
  });

  /** Lista plana de todos los módulos navegables del sidebar. */
  readonly cmdItems = computed<CmdItem[]>(() => {
    const flat: CmdItem[] = [];
    for (const section of this.sidebarSections()) {
      for (const entry of section.entries) {
        if (entry.route) {
          flat.push({ label: entry.label, route: entry.route, section: section.title, icon: entry.icon });
        }
        for (const child of entry.visibleChildren) {
          flat.push({ label: child.label, route: child.route, section: entry.label, icon: child.icon });
        }
      }
    }
    return flat;
  });

  /** Items filtrados por la búsqueda (máx 10). */
  readonly filteredCmdItems = computed<CmdItem[]>(() => {
    const q = this.cmdQuery().toLowerCase().trim();
    const all = this.cmdItems();
    if (!q) return all.slice(0, 8);
    return all.filter(item =>
      item.label.toLowerCase().includes(q) || item.section.toLowerCase().includes(q)
    ).slice(0, 10);
  });

  @HostListener('document:keydown', ['$event'])
  onKeydown(event: KeyboardEvent): void {
    if ((event.ctrlKey || event.metaKey) && event.key === 'k') {
      event.preventDefault();
      if (this.showCmdPalette()) {
        this.closeCmdPalette();
      } else {
        this.openCmdPalette();
      }
      return;
    }
    if (!this.showCmdPalette()) return;
    if (event.key === 'Escape') { this.closeCmdPalette(); return; }
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      this.selectedCmdIdx.update(i => Math.min(i + 1, this.filteredCmdItems().length - 1));
    }
    if (event.key === 'ArrowUp') {
      event.preventDefault();
      this.selectedCmdIdx.update(i => Math.max(i - 1, 0));
    }
    if (event.key === 'Enter') {
      const item = this.filteredCmdItems()[this.selectedCmdIdx()];
      if (item) this.navigateCmdItem(item);
    }
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as Node;
    if (this.showProfileMenu && this.profileRef?.nativeElement && !this.profileRef.nativeElement.contains(target)) {
      this.showProfileMenu = false;
    }
    if (this.showNotificationsMenu && this.notificationsRef?.nativeElement && !this.notificationsRef.nativeElement.contains(target)) {
      this.showNotificationsMenu = false;
    }
  }

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      // Cargar módulos del rol activo guardado (o el primario si es sesión recién iniciada)
      this.permissions.load(this.authService.rolActivo());
      this.empresaCurrent.load();
      this.loadNotifications();
      // Refrescar contador de notificaciones cada 15 s y al volver a la pestaña
      this.notificationsSubscription = interval(15_000).subscribe(() => this.loadNotifications());
      if (typeof document !== 'undefined') {
        document.addEventListener('visibilitychange', this.onVisibilityChange);
      }
      // Activar cierre de sesión automático por inactividad
      this.idleTimeout.start();
    }
  }

  private onVisibilityChange = (): void => {
    if (typeof document !== 'undefined' && document.visibilityState === 'visible' && this.authService.isAuthenticated()) {
      this.loadNotifications();
    }
  };

  ngOnDestroy(): void {
    this.notificationsSubscription?.unsubscribe();
    if (typeof document !== 'undefined') {
      document.removeEventListener('visibilitychange', this.onVisibilityChange);
    }
    this.idleTimeout.stop();
  }

  loadNotifications(): void {
    if (!this.authService.isAuthenticated()) return;
    this.loadingNotifications.set(true);
    forkJoin({
      count: this.notificacionService.countNoLeidas(),
      list:  this.notificacionService.listRecibidas(0, 5),
    }).subscribe({
      next: ({ count, list }) => {
        this.unreadCount.set(count);
        this.recentNotifications.set(list.content);
        this.loadingNotifications.set(false);
      },
      error: () => this.loadingNotifications.set(false),
    });
  }

  toggleNotificationsMenu(): void {
    this.showNotificationsMenu = !this.showNotificationsMenu;
    if (this.showNotificationsMenu) this.loadNotifications();
  }

  closeNotificationsMenu(): void { this.showNotificationsMenu = false; }

  openNotification(notif: NotificacionDto): void {
    this.notificacionService.marcarLeida(notif.id).subscribe({
      next:  () => this.loadNotifications(),
      error: () => { },
    });
    this.closeNotificationsMenu();
    this.router.navigate(['/notificaciones']);
  }

  /** Muestra el shell (sidebar, topbar) solo si está autenticado y no está en /login. Evita ver el dashboard al recargar en /login. */
  get showAppShell(): boolean {
    return this.authService.isAuthenticated() && !this.router.url.startsWith('/login');
  }

  get userInitials(): string {
    const user = this.authService.currentUser();
    if (!user?.nombreCompleto) return '?';
    const parts = user.nombreCompleto.trim().split(/\s+/);
    if (parts.length >= 2) return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    return (parts[0][0] ?? '?').toUpperCase();
  }

  toggleProfileMenu(): void { this.showProfileMenu = !this.showProfileMenu; }

  openPerfil(): void {
    this.showProfileMenu = false;
    this.perfilModal.open();
  }

  closeProfileAndLogout(): void {
    this.showProfileMenu = false;
    this.permissions.clear();
    this.idleTimeout.stop();
    this.authService.logout();
  }

  /** Cambia el rol activo y recarga los permisos exclusivamente para ese rol. */
  switchRole(rol: string): void {
    this.authService.switchRole(rol);
    this.showProfileMenu = false;
    this.permissions.load(rol);
  }

  /** Texto plano para previsualización de notificación (sin etiquetas HTML). */
  notifPreview(html: string | undefined, maxLen = 80): string {
    if (!html || typeof html !== 'string') return '';
    const div = document.createElement('div');
    div.innerHTML = html;
    const text = (div.textContent || div.innerText || '').trim().replace(/\s+/g, ' ');
    if (text.length <= maxLen) return text;
    return text.slice(0, maxLen) + '…';
  }

  openCmdPalette(): void {
    this.showCmdPalette.set(true);
    this.cmdQuery.set('');
    this.selectedCmdIdx.set(0);
    setTimeout(() => this.cmdInputRef?.nativeElement.focus(), 40);
  }

  closeCmdPalette(): void {
    this.showCmdPalette.set(false);
    this.cmdQuery.set('');
    this.selectedCmdIdx.set(0);
  }

  navigateCmdItem(item: CmdItem): void {
    this.router.navigate([item.route]);
    this.closeCmdPalette();
  }

  onCmdInput(event: Event): void {
    this.cmdQuery.set((event.target as HTMLInputElement).value);
    this.selectedCmdIdx.set(0);
  }

  /** Etiqueta legible para un rol técnico. */
  rolLabel(rol: string): string {
    const map: Record<string, string> = {
      SUPERADMINISTRADOR:  'Super Administrador',
      ADMIN:               'Administrador',
      MEDICO:              'Médico',
      COORDINADOR_MEDICO:  'Coordinador Médico',
      ODONTOLOGO:          'Odontólogo/a',
      BACTERIOLOGO:        'Bacteriólogo',
      ENFERMERO:           'Enfermero/a',
      JEFE_ENFERMERIA:     'Jefe de Enfermería',
      AUXILIAR_ENFERMERIA: 'Auxiliar de Enfermería',
      PSICOLOGO:           'Psicólogo',
      REGENTE_FARMACIA:    'Regente de Farmacia',
      RECEPCIONISTA:       'Recepcionista',
      EBS:                 'Profesional EBS',
      COORDINADOR_TERRITORIAL: 'Coordinador Territorial',
      SUPERVISOR_APS:      'Supervisor APS',
    };
    return map[rol] ?? rol;
  }
}
