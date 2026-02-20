import { CommonModule } from '@angular/common';
import { Component, HostListener, inject, OnInit, OnDestroy, ViewChild, ElementRef, signal } from '@angular/core';
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
} from '@fortawesome/free-solid-svg-icons';
import { AuthService } from './core/services/auth.service';
import { EmpresaCurrentService } from './core/services/empresa-current.service';
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

  /* Font Awesome icons (sidebar y topbar) */
  faChartLine = faChartLine;
  faUsers = faUsers;
  faClipboardList = faClipboardList;
  faMagnifyingGlass = faMagnifyingGlass;
  faPlus = faPlus;
  faFlask = faFlask;
  faImage = faImage;
  faTruckMedical = faTruckMedical;
  faBed = faBed;
  faPills = faPills;
  faFileInvoice = faFileInvoice;
  faCalendarDays  = faCalendarDays;
  faCalendarCheck = faCalendarCheck;
  faHeartPulse    = faHeartPulse;
  faBuilding = faBuilding;
  faBuildingColumns = faBuildingColumns;
  faShieldHalved = faShieldHalved;
  faBell = faBell;
  faChartBar = faChartBar;
  faSun = faSun;
  faMoon = faMoon;

  authService = inject(AuthService);
  perfilModal = inject(SesaPerfilModalService);
  themeService = inject(ThemeService);
  permissions = inject(PermissionsService);
  empresaCurrent = inject(EmpresaCurrentService);
  connectionService = inject(ConnectionService);
  notificacionService = inject(NotificacionService);
  private router = inject(Router);

  isSuperAdmin = this.authService.isSuperAdmin;

  unreadCount = signal(0);
  recentNotifications = signal<NotificacionDto[]>([]);
  loadingNotifications = signal(false);
  private notificationsSubscription?: Subscription;

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
      this.empresaCurrent.load();
      this.loadNotifications();
      this.notificationsSubscription = interval(30_000).subscribe(() => this.loadNotifications());
    }
  }

  ngOnDestroy(): void {
    this.notificationsSubscription?.unsubscribe();
  }

  loadNotifications(): void {
    if (!this.authService.isAuthenticated()) return;
    this.loadingNotifications.set(true);
    forkJoin({
      count: this.notificacionService.countNoLeidas(),
      list: this.notificacionService.listRecibidas(0, 5),
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
    if (this.showNotificationsMenu) {
      this.loadNotifications();
    }
  }

  closeNotificationsMenu(): void {
    this.showNotificationsMenu = false;
  }

  openNotification(notif: NotificacionDto): void {
    this.notificacionService.marcarLeida(notif.id).subscribe({
      next: () => this.loadNotifications(),
      error: () => { },
    });
    this.closeNotificationsMenu();
    this.router.navigate(['/notificaciones']);
  }

  get isLoginRoute(): boolean {
    return this.router.url.startsWith('/login');
  }

  get userInitials(): string {
    const user = this.authService.currentUser();
    if (!user?.nombreCompleto) return '?';
    const parts = user.nombreCompleto.trim().split(/\s+/);
    if (parts.length >= 2) return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    return (parts[0][0] ?? '?').toUpperCase();
  }

  isMedico(): boolean {
    return this.permissions.canCrearHistoriaClinica();
  }

  toggleProfileMenu(): void {
    this.showProfileMenu = !this.showProfileMenu;
  }

  openPerfil(): void {
    this.showProfileMenu = false;
    this.perfilModal.open();
  }

  closeProfileAndLogout(): void {
    this.showProfileMenu = false;
    this.authService.logout();
  }
}
