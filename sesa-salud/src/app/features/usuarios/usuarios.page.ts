import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
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
  imports: [CommonModule, FormsModule, SesaCardComponent],
  templateUrl: './usuarios.page.html',
  styleUrl: './usuarios.page.scss',
})
export class UsuariosPageComponent implements OnInit {
  private readonly usuarioService = inject(UsuarioService);

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
  selectedRol = '';
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
      },
    });
  }

  openCreate(): void {
    this.showForm = true;
    this.editingId = null;
    this.selectedRol = '';
    this.form = { email: '', nombreCompleto: '', password: '', activo: true, roles: [] };
  }

  openEdit(u: UsuarioDto): void {
    this.showForm = true;
    this.editingId = u.id;
    this.selectedRol = u.roles?.[0] || '';
    this.form = {
      email: u.email,
      nombreCompleto: u.nombreCompleto,
      password: '',
      activo: u.activo,
      roles: u.roles || [],
    };
  }

  cancel(): void {
    this.showForm = false;
    this.editingId = null;
  }

  save(): void {
    if (!this.form.email || !this.form.nombreCompleto || !this.selectedRol) {
      this.error = 'Email, nombre y rol son obligatorios';
      return;
    }
    if (this.editingId == null && !this.form.password) {
      this.error = 'La contraseña es obligatoria al crear';
      return;
    }
    this.saving = true;
    this.error = null;
    const payload: UsuarioRequestDto = {
      email: this.form.email.trim(),
      nombreCompleto: this.form.nombreCompleto.trim(),
      password: this.form.password?.trim() || undefined,
      activo: this.form.activo ?? true,
      roles: [this.selectedRol],
    };
    const req = this.editingId == null
      ? this.usuarioService.create(payload)
      : this.usuarioService.update(this.editingId, payload);

    req.subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.editingId = null;
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.error || 'No se pudo guardar usuario';
        this.saving = false;
      },
    });
  }

  delete(u: UsuarioDto): void {
    if (!confirm(`¿Eliminar usuario ${u.email}?`)) return;
    this.usuarioService.delete(u.id).subscribe({
      next: () => this.load(),
      error: (err) => this.error = err?.error?.error || 'No se pudo eliminar usuario',
    });
  }

  nextPage(): void {
    if ((this.page + 1) * this.size < this.totalElements) {
      this.page++;
      this.load();
    }
  }

  prevPage(): void {
    if (this.page > 0) {
      this.page--;
      this.load();
    }
  }
}
