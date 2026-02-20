/**
 * Gestión de Notificaciones — envío individual, broadcast por rol y SUPERADMIN.
 * Autor: Ing. J Sebastian Vargas S
 */
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  faInbox,
  faPaperPlane,
  faPenToSquare,
  faPaperclip,
  faBullhorn,
} from '@fortawesome/free-solid-svg-icons';
import {
  NotificacionService,
  NotificacionDto,
  NotificacionCreateRequest,
  DestinatarioDisponible,
  NotificacionBroadcastResult,
} from '../../core/services/notificacion.service';
import { AuthService } from '../../core/services/auth.service';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';

type Tab = 'recibidas' | 'enviadas' | 'redactar';

@Component({
  standalone: true,
  selector: 'sesa-notificaciones-page',
  imports: [CommonModule, FormsModule, ReactiveFormsModule, FontAwesomeModule],
  templateUrl: './notificaciones.page.html',
  styleUrl: './notificaciones.page.scss',
})
export class NotificacionesPageComponent implements OnInit {
  private readonly notificacionService = inject(NotificacionService);
  private readonly authService         = inject(AuthService);
  private readonly fb                  = inject(FormBuilder);
  private readonly toast               = inject(SesaToastService);

  faInbox       = faInbox;
  faPaperPlane  = faPaperPlane;
  faPenToSquare = faPenToSquare;
  faPaperclip   = faPaperclip;
  faBullhorn    = faBullhorn;

  // ── Estado de tabs y listas ──────────────────────────────────────────────
  tab          = signal<Tab>('recibidas');
  recibidas    = signal<NotificacionDto[]>([]);
  enviadas     = signal<NotificacionDto[]>([]);
  destinatarios = signal<DestinatarioDisponible[]>([]);
  loading      = signal(false);
  error        = signal<string | null>(null);
  success      = signal<string | null>(null);
  selectedNotif = signal<NotificacionDto | null>(null);
  noLeidas     = signal(0);

  // ── Roles ────────────────────────────────────────────────────────────────
  isSuperAdmin = false;
  isAdmin      = false;

  // ── Formulario de redacción ───────────────────────────────────────────────
  form: FormGroup;

  /** Archivos adjuntos seleccionados */
  adjuntos: File[] = [];

  /** Modo broadcast todos (ADMIN) */
  broadcastTodos = false;

  /** Búsqueda y selección de destinatarios individuales */
  searchDestinatarios = '';
  selectedIds = new Set<number>();

  filteredDestinatarios = computed(() => {
    const q   = this.searchDestinatarios.toLowerCase();
    const all = this.destinatarios();
    if (!q) return all;
    return all.filter(u =>
      u.email?.toLowerCase().includes(q) ||
      u.nombre?.toLowerCase().includes(q)
    );
  });

  constructor() {
    this.form = this.fb.group({
      titulo:   ['', [Validators.required, Validators.maxLength(255)]],
      contenido: ['', [Validators.required]],
      tipo:     ['GENERAL'],
    });
  }

  ngOnInit(): void {
    const user       = this.authService.currentUser();
    this.isSuperAdmin = user?.role === 'SUPERADMINISTRADOR';
    this.isAdmin      = user?.role === 'ADMIN' || this.isSuperAdmin;

    this.loadRecibidas();
    this.loadNoLeidas();
    this.loadDestinatarios();
  }

  // ── Navegación ────────────────────────────────────────────────────────────

  switchTab(t: Tab): void {
    this.tab.set(t);
    this.selectedNotif.set(null);
    this.error.set(null);
    this.success.set(null);
    if (t === 'recibidas') this.loadRecibidas();
    if (t === 'enviadas')  this.loadEnviadas();
  }

  // ── Carga de datos ────────────────────────────────────────────────────────

  loadRecibidas(): void {
    this.loading.set(true);
    this.notificacionService.listRecibidas(0, 50).subscribe({
      next: (page) => { this.recibidas.set(page.content); this.loading.set(false); },
      error: (e) => {
        const msg = e.error?.error || 'Error cargando notificaciones';
        this.error.set(msg);
        this.loading.set(false);
        this.toast.error(msg, 'Error');
      },
    });
  }

  loadEnviadas(): void {
    this.loading.set(true);
    this.notificacionService.listEnviadas(0, 50).subscribe({
      next: (page) => { this.enviadas.set(page.content); this.loading.set(false); },
      error: (e) => { this.error.set(e.error?.error || 'Error'); this.loading.set(false); },
    });
  }

  loadNoLeidas(): void {
    this.notificacionService.countNoLeidas().subscribe({
      next:  (n) => this.noLeidas.set(n),
      error: ()  => {},
    });
  }

  loadDestinatarios(): void {
    this.notificacionService.getDestinatariosDisponibles().subscribe({
      next:  (list) => this.destinatarios.set(list),
      error: ()     => {},
    });
  }

  // ── Selección de destinatarios ────────────────────────────────────────────

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

  // ── Adjuntos ──────────────────────────────────────────────────────────────

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      for (let i = 0; i < input.files.length; i++) this.adjuntos.push(input.files[i]);
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

  // ── Envío normal ──────────────────────────────────────────────────────────

  send(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    if (!this.broadcastTodos && this.selectedIds.size === 0) {
      this.error.set('Seleccione al menos un destinatario o active «Enviar a todos».');
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.success.set(null);

    const req: NotificacionCreateRequest = {
      titulo:         this.form.value.titulo,
      contenido:      this.form.value.contenido,
      tipo:           this.form.value.tipo,
      broadcastTodos: this.broadcastTodos,
      destinatarioIds: this.broadcastTodos ? [] : Array.from(this.selectedIds),
    };

    this.notificacionService.create(req).subscribe({
      next: (notif) => {
        if (this.adjuntos.length > 0) this.uploadAdjuntosSecuencial(notif.id, 0);
        else this.onSendSuccess('Notificación enviada correctamente.');
      },
      error: (e) => {
        this.error.set(e.error?.error || e.message || 'Error al enviar');
        this.loading.set(false);
      },
    });
  }

  // ── Broadcast SUPERADMIN → admins ─────────────────────────────────────────

  broadcastAdmins(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }

    this.loading.set(true);
    this.error.set(null);
    this.success.set(null);

    const req: NotificacionCreateRequest = {
      titulo:    this.form.value.titulo,
      contenido: this.form.value.contenido,
      tipo:      this.form.value.tipo,
    };

    this.notificacionService.broadcastAdmins(req).subscribe({
      next: (result: NotificacionBroadcastResult) => {
        const msg = `Enviado a ${result.totalDestinatarios} administrador(es) en ${result.schemasProcessados} empresa(s).`;
        this.onSendSuccess(msg);
        if (result.errores && result.errores.length > 0) {
          this.toast.error('Algunos schemas fallaron: ' + result.errores.join(', '), 'Advertencia');
        }
      },
      error: (e) => {
        this.error.set(e.error?.error || e.message || 'Error al enviar broadcast');
        this.loading.set(false);
      },
    });
  }

  // ── Helpers privados ──────────────────────────────────────────────────────

  private uploadAdjuntosSecuencial(notifId: number, index: number): void {
    if (index >= this.adjuntos.length) {
      this.onSendSuccess('Notificación enviada correctamente.');
      return;
    }
    this.notificacionService.uploadAdjunto(notifId, this.adjuntos[index]).subscribe({
      next:  () => this.uploadAdjuntosSecuencial(notifId, index + 1),
      error: () => this.uploadAdjuntosSecuencial(notifId, index + 1),
    });
  }

  private onSendSuccess(msg: string): void {
    this.loading.set(false);
    this.success.set(msg);
    this.toast.success(msg, 'Enviada');
    this.form.reset({ tipo: 'GENERAL' });
    this.selectedIds.clear();
    this.adjuntos      = [];
    this.broadcastTodos = false;
    setTimeout(() => this.success.set(null), 5000);
  }

  // ── Detalle ───────────────────────────────────────────────────────────────

  viewNotif(notif: NotificacionDto): void {
    this.selectedNotif.set(notif);
    if (this.tab() === 'recibidas') {
      this.notificacionService.marcarLeida(notif.id).subscribe({
        next:  () => this.loadNoLeidas(),
        error: () => {},
      });
    }
  }

  closeDetail(): void { this.selectedNotif.set(null); }

  downloadFile(notifId: number, adj: { id: number; nombreArchivo: string }): void {
    this.notificacionService.downloadAdjunto(notifId, adj.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a   = document.createElement('a');
        a.href     = url;
        a.download = adj.nombreArchivo;
        a.click();
        URL.revokeObjectURL(url);
      },
    });
  }

  isImageType(ct?: string): boolean { return !!ct && ct.startsWith('image/'); }
  isVideoType(ct?: string): boolean  { return !!ct && ct.startsWith('video/'); }

  adjuntoPreviewUrl(notifId: number, adjId: number): string {
    return this.notificacionService.adjuntoUrl(notifId, adjId);
  }
}
