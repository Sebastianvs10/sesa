/**
 * Gestión de Usuarios — soporte multi-rol, confirm dialog, toast CRUD, skeleton loading.
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faUserShield, faPlus } from '@fortawesome/free-solid-svg-icons';
import { SesaPageHeaderComponent } from '../../shared/components/sesa-page-header/sesa-page-header.component';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';
import { SesaConfirmDialogService } from '../../shared/components/sesa-confirm-dialog/sesa-confirm-dialog.component';
import {
  PageResponse,
  ROLES_USUARIO,
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
export class UsuariosPageComponent implements OnInit {
  readonly faUserShield = faUserShield;
  readonly faPlus = faPlus;
  private readonly usuarioService = inject(UsuarioService);
  private readonly toast = inject(SesaToastService);
  private readonly confirmDialog = inject(SesaConfirmDialogService);

  roles = ROLES_USUARIO;
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
    this.editingId = null;
    this.selectedRoles.set([]);
    this.form = { email: '', nombreCompleto: '', password: '', activo: true, roles: [] };
  }

  openEdit(u: UsuarioDto): void {
    this.showForm = true;
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

  save(): void {
    const rolesSeleccionados = this.selectedRoles();
    if (!this.form.email || !this.form.nombreCompleto || rolesSeleccionados.length === 0) {
      this.error = 'Email, nombre y al menos un rol son obligatorios';
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
      roles:         rolesSeleccionados,
    };
    const req = this.editingId == null
      ? this.usuarioService.create(payload)
      : this.usuarioService.update(this.editingId, payload);

    req.subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.editingId = null;
        this.toast.success(
          this.editingId != null ? 'Usuario actualizado.' : 'Usuario creado.',
          'Guardado'
        );
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.error || 'No se pudo guardar usuario';
        this.saving = false;
        this.toast.error(this.error!, 'Error');
      },
    });
  }

  async delete(u: UsuarioDto): Promise<void> {
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
        this.error = err?.error?.error || 'No se pudo eliminar usuario';
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
      ['#7c3aed', '#a855f7'],
      ['#1f6ae1', '#2bb0a6'],
      ['#059669', '#10b981'],
      ['#d97706', '#f59e0b'],
      ['#dc2626', '#f87171'],
      ['#0891b2', '#38bdf8'],
      ['#db2777', '#f472b6'],
    ];
    const idx = (name.charCodeAt(0) || 0) % palettes.length;
    const [from, to] = palettes[idx];
    return { background: `linear-gradient(135deg, ${from}, ${to})` };
  }

  /** Devuelve la etiqueta legible de un rol por su valor. */
  rolLabel(value: string): string {
    return this.roles.find(r => r.value === value)?.label ?? value;
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
