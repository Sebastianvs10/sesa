/**
 * Gestión de Usuarios — soporte multi-rol, confirm dialog, toast CRUD, skeleton loading.
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy, HostListener, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faUserShield, faPlus } from '@fortawesome/free-solid-svg-icons';
import { AuthService } from '../../core/services/auth.service';
import { SesaPageHeaderComponent } from '../../shared/components/sesa-page-header/sesa-page-header.component';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';
import { SesaConfirmDialogService } from '../../shared/components/sesa-confirm-dialog/sesa-confirm-dialog.component';
import {
  PageResponse,
  ROLES_USUARIO,
  ROLES_USUARIOS_ADM_ADMIN_ONLY,
  ROLES_USUARIOS_ADM_SUPER,
  UsuarioDto,
  UsuarioRequestDto,
  UsuarioService,
} from '../../core/services/usuario.service';

@Component({
  standalone: true,
  selector: 'sesa-usuarios-page',
  imports: [CommonModule, FormsModule, FontAwesomeModule, SesaPageHeaderComponent],
  templateUrl: './usuarios.page.html',
  styleUrl: './usuarios.page.scss',
})
export class UsuariosPageComponent implements OnInit, OnDestroy {
  readonly faUserShield = faUserShield;
  readonly faPlus = faPlus;
  private readonly usuarioService = inject(UsuarioService);
  private readonly toast = inject(SesaToastService);
  private readonly confirmDialog = inject(SesaConfirmDialogService);
  readonly auth = inject(AuthService);

  /** Roles mostrados como chips en el modal (según permiso del usuario conectado). */
  readonly rolesFormOptions = computed(() =>
    this.auth.isSuperAdmin() ? ROLES_USUARIOS_ADM_SUPER : ROLES_USUARIOS_ADM_ADMIN_ONLY,
  );
  list: UsuarioDto[] = [];
  page = 0;
  size = 20;
  totalElements = 0;
  loading = false;
  error: string | null = null;
  saving = false;

  showForm = false;
  editingId: number | null = null;

  /** Roles seleccionados en el formulario (multi-rol). */
  selectedRoles = signal<string[]>([]);

  form: UsuarioRequestDto = {
    email: '',
    nombreCompleto: '',
    password: '',
    activo: true,
    roles: [],
  };

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.unlockModalScroll();
  }

  @HostListener('document:keydown.escape', ['$event'])
  onDocumentEscape(ev: Event): void {
    if (!this.showForm || this.saving) return;
    ev.preventDefault();
    this.cancel();
  }

  private lockModalScroll(): void {
    document.body.style.overflow = 'hidden';
  }

  private unlockModalScroll(): void {
    document.body.style.overflow = '';
  }

  load(): void {
    this.loading = true;
    this.error = null;
    this.usuarioService.list(this.page, this.size).subscribe({
      next: (res: PageResponse<UsuarioDto>) => {
        this.list = res.content ?? [];
        this.totalElements = res.totalElements ?? 0;
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.error || 'No se pudo cargar usuarios';
        this.loading = false;
        this.toast.error(this.error!, 'Error');
      },
    });
  }

  openCreate(): void {
    this.showForm = true;
    this.lockModalScroll();
    this.editingId = null;
    this.selectedRoles.set([]);
    this.form = { email: '', nombreCompleto: '', password: '', activo: true, roles: [] };
  }

  openEdit(u: UsuarioDto): void {
    if (!this.canEditUser(u)) {
      this.toast.error(
        'Solo un superadministrador puede editar cuentas de superadministrador.',
        'Sin permiso',
      );
      return;
    }
    this.showForm = true;
    this.lockModalScroll();
    this.editingId = u.id;
    this.selectedRoles.set([...(u.roles || [])]);
    this.form = {
      email: u.email,
      nombreCompleto: u.nombreCompleto,
      password: '',
      activo: u.activo,
      roles: [...(u.roles || [])],
    };
  }

  cancel(): void {
    this.unlockModalScroll();
    this.showForm = false;
    this.editingId = null;
  }

  /** Alterna la selección de un rol en el formulario. */
  toggleRole(rolValue: string): void {
    const current = this.selectedRoles();
    if (current.includes(rolValue)) {
      this.selectedRoles.set(current.filter(r => r !== rolValue));
    } else {
      this.selectedRoles.set([...current, rolValue]);
    }
  }

  isRoleSelected(rolValue: string): boolean {
    return this.selectedRoles().includes(rolValue);
  }

  /** ADMIN de tenant no puede editar/eliminar filas de superadministrador. */
  canEditUser(u: UsuarioDto): boolean {
    if (this.auth.isSuperAdmin()) return true;
    return !(u.roles ?? []).includes('SUPERADMINISTRADOR');
  }

  canDeleteUser(u: UsuarioDto): boolean {
    return this.canEditUser(u);
  }

  save(): void {
    const rolesSeleccionados = this.selectedRoles();
    if (!this.form.email || !this.form.nombreCompleto || rolesSeleccionados.length === 0) {
      this.error = 'Email, nombre y al menos un rol son obligatorios';
      return;
    }
    const adm = rolesSeleccionados.filter((r) => r === 'ADMIN' || r === 'SUPERADMINISTRADOR');
    if (adm.length === 0) {
      this.error = 'Seleccione al menos Administrador o Superadministrador.';
      return;
    }
    if (!this.auth.isSuperAdmin() && rolesSeleccionados.includes('SUPERADMINISTRADOR')) {
      this.error = 'No puede asignar el rol Superadministrador.';
      return;
    }
    const allowed = new Set(this.rolesFormOptions().map((r) => r.value));
    const rolesPayload = rolesSeleccionados.filter((r) => allowed.has(r));
    if (rolesPayload.length === 0) {
      this.error = 'Seleccione un rol administrativo válido.';
      return;
    }
    if (this.editingId == null && !this.form.password) {
      this.error = 'La contraseña es obligatoria al crear';
      return;
    }
    this.saving = true;
    this.error = null;
    const payload: UsuarioRequestDto = {
      email:         this.form.email.trim(),
      nombreCompleto: this.form.nombreCompleto.trim(),
      password:      this.form.password?.trim() || undefined,
      activo:        this.form.activo ?? true,
      roles:         rolesPayload,
    };
    const req = this.editingId == null
      ? this.usuarioService.create(payload)
      : this.usuarioService.update(this.editingId, payload);

    const wasEdit = this.editingId != null;
    req.subscribe({
      next: () => {
        this.saving = false;
        this.unlockModalScroll();
        this.showForm = false;
        this.editingId = null;
        this.toast.success(
          wasEdit ? 'Usuario actualizado.' : 'Usuario creado.',
          'Guardado'
        );
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || err?.error?.error || 'No se pudo guardar usuario';
        this.saving = false;
        this.toast.error(this.error!, 'Error');
      },
    });
  }

  async delete(u: UsuarioDto): Promise<void> {
    if (!this.canDeleteUser(u)) {
      this.toast.error(
        'Solo un superadministrador puede eliminar cuentas de superadministrador.',
        'Sin permiso',
      );
      return;
    }
    const ok = await this.confirmDialog.confirm({
      title: 'Eliminar usuario',
      message: `¿Eliminar el usuario "${u.email}"? Esta acción no se puede deshacer.`,
      type: 'danger',
    });
    if (!ok) return;
    this.usuarioService.delete(u.id).subscribe({
      next: () => {
        this.toast.success(`Usuario "${u.email}" eliminado.`, 'Eliminado');
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || err?.error?.error || 'No se pudo eliminar usuario';
        this.toast.error(this.error!, 'Error');
      },
    });
  }

  nextPage(): void {
    if ((this.page + 1) * this.size < this.totalElements) { this.page++; this.load(); }
  }

  prevPage(): void {
    if (this.page > 0) { this.page--; this.load(); }
  }

  userInitials(u: UsuarioDto): string {
    const parts = (u.nombreCompleto || u.email || '').trim().split(' ');
    const first = (parts[0] || '').charAt(0).toUpperCase();
    const last  = (parts[1] || '').charAt(0).toUpperCase();
    return (first + last) || '?';
  }

  avatarStyle(name: string): Record<string, string> {
    const palettes: [string, string][] = [
      ['#0d9488', '#6366f1'],
      ['#1f6ae1', '#2bb0a6'],
      ['#059669', '#10b981'],
      ['#d97706', '#f59e0b'],
      ['#dc2626', '#f87171'],
      ['#0891b2', '#38bdf8'],
      ['#0f766e', '#4f46e5'],
    ];
    const idx = (name.charCodeAt(0) || 0) % palettes.length;
    const [from, to] = palettes[idx];
    return { background: `linear-gradient(135deg, ${from}, ${to})` };
  }

  /** Devuelve la etiqueta legible de un rol por su valor (tabla y resumen). */
  rolLabel(value: string): string {
    return ROLES_USUARIO.find((r) => r.value === value)?.label ?? value;
  }

  roleClass(rol: string | undefined): string {
    const map: Record<string, string> = {
      MEDICO:              'medico',
      COORDINADOR_MEDICO:  'coordinador',
      ODONTOLOGO:          'odontologo',
      BACTERIOLOGO:        'bacteriologo',
      ENFERMERO:           'enfermero',
      JEFE_ENFERMERIA:     'jefe-enfermeria',
      AUXILIAR_ENFERMERIA: 'auxiliar',
      PSICOLOGO:           'psicologo',
      REGENTE_FARMACIA:    'farmacia',
      RECEPCIONISTA:       'recepcionista',
      ADMIN:               'admin',
      SUPERADMINISTRADOR:  'super',
    };
    return map[(rol ?? '').toUpperCase()] || 'default';
  }
}
