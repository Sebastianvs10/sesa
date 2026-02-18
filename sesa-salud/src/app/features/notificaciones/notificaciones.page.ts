import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  faInbox,
  faPaperPlane,
  faPenToSquare,
  faPaperclip,
} from '@fortawesome/free-solid-svg-icons';
import { NotificacionService, NotificacionDto, NotificacionCreateRequest } from '../../core/services/notificacion.service';
import { UsuarioService, UsuarioDto } from '../../core/services/usuario.service';
import { AuthService } from '../../core/services/auth.service';

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
  private readonly usuarioService = inject(UsuarioService);
  private readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  faInbox = faInbox;
  faPaperPlane = faPaperPlane;
  faPenToSquare = faPenToSquare;
  faPaperclip = faPaperclip;

  tab = signal<Tab>('recibidas');
  recibidas = signal<NotificacionDto[]>([]);
  enviadas = signal<NotificacionDto[]>([]);
  usuarios = signal<UsuarioDto[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  success = signal<string | null>(null);
  selectedNotif = signal<NotificacionDto | null>(null);
  noLeidas = signal(0);

  /** Formulario de redacción */
  form: FormGroup;

  /** Archivos adjuntos seleccionados */
  adjuntos: File[] = [];

  /** Búsqueda de usuarios destinatarios */
  searchUsuarios = '';
  selectedIds = new Set<number>();

  filteredUsuarios = computed(() => {
    const q = this.searchUsuarios.toLowerCase();
    const all = this.usuarios();
    if (!q) return all;
    return all.filter(u =>
      u.email?.toLowerCase().includes(q) ||
      u.nombreCompleto?.toLowerCase().includes(q)
    );
  });

  constructor() {
    this.form = this.fb.group({
      titulo: ['', [Validators.required, Validators.maxLength(255)]],
      contenido: ['', [Validators.required]],
      tipo: ['GENERAL'],
    });
  }

  ngOnInit(): void {
    this.loadRecibidas();
    this.loadNoLeidas();
    this.loadUsuarios();
  }

  switchTab(t: Tab): void {
    this.tab.set(t);
    this.selectedNotif.set(null);
    this.error.set(null);
    this.success.set(null);
    if (t === 'recibidas') this.loadRecibidas();
    if (t === 'enviadas') this.loadEnviadas();
  }

  /* ========== Carga de datos ========== */

  loadRecibidas(): void {
    this.loading.set(true);
    this.notificacionService.listRecibidas(0, 50).subscribe({
      next: (page) => { this.recibidas.set(page.content); this.loading.set(false); },
      error: (e) => { this.error.set(e.error?.error || 'Error cargando notificaciones'); this.loading.set(false); },
    });
  }

  loadEnviadas(): void {
    this.loading.set(true);
    this.notificacionService.listEnviadas(0, 50).subscribe({
      next: (page) => { this.enviadas.set(page.content); this.loading.set(false); },
      error: (e) => { this.error.set(e.error?.error || 'Error cargando enviadas'); this.loading.set(false); },
    });
  }

  loadNoLeidas(): void {
    this.notificacionService.countNoLeidas().subscribe({
      next: (n) => this.noLeidas.set(n),
      error: () => {},
    });
  }

  loadUsuarios(): void {
    this.usuarioService.list(0, 200).subscribe({
      next: (page) => this.usuarios.set(page.content),
      error: () => {},
    });
  }

  /* ========== Selección de destinatarios ========== */

  toggleDestinatario(id: number): void {
    if (this.selectedIds.has(id)) this.selectedIds.delete(id);
    else this.selectedIds.add(id);
  }

  isSelected(id: number): boolean {
    return this.selectedIds.has(id);
  }

  selectAll(): void {
    for (const u of this.filteredUsuarios()) {
      this.selectedIds.add(u.id);
    }
  }

  deselectAll(): void {
    this.selectedIds.clear();
  }

  /* ========== Adjuntos ========== */

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      for (let i = 0; i < input.files.length; i++) {
        this.adjuntos.push(input.files[i]);
      }
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

  /* ========== Enviar notificación ========== */

  send(): void {
    if (this.form.invalid || this.selectedIds.size === 0) {
      this.form.markAllAsTouched();
      if (this.selectedIds.size === 0) {
        this.error.set('Seleccione al menos un destinatario.');
      }
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.success.set(null);

    const req: NotificacionCreateRequest = {
      titulo: this.form.value.titulo,
      contenido: this.form.value.contenido,
      tipo: this.form.value.tipo,
      destinatarioIds: Array.from(this.selectedIds),
    };

    this.notificacionService.create(req).subscribe({
      next: (notif) => {
        if (this.adjuntos.length > 0) {
          this.uploadAdjuntosSecuencial(notif.id, 0);
        } else {
          this.onSendSuccess();
        }
      },
      error: (e) => {
        this.error.set(e.error?.error || e.message || 'Error al enviar');
        this.loading.set(false);
      },
    });
  }

  private uploadAdjuntosSecuencial(notifId: number, index: number): void {
    if (index >= this.adjuntos.length) {
      this.onSendSuccess();
      return;
    }
    this.notificacionService.uploadAdjunto(notifId, this.adjuntos[index]).subscribe({
      next: () => this.uploadAdjuntosSecuencial(notifId, index + 1),
      error: () => this.uploadAdjuntosSecuencial(notifId, index + 1),
    });
  }

  private onSendSuccess(): void {
    this.loading.set(false);
    this.success.set('Notificación enviada correctamente.');
    this.form.reset({ tipo: 'GENERAL' });
    this.selectedIds.clear();
    this.adjuntos = [];
    setTimeout(() => this.success.set(null), 5000);
  }

  /* ========== Ver detalle ========== */

  viewNotif(notif: NotificacionDto): void {
    this.selectedNotif.set(notif);
    if (this.tab() === 'recibidas') {
      this.notificacionService.marcarLeida(notif.id).subscribe({
        next: () => this.loadNoLeidas(),
        error: () => {},
      });
    }
  }

  closeDetail(): void {
    this.selectedNotif.set(null);
  }

  downloadFile(notifId: number, adj: { id: number; nombreArchivo: string }): void {
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
}
