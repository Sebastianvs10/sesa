/**
 * Bandeja de Notificaciones — UX/UI premium, cliente de correo moderno.
 * Sidebar, búsqueda, acciones masivas, panel Redactar con editor rich text y adjuntos.
 * Autor: Ing. J Sebastian Vargas S
 */
import { Component, OnInit, OnDestroy, inject, signal, computed, effect, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  faInbox,
  faPaperPlane,
  faPaperclip,
  faBullhorn,
  faSearch,
  faPlus,
  faTimes,
  faFlag,
  faEnvelopeOpen,
  faBold,
  faItalic,
  faListUl,
  faListOl,
  faLink,
  faCheckDouble,
  faArrowLeft,
  faEnvelope,
  faTrashCan,
  faSortDown,
  faSortUp,
  faFilter,
  faEllipsisVertical,
  faExpand,
  faStar,
  faArchive,
  faTriangleExclamation,
  faCircleQuestion,
} from '@fortawesome/free-solid-svg-icons';
import type { IconDefinition } from '@fortawesome/fontawesome-svg-core';
import { interval, Subscription } from 'rxjs';
import {
  NotificacionService,
  NotificacionDto,
  NotificacionCreateRequest,
  DestinatarioDisponible,
  NotificacionBroadcastResult,
  PageResponse,
} from '../../core/services/notificacion.service';
import { AuthService } from '../../core/services/auth.service';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';

type MenuId = 'inbox' | 'important' | 'sent' | 'archived' | 'trash';
type CategoryId = 'GENERAL' | 'URGENTE' | 'INFORMATIVO' | '';

interface ConfirmModalState {
  open: boolean;
  title: string;
  message: string;
  confirmLabel: string;
  danger: boolean;
  resolve: ((result: boolean) => void) | null;
}

interface CategoryFilter {
  id: CategoryId;
  label: string;
  icon: IconDefinition;
  count: number;
}

@Component({
  standalone: true,
  selector: 'sesa-notificaciones-page',
  imports: [CommonModule, FormsModule, ReactiveFormsModule, FontAwesomeModule],
  templateUrl: './notificaciones.page.html',
  styleUrl: './notificaciones.page.scss',
})
export class NotificacionesPageComponent implements OnInit, OnDestroy {
  private readonly notificacionService = inject(NotificacionService);
  private readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(SesaToastService);

  faInbox = faInbox;
  faPaperPlane = faPaperPlane;
  faPaperclip = faPaperclip;
  faBullhorn = faBullhorn;
  faSearch = faSearch;
  faPlus = faPlus;
  faTimes = faTimes;
  faFlag = faFlag;
  faEnvelopeOpen = faEnvelopeOpen;
  faBold = faBold;
  faItalic = faItalic;
  faListUl = faListUl;
  faListOl = faListOl;
  faLink = faLink;
  faCheckDouble = faCheckDouble;
  faArrowLeft = faArrowLeft;
  faEnvelope = faEnvelope;
  faTrashCan = faTrashCan;
  faSortDown = faSortDown;
  faSortUp = faSortUp;
  faFilter = faFilter;
  faEllipsisVertical = faEllipsisVertical;
  faExpand = faExpand;
  faStar = faStar;
  faArchive = faArchive;
  faTriangleExclamation = faTriangleExclamation;
  faCircleQuestion = faCircleQuestion;

  menuActive = signal<MenuId>('inbox');
  sidebarCollapsed = signal(false);
  categoryActive = signal<CategoryId>('');
  searchQuery = '';
  composeOpen = signal(false);
  composePreview = signal(false);
  /** Orden: 'fecha-desc' | 'fecha-asc' */
  sortOrder = signal<'fecha-desc' | 'fecha-asc'>('fecha-desc');
  /** Filtro por tipo en header (vacío = todos) */
  headerFilterType = signal<CategoryId>('');
  filterDropdownOpen = signal(false);
  sortDropdownOpen = signal(false);
  estadoDropdownOpen = signal(false);
  /** Filtro por estado: todos | leídas | no leídas (solo en bandeja/importantes) */
  stateFilter = signal<'all' | 'read' | 'unread'>('all');
  rowMenuOpenId = signal<number | null>(null);

  recibidas = signal<NotificacionDto[]>([]);
  archivadas = signal<NotificacionDto[]>([]);
  papelera = signal<NotificacionDto[]>([]);
  enviadas = signal<NotificacionDto[]>([]);
  totalRecibidas = signal(0);
  totalArchivadas = signal(0);
  totalPapelera = signal(0);
  totalEnviadas = signal(0);
  destinatarios = signal<DestinatarioDisponible[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  success = signal<string | null>(null);
  selectedNotif = signal<NotificacionDto | null>(null);
  noLeidas = signal(0);

  currentPage = signal(0);
  readonly pageSize = 20;

  isSuperAdmin = false;
  isAdmin = false;

  form: FormGroup;
  adjuntos: File[] = [];
  broadcastTodos = false;
  searchDestinatarios = '';
  selectedIds = new Set<number>();

  /** Selección en la lista (checkboxes) */
  selectedListIds = new Set<number>();
  readonly skeletonRows = [1, 2, 3, 4, 5, 6, 7];

  /** Si el formulario de redacción tiene contenido sin enviar (para confirmar cierre) */
  composeDirty = false;

  /** IDs de filas que están animando salida (swipe exit) */
  exitingRowIds = signal<Set<number>>(new Set());

  /** Estado del modal de confirmación custom */
  confirmModal = signal<ConfirmModalState>({
    open: false,
    title: '',
    message: '',
    confirmLabel: 'Confirmar',
    danger: false,
    resolve: null,
  });

  private readonly originalTitle = document.title;
  private currentUserId: number | null = null;
  private refreshCountSubscription?: Subscription;
  private readonly prefsKey = 'sesa.notificaciones.preferences.v1';

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.confirmModal().open) {
      this.onConfirmCancel();
      return;
    }
    if (this.selectedNotif()) {
      this.closeDetail();
    } else if (this.composeOpen()) {
      this.closeComposeWithConfirm();
    }
    this.rowMenuOpenId.set(null);
    this.closeDropdowns();
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    this.rowMenuOpenId.set(null);
  }

  filteredDestinatarios = computed(() => {
    const q = this.searchDestinatarios.toLowerCase();
    const all = this.destinatarios();
    if (!q) return all;
    return all.filter(
      (u) =>
        u.email?.toLowerCase().includes(q) ||
        u.nombre?.toLowerCase().includes(q)
    );
  });

  categoryFilters = computed((): CategoryFilter[] => {
    const rec = this.recibidas();
    const env = this.enviadas();
    const menu = this.menuActive();
    const list = menu === 'sent' ? env : rec;
    const general = list.filter((n) => n.tipo === 'GENERAL').length;
    const urgente = list.filter((n) => n.tipo === 'URGENTE').length;
    const informativo = list.filter((n) => n.tipo === 'INFORMATIVO').length;
    return [
      { id: 'GENERAL', label: 'General', icon: faInbox, count: general },
      { id: 'URGENTE', label: 'Urgente', icon: faFlag, count: urgente },
      { id: 'INFORMATIVO', label: 'Informativo', icon: faPaperPlane, count: informativo },
    ];
  });

  filteredList = computed(() => {
    const menu = this.menuActive();
    const cat = this.categoryActive();
    const headerType = this.headerFilterType();
    const q = this.searchQuery.trim().toLowerCase();
    let list: NotificacionDto[] = this.recibidas();
    if (menu === 'sent') list = this.enviadas();
    if (menu === 'archived') list = this.archivadas();
    if (menu === 'trash') list = this.papelera();
    if (menu === 'important') {
      list = list.filter((n) => n.tipo === 'URGENTE');
    }
    if (cat) {
      list = list.filter((n) => n.tipo === cat);
    }
    if (headerType) {
      list = list.filter((n) => n.tipo === headerType);
    }
    const state = this.stateFilter();
    if ((menu === 'inbox' || menu === 'important' || menu === 'archived') && state !== 'all') {
      list = list.filter((n) => (state === 'unread' ? this.isUnread(n) : !this.isUnread(n)));
    }
    if (q) {
      list = list.filter(
        (n) =>
          n.titulo?.toLowerCase().includes(q) ||
          this.stripHtml(n.contenido || '').toLowerCase().includes(q) ||
          n.remitenteNombre?.toLowerCase().includes(q)
      );
    }
    const order = this.sortOrder();
    const sorted = [...list].sort((a, b) => {
      const ta = new Date(a.fechaEnvio).getTime();
      const tb = new Date(b.fechaEnvio).getTime();
      return order === 'fecha-desc' ? tb - ta : ta - tb;
    });
    return sorted;
  });

  /** Agrupación por fecha: Hoy, Ayer, Esta semana, Anterior */
  groupedByDate = computed(() => {
    const list = this.filteredList();
    const now = new Date();
    const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const yesterdayStart = new Date(todayStart);
    yesterdayStart.setDate(yesterdayStart.getDate() - 1);
    const weekStart = new Date(todayStart);
    weekStart.setDate(weekStart.getDate() - 7);

    const groups: { label: string; key: string; items: NotificacionDto[] }[] = [];
    const hoy: NotificacionDto[] = [];
    const ayer: NotificacionDto[] = [];
    const estaSemana: NotificacionDto[] = [];
    const anterior: NotificacionDto[] = [];

    for (const n of list) {
      const d = new Date(n.fechaEnvio);
      if (d >= todayStart) hoy.push(n);
      else if (d >= yesterdayStart) ayer.push(n);
      else if (d >= weekStart) estaSemana.push(n);
      else anterior.push(n);
    }

    if (hoy.length) groups.push({ label: 'Hoy', key: 'hoy', items: hoy });
    if (ayer.length) groups.push({ label: 'Ayer', key: 'ayer', items: ayer });
    if (estaSemana.length) groups.push({ label: 'Esta semana', key: 'semana', items: estaSemana });
    if (anterior.length) groups.push({ label: 'Anteriores', key: 'anterior', items: anterior });
    return groups;
  });

  totalElements = computed(() => {
    const menu = this.menuActive();
    if (menu === 'sent') return this.totalEnviadas();
    if (menu === 'archived') return this.totalArchivadas();
    if (menu === 'trash') return this.totalPapelera();
    return this.totalRecibidas();
  });

  totalPages = computed(() => {
    const t = this.totalElements();
    const s = this.pageSize;
    return t <= 0 ? 1 : Math.ceil(t / s);
  });

  /** Números de página visibles en la paginación (ventana deslizante de hasta 7) */
  pageNumbers = computed((): number[] => {
    const total = this.totalPages();
    const current = this.currentPage();
    const pages: number[] = [];
    const max = 7;
    if (total <= max) {
      for (let i = 0; i < total; i++) pages.push(i);
      return pages;
    }
    let start = Math.max(0, current - 3);
    let end = Math.min(total - 1, start + max - 1);
    if (end - start < max - 1) start = Math.max(0, end - max + 1);
    for (let i = start; i <= end; i++) pages.push(i);
    return pages;
  });

  /** Etiqueta legible del tipo de notificación */
  tipoLabel(tipo: string): string {
    const map: Record<string, string> = { URGENTE: 'Urgente', GENERAL: 'General', INFORMATIVO: 'Informativo' };
    return map[tipo] ?? tipo;
  }

  /** KPI: total no leídas en la bandeja activa */
  unreadCurrentPage = computed(() => {
    const menu = this.menuActive();
    if (menu !== 'inbox' && menu !== 'important') return 0;
    return this.filteredList().filter((n) => this.isUnread(n)).length;
  });

  constructor() {
    this.form = this.fb.group({
      titulo: ['', [Validators.required, Validators.maxLength(255)]],
      contenido: ['', [Validators.required]],
      tipo: ['GENERAL'],
    });

    // Actualiza el título del tab del navegador con el conteo de no leídas
    effect(() => {
      const count = this.noLeidas();
      document.title = count > 0 ? `(${count}) Notificaciones — SESA` : 'Notificaciones — SESA';
    });
  }

  ngOnInit(): void {
    this.restorePreferences();
    const user = this.authService.currentUser();
    this.currentUserId = user?.userId ?? null;
    this.isSuperAdmin = user?.role === 'SUPERADMINISTRADOR';
    this.isAdmin = user?.role === 'ADMIN' || this.isSuperAdmin;

    this.loadRecibidas();
    this.loadArchivadas();
    this.loadPapelera();
    this.loadEnviadas();
    this.loadNoLeidas();
    this.loadDestinatarios();
    // Refrescar contador de no leídas cada 15 s para que el badge se actualice al recibir
    this.refreshCountSubscription = interval(15_000).subscribe(() => this.loadNoLeidas());
  }

  ngOnDestroy(): void {
    this.refreshCountSubscription?.unsubscribe();
    document.title = this.originalTitle;
  }

  setMenu(menu: MenuId): void {
    this.menuActive.set(menu);
    this.selectedNotif.set(null);
    this.error.set(null);
    this.success.set(null);
    this.currentPage.set(0);
    this.persistPreferences();
    if (menu === 'inbox' || menu === 'important') this.loadRecibidas();
    if (menu === 'archived') this.loadArchivadas();
    if (menu === 'trash') this.loadPapelera();
    if (menu === 'sent') this.loadEnviadas();
  }

  toggleSidebar(): void {
    this.sidebarCollapsed.update((v) => !v);
    this.persistPreferences();
  }

  setCategory(cat: CategoryId): void {
    this.categoryActive.set(this.categoryActive() === cat ? '' : cat);
  }

  setSortOrder(order: 'fecha-desc' | 'fecha-asc'): void {
    this.sortOrder.set(order);
    this.persistPreferences();
  }

  setHeaderFilterType(tipo: CategoryId): void {
    this.headerFilterType.set(this.headerFilterType() === tipo ? '' : tipo);
    this.filterDropdownOpen.set(false);
    this.persistPreferences();
  }

  setSortOrderAndClose(order: 'fecha-desc' | 'fecha-asc'): void {
    this.sortOrder.set(order);
    this.sortDropdownOpen.set(false);
    this.persistPreferences();
  }

  closeDropdowns(): void {
    this.filterDropdownOpen.set(false);
    this.sortDropdownOpen.set(false);
    this.estadoDropdownOpen.set(false);
  }

  toggleFilterDropdown(): void {
    this.filterDropdownOpen.update((v) => !v);
    this.sortDropdownOpen.set(false);
    this.estadoDropdownOpen.set(false);
  }

  toggleSortDropdown(): void {
    this.sortDropdownOpen.update((v) => !v);
    this.filterDropdownOpen.set(false);
    this.estadoDropdownOpen.set(false);
  }

  toggleEstadoDropdown(): void {
    this.estadoDropdownOpen.update((v) => !v);
    this.filterDropdownOpen.set(false);
    this.sortDropdownOpen.set(false);
  }

  setEstadoFilter(value: 'all' | 'read' | 'unread'): void {
    this.stateFilter.set(value);
    this.estadoDropdownOpen.set(false);
    this.persistPreferences();
  }

  onSearchChange(): void {
    this.persistPreferences();
  }

  toggleRowMenu(id: number, ev: Event): void {
    ev.stopPropagation();
    this.rowMenuOpenId.set(this.rowMenuOpenId() === id ? null : id);
  }

  runRowAction(action: 'read' | 'unread' | 'important' | 'archive' | 'delete', notif: NotificacionDto, ev: Event): void {
    ev.stopPropagation();
    this.rowMenuOpenId.set(null);
    if (action === 'read') {
      this.quickMarkAsRead(notif, ev);
      return;
    }
    if (action === 'unread') {
      this.markAsUnread(notif);
      return;
    }
    if (action === 'important') {
      this.quickMarkImportant(notif, ev);
      return;
    }
    if (action === 'archive') {
      this.toggleArchive(notif);
      return;
    }
    this.handleDeleteAction(notif);
  }

  /** Acción rápida: marcar como leída sin abrir (solo en bandeja/importantes) */
  quickMarkAsRead(notif: NotificacionDto, ev: Event): void {
    ev.stopPropagation();
    if (this.menuActive() !== 'inbox' && this.menuActive() !== 'important') return;
    if (!this.isUnread(notif)) return;
    this.notificacionService.marcarLeida(notif.id).subscribe({
      next: () => {
        this.loadRecibidas();
        this.loadNoLeidas();
        this.toast.success('Marcada como leída', 'Listo');
      },
      error: () => {},
    });
  }

  /** Acción rápida: marcar como importante (placeholder; el tipo Urgente se gestiona al crear) */
  quickMarkImportant(notif: NotificacionDto, ev: Event): void {
    ev.stopPropagation();
    this.toast.info('Próximamente podrás marcar como importante desde aquí.', 'Acción');
  }

  quickDelete(notif: NotificacionDto, ev: Event): void {
    ev.stopPropagation();
    this.handleDeleteAction(notif);
  }

  /** Abre el modal de confirmación y devuelve una promesa que resuelve true/false */
  private showConfirm(
    title: string,
    message: string,
    confirmLabel = 'Confirmar',
    danger = false
  ): Promise<boolean> {
    return new Promise((resolve) => {
      this.confirmModal.set({ open: true, title, message, confirmLabel, danger, resolve });
    });
  }

  onConfirmAccept(): void {
    const modal = this.confirmModal();
    if (modal.resolve) modal.resolve(true);
    this.confirmModal.update((m) => ({ ...m, open: false, resolve: null }));
  }

  onConfirmCancel(): void {
    const modal = this.confirmModal();
    if (modal.resolve) modal.resolve(false);
    this.confirmModal.update((m) => ({ ...m, open: false, resolve: null }));
  }

  isExiting(id: number): boolean {
    return this.exitingRowIds().has(id);
  }

  /** Anima la salida de una fila y devuelve una promesa que resuelve tras la animación */
  private animateRowExit(id: number): Promise<void> {
    return new Promise((resolve) => {
      this.exitingRowIds.update((s) => new Set([...s, id]));
      setTimeout(() => {
        this.exitingRowIds.update((s) => { const n = new Set(s); n.delete(id); return n; });
        resolve();
      }, 280);
    });
  }

  private async toggleArchive(notif: NotificacionDto): Promise<void> {
    if (this.menuActive() === 'archived') {
      await this.animateRowExit(notif.id);
      if (this.selectedNotif()?.id === notif.id) this.selectedNotif.set(null);
      this.notificacionService.desarchivar(notif.id).subscribe({
        next: () => {
          this.toast.success('Notificación movida a Recibidos', 'Listo');
          this.refreshListsAfterStateChange();
        },
        error: (e) => this.toast.error(e.error?.error || 'No se pudo desarchivar', 'Error'),
      });
      return;
    }
    await this.animateRowExit(notif.id);
    if (this.selectedNotif()?.id === notif.id) this.selectedNotif.set(null);
    this.notificacionService.archivar(notif.id).subscribe({
      next: () => {
        this.toast.success('Notificación archivada', 'Listo');
        this.refreshListsAfterStateChange();
      },
      error: (e) => this.toast.error(e.error?.error || 'No se pudo archivar', 'Error'),
    });
  }

  private async handleDeleteAction(notif: NotificacionDto): Promise<void> {
    if (this.menuActive() === 'trash') {
      const ok = await this.showConfirm(
        'Eliminar definitivamente',
        'Esta acción no se puede deshacer. La notificación se eliminará de forma permanente.',
        'Eliminar',
        true
      );
      if (!ok) return;
      await this.animateRowExit(notif.id);
      if (this.selectedNotif()?.id === notif.id) this.selectedNotif.set(null);
      this.notificacionService.eliminarDefinitivo(notif.id).subscribe({
        next: () => {
          this.toast.success('Notificación eliminada definitivamente', 'Listo');
          this.refreshListsAfterStateChange();
        },
        error: (e) => this.toast.error(e.error?.error || 'No se pudo eliminar definitivamente', 'Error'),
      });
      return;
    }
    const ok = await this.showConfirm(
      'Mover a papelera',
      '¿Mover esta notificación a la papelera? Podrás restaurarla más tarde.',
      'Mover a papelera',
      false
    );
    if (!ok) return;
    await this.animateRowExit(notif.id);
    if (this.selectedNotif()?.id === notif.id) this.selectedNotif.set(null);
    this.notificacionService.moverAPapelera(notif.id).subscribe({
      next: () => {
        this.toast.success('Notificación movida a papelera', 'Listo');
        this.refreshListsAfterStateChange();
      },
      error: (e) => this.toast.error(e.error?.error || 'No se pudo mover a papelera', 'Error'),
    });
  }

  restoreFromTrash(notif: NotificacionDto): void {
    this.notificacionService.restaurar(notif.id).subscribe({
      next: () => {
        this.toast.success('Notificación restaurada', 'Listo');
        this.refreshListsAfterStateChange();
      },
      error: (e) => this.toast.error(e.error?.error || 'No se pudo restaurar', 'Error'),
    });
  }

  private refreshListsAfterStateChange(): void {
    this.loadRecibidas();
    this.loadArchivadas();
    this.loadPapelera();
    this.loadNoLeidas();
  }

  countImportant(): number {
    return this.recibidas().filter((n) => n.tipo === 'URGENTE').length;
  }

  folderCount(folder: 'inbox' | 'important' | 'sent' | 'archived' | 'trash' | 'drafts'): number {
    if (folder === 'inbox') return this.noLeidas();
    if (folder === 'important') return this.countImportant();
    if (folder === 'sent') return this.totalEnviadas();
    if (folder === 'archived') return this.totalArchivadas();
    if (folder === 'trash') return this.totalPapelera();
    return 0;
  }

  loadRecibidas(): void {
    this.loading.set(true);
    const page = this.currentPage();
    this.notificacionService.listRecibidas(page, this.pageSize).subscribe({
      next: (p: PageResponse<NotificacionDto>) => {
        this.recibidas.set(p.content);
        this.totalRecibidas.set(p.totalElements);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(e.error?.error || 'Error cargando notificaciones');
        this.loading.set(false);
        this.toast.error(this.error()!, 'Error');
      },
    });
  }

  loadEnviadas(): void {
    this.loading.set(true);
    const page = this.currentPage();
    this.notificacionService.listEnviadas(page, this.pageSize).subscribe({
      next: (p: PageResponse<NotificacionDto>) => {
        this.enviadas.set(p.content);
        this.totalEnviadas.set(p.totalElements);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(e.error?.error || 'Error');
        this.loading.set(false);
      },
    });
  }

  loadArchivadas(): void {
    this.loading.set(true);
    const page = this.currentPage();
    this.notificacionService.listArchivadas(page, this.pageSize).subscribe({
      next: (p: PageResponse<NotificacionDto>) => {
        this.archivadas.set(p.content);
        this.totalArchivadas.set(p.totalElements);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(e.error?.error || 'Error');
        this.loading.set(false);
      },
    });
  }

  loadPapelera(): void {
    this.loading.set(true);
    const page = this.currentPage();
    this.notificacionService.listPapelera(page, this.pageSize).subscribe({
      next: (p: PageResponse<NotificacionDto>) => {
        this.papelera.set(p.content);
        this.totalPapelera.set(p.totalElements);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(e.error?.error || 'Error');
        this.loading.set(false);
      },
    });
  }

  loadNoLeidas(): void {
    this.notificacionService.countNoLeidas().subscribe({
      next: (n) => this.noLeidas.set(n),
      error: () => {},
    });
  }

  loadDestinatarios(): void {
    this.notificacionService.getDestinatariosDisponibles().subscribe({
      next: (list) => this.destinatarios.set(list),
      error: () => {},
    });
  }

  goToPage(page: number): void {
    this.currentPage.set(Math.max(0, Math.min(page, this.totalPages() - 1)));
    if (this.menuActive() === 'sent') return this.loadEnviadas();
    if (this.menuActive() === 'archived') return this.loadArchivadas();
    if (this.menuActive() === 'trash') return this.loadPapelera();
    this.loadRecibidas();
  }

  isAllSelected(): boolean {
    const list = this.filteredList();
    if (list.length === 0) return false;
    return list.every((n) => this.selectedListIds.has(n.id));
  }

  toggleSelectAll(ev: Event): void {
    const checked = (ev.target as HTMLInputElement).checked;
    const list = this.filteredList();
    if (checked) list.forEach((n) => this.selectedListIds.add(n.id));
    else list.forEach((n) => this.selectedListIds.delete(n.id));
  }

  toggleSelect(id: number, ev: Event): void {
    const checked = (ev.target as HTMLInputElement).checked;
    if (checked) this.selectedListIds.add(id);
    else this.selectedListIds.delete(id);
  }

  isListSelected(id: number): boolean {
    return this.selectedListIds.has(id);
  }

  hasSelection(): boolean {
    return this.selectedListIds.size > 0;
  }

  selectedCount(): number {
    return this.selectedListIds.size;
  }

  /** Marcar como leídas las notificaciones seleccionadas (solo en bandeja/importantes) */
  markSelectedAsRead(): void {
    const ids = Array.from(this.selectedListIds);
    if (ids.length === 0) return;
    this.notificacionService.marcarLeidas(ids).subscribe({
      next: () => {
        this.selectedListIds.clear();
        this.loadRecibidas();
        this.loadNoLeidas();
        this.toast.success(`${ids.length} notificación(es) marcada(s) como leída(s)`, 'Listo');
      },
      error: (e) => {
        this.toast.error(e.error?.error || 'Error al marcar como leídas', 'Error');
      },
    });
  }

  clearSelection(): void {
    this.selectedListIds.clear();
  }

  /** Archivar selección (placeholder si el backend no lo soporta) */
  archiveSelected(): void {
    if (this.selectedListIds.size === 0) return;
    this.toast.info('Archivado no disponible en esta versión.', 'Próximamente');
    this.clearSelection();
  }

  /** Eliminar selección (placeholder si el backend no lo soporta) */
  deleteSelected(): void {
    if (this.selectedListIds.size === 0) return;
    if (!window.confirm(`¿Eliminar ${this.selectedListIds.size} notificación(es)? Esta acción no está disponible aún.`)) return;
    this.toast.info('Eliminación masiva no disponible en esta versión.', 'Info');
    this.clearSelection();
  }

  /** Marcar como no leídas las notificaciones seleccionadas (solo en bandeja/importantes) */
  markSelectedAsUnread(): void {
    const ids = Array.from(this.selectedListIds);
    if (ids.length === 0) return;
    this.notificacionService.marcarNoLeidas(ids).subscribe({
      next: () => {
        this.selectedListIds.clear();
        this.loadRecibidas();
        this.loadNoLeidas();
        this.toast.success(`${ids.length} notificación(es) marcada(s) como no leída(s)`, 'Listo');
      },
      error: (e) => {
        this.toast.error(e.error?.error || 'Error al marcar como no leídas', 'Error');
      },
    });
  }

  /** Marcar una notificación como no leída (desde el detalle) */
  markAsUnread(notif: NotificacionDto): void {
    this.notificacionService.marcarNoLeida(notif.id).subscribe({
      next: () => {
        this.loadRecibidas();
        this.loadNoLeidas();
        this.notificacionService.get(notif.id).subscribe({
          next: (updated) => this.selectedNotif.set(updated),
          error: () => {},
        });
        this.toast.success('Marcada como no leída', 'Listo');
      },
      error: () => this.toast.error('No se pudo marcar como no leída', 'Error'),
    });
  }

  /** Indica si el usuario actual es el remitente de la notificación (puede eliminar adjuntos) */
  isRemitente(notif: NotificacionDto): boolean {
    return this.currentUserId != null && notif.remitenteId === this.currentUserId;
  }

  /** Eliminar adjunto (solo remitente). Refresca el detalle tras eliminar. */
  deleteAdjunto(notifId: number, adj: { id: number; nombreArchivo: string }): void {
    if (!confirm(`¿Eliminar el archivo "${adj.nombreArchivo}"?`)) return;
    this.notificacionService.deleteAdjunto(notifId, adj.id).subscribe({
      next: () => {
        this.notificacionService.get(notifId).subscribe({
          next: (updated) => this.selectedNotif.set(updated),
          error: () => {},
        });
        this.toast.success('Adjunto eliminado', 'Listo');
      },
      error: (e) => this.toast.error(e.error?.error || 'No se pudo eliminar el adjunto', 'Error'),
    });
  }

  /** Longitud del asunto (para contador en compose) */
  subjectLength(): number {
    return (this.form.get('titulo')?.value ?? '').length;
  }

  readonly subjectMaxLength = 255;

  openCompose(): void {
    this.composeOpen.set(true);
    this.composePreview.set(false);
    this.composeDirty = false;
    this.error.set(null);
  }

  closeCompose(): void {
    this.composeOpen.set(false);
    this.composePreview.set(false);
    this.composeDirty = false;
  }

  async closeComposeWithConfirm(): Promise<void> {
    if (this.composeDirty && (this.form.value.titulo?.trim() || this.form.value.contenido?.trim() || this.adjuntos.length > 0)) {
      const ok = await this.showConfirm(
        'Descartar borrador',
        'El mensaje no ha sido enviado. ¿Descartar los cambios?',
        'Descartar',
        false
      );
      if (ok) this.closeCompose();
    } else {
      this.closeCompose();
    }
  }

  onComposeFormChange(): void {
    this.composeDirty = true;
  }

  toggleDestinatario(id: number): void {
    if (this.selectedIds.has(id)) this.selectedIds.delete(id);
    else this.selectedIds.add(id);
  }

  isSelected(id: number): boolean {
    return this.selectedIds.has(id);
  }

  selectAll(): void {
    for (const u of this.filteredDestinatarios()) this.selectedIds.add(u.id);
  }

  deselectAll(): void {
    this.selectedIds.clear();
  }

  onBroadcastTodosChange(): void {
    if (this.broadcastTodos) this.selectedIds.clear();
  }

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      for (let i = 0; i < input.files.length; i++)
        this.adjuntos.push(input.files[i]);
      this.composeDirty = true;
    }
    input.value = '';
  }

  onDragOver(ev: DragEvent): void {
    ev.preventDefault();
    ev.stopPropagation();
  }

  onDrop(ev: DragEvent): void {
    ev.preventDefault();
    ev.stopPropagation();
    const files = ev.dataTransfer?.files;
    if (files) {
      for (let i = 0; i < files.length; i++) this.adjuntos.push(files[i]);
      this.composeDirty = true;
    }
  }

  removeAdjunto(index: number): void {
    this.adjuntos.splice(index, 1);
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }

  execCmd(cmd: string, value?: string): void {
    if (cmd === 'createLink') {
      const url = value || window.prompt('URL del enlace:', 'https://');
      if (url) document.execCommand(cmd, false, url);
    } else {
      document.execCommand(cmd, false, value ?? undefined);
    }
  }

  onEditorInput(_ev: Event): void {
    this.syncEditorToForm();
  }

  syncEditorToForm(): void {
    const el = document.querySelector('.notif-editor-content');
    if (el instanceof HTMLElement) {
      this.form.patchValue({ contenido: el.innerHTML || '' });
      this.composeDirty = true;
    }
  }

  send(): void {
    this.syncEditorToForm();
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (!this.broadcastTodos && this.selectedIds.size === 0) {
      this.error.set(
        'Seleccione al menos un destinatario o active «Enviar a todos».'
      );
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.success.set(null);

    const req: NotificacionCreateRequest = {
      titulo: this.form.value.titulo,
      contenido: this.form.value.contenido,
      tipo: this.form.value.tipo,
      broadcastTodos: this.broadcastTodos,
      destinatarioIds: this.broadcastTodos ? [] : Array.from(this.selectedIds),
    };

    this.notificacionService.create(req).subscribe({
      next: (notif) => {
        if (this.adjuntos.length > 0)
          this.uploadAdjuntosSecuencial(notif.id, 0);
        else this.onSendSuccess('Notificación enviada correctamente.');
      },
      error: (e) => {
        this.error.set(e.error?.error || e.message || 'Error al enviar');
        this.loading.set(false);
      },
    });
  }

  broadcastAdmins(): void {
    this.syncEditorToForm();
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.success.set(null);

    const req: NotificacionCreateRequest = {
      titulo: this.form.value.titulo,
      contenido: this.form.value.contenido,
      tipo: this.form.value.tipo,
    };

    this.notificacionService.broadcastAdmins(req).subscribe({
      next: (result: NotificacionBroadcastResult) => {
        const msg = `Enviado a ${result.totalDestinatarios} administrador(es) en ${result.schemasProcessados} empresa(s).`;
        this.onSendSuccess(msg);
        if (result.errores?.length) {
          this.toast.error(
            'Algunos schemas fallaron: ' + result.errores.join(', '),
            'Advertencia'
          );
        }
      },
      error: (e) => {
        this.error.set(
          e.error?.error || e.message || 'Error al enviar broadcast'
        );
        this.loading.set(false);
      },
    });
  }

  private uploadAdjuntosSecuencial(notifId: number, index: number): void {
    if (index >= this.adjuntos.length) {
      this.onSendSuccess('Notificación enviada correctamente.');
      return;
    }
    this.notificacionService
      .uploadAdjunto(notifId, this.adjuntos[index])
      .subscribe({
        next: () => this.uploadAdjuntosSecuencial(notifId, index + 1),
        error: () => this.uploadAdjuntosSecuencial(notifId, index + 1),
      });
  }

  private onSendSuccess(msg: string): void {
    this.loading.set(false);
    this.success.set(msg);
    this.toast.success(msg, 'Enviada');
    this.form.reset({ tipo: 'GENERAL' });
    this.selectedIds.clear();
    this.adjuntos = [];
    this.broadcastTodos = false;
    this.composeDirty = false;
    this.composeOpen.set(false);
    this.loadNoLeidas();
    // Refrescar bandeja de entrada y enviados tras un breve delay para que el backend persista
    setTimeout(() => {
      this.loadRecibidas();
      this.loadArchivadas();
      this.loadPapelera();
      this.loadEnviadas();
    }, 150);
    setTimeout(() => this.success.set(null), 5000);
  }

  viewNotif(notif: NotificacionDto): void {
    this.selectedNotif.set(notif);
    if (this.menuActive() === 'inbox' || this.menuActive() === 'important' || this.menuActive() === 'archived') {
      this.notificacionService.marcarLeida(notif.id).subscribe({
        next: () => this.loadNoLeidas(),
        error: () => {},
      });
    }
  }

  closeDetail(): void {
    this.selectedNotif.set(null);
  }

  isUnread(n: NotificacionDto): boolean {
    if (this.menuActive() !== 'inbox' && this.menuActive() !== 'important' && this.menuActive() !== 'archived')
      return false;
    if (this.currentUserId == null) return true;
    const d = n.destinatarios?.find((x) => x.usuarioId === this.currentUserId);
    return d ? !d.leido : true;
  }

  contentPreview(n: NotificacionDto, maxLen = 80): string {
    const text = this.stripHtml(n.contenido || '');
    if (text.length <= maxLen) return text;
    return text.slice(0, maxLen) + '…';
  }

  contentPreviewLong(n: NotificacionDto): string {
    const text = this.stripHtml(n.contenido || '');
    if (text.length <= 200) return text;
    return text.slice(0, 200) + '…';
  }

  emptyMessage(): string {
    if (this.hasFilterEffect()) return 'No hay resultados con los filtros actuales.';
    if (this.menuActive() === 'archived') return 'No tiene notificaciones archivadas.';
    if (this.menuActive() === 'trash') return 'La papelera está vacía.';
    if (this.menuActive() === 'sent') return 'No ha enviado notificaciones.';
    if (this.menuActive() === 'important')
      return 'No tiene notificaciones importantes.';
    return 'No tiene notificaciones en la bandeja de entrada.';
  }

  min(a: number, b: number): number {
    return Math.min(a, b);
  }

  getInitial(name: string): string {
    return (name || '?').charAt(0).toUpperCase();
  }

  hasFilterEffect(): boolean {
    return this.rawListCurrentMenu().length > 0 && this.filteredList().length === 0;
  }

  clearFilters(): void {
    this.headerFilterType.set('');
    this.stateFilter.set('all');
    this.searchQuery = '';
    this.sortOrder.set('fecha-desc');
    this.persistPreferences();
  }

  private stripHtml(html: string): string {
    const div = document.createElement('div');
    div.innerHTML = html;
    return (div.textContent || div.innerText || '').trim();
  }

  private rawListCurrentMenu(): NotificacionDto[] {
    const menu = this.menuActive();
    if (menu === 'sent') return this.enviadas();
    if (menu === 'archived') return this.archivadas();
    if (menu === 'trash') return this.papelera();
    if (menu === 'important') return this.recibidas().filter((n) => n.tipo === 'URGENTE');
    return this.recibidas();
  }

  downloadFile(
    notifId: number,
    adj: { id: number; nombreArchivo: string }
  ): void {
    this.notificacionService.downloadAdjunto(notifId, adj.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = adj.nombreArchivo;
        a.click();
        URL.revokeObjectURL(url);
      },
    });
  }

  isImageType(ct?: string): boolean {
    return !!ct && ct.startsWith('image/');
  }
  isVideoType(ct?: string): boolean {
    return !!ct && ct.startsWith('video/');
  }

  adjuntoPreviewUrl(notifId: number, adjId: number): string {
    return this.notificacionService.adjuntoUrl(notifId, adjId);
  }

  filePreviewUrl(file: File): string {
    return URL.createObjectURL(file);
  }

  private persistPreferences(): void {
    const payload = {
      menu: this.menuActive(),
      sortOrder: this.sortOrder(),
      typeFilter: this.headerFilterType(),
      stateFilter: this.stateFilter(),
      searchQuery: this.searchQuery,
      sidebarCollapsed: this.sidebarCollapsed(),
    };
    localStorage.setItem(this.prefsKey, JSON.stringify(payload));
  }

  private restorePreferences(): void {
    try {
      const raw = localStorage.getItem(this.prefsKey);
      if (!raw) return;
      const parsed = JSON.parse(raw) as {
        menu?: MenuId;
        sortOrder?: 'fecha-desc' | 'fecha-asc';
        typeFilter?: CategoryId;
        stateFilter?: 'all' | 'read' | 'unread';
        searchQuery?: string;
        sidebarCollapsed?: boolean;
      };
      if (parsed.menu) this.menuActive.set(parsed.menu);
      if (parsed.sortOrder) this.sortOrder.set(parsed.sortOrder);
      if (parsed.typeFilter !== undefined) this.headerFilterType.set(parsed.typeFilter);
      if (parsed.stateFilter) this.stateFilter.set(parsed.stateFilter);
      if (typeof parsed.searchQuery === 'string') this.searchQuery = parsed.searchQuery;
      if (typeof parsed.sidebarCollapsed === 'boolean') this.sidebarCollapsed.set(parsed.sidebarCollapsed);
    } catch {
      // Ignorar errores de parseo para no bloquear la vista
    }
  }
}
