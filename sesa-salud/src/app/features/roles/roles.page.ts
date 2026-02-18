/**
 * Gestión de roles del sistema (SUPERADMINISTRADOR).
 * Permite ver y editar los módulos por rol.
 * Autor: Ing. J Sebastian Vargas S
 */

import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RolesService, RolDto } from '../../core/services/roles.service';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';

const MODULOS_DISPONIBLES: { codigo: string; label: string }[] = [
  { codigo: 'DASHBOARD', label: 'Dashboard' },
  { codigo: 'PACIENTES', label: 'Pacientes' },
  { codigo: 'HISTORIA_CLINICA', label: 'Historia clínica' },
  { codigo: 'LABORATORIOS', label: 'Laboratorios' },
  { codigo: 'IMAGENES', label: 'Imágenes diagnósticas' },
  { codigo: 'URGENCIAS', label: 'Urgencias' },
  { codigo: 'HOSPITALIZACION', label: 'Hospitalización' },
  { codigo: 'FARMACIA', label: 'Farmacia' },
  { codigo: 'FACTURACION', label: 'Facturación' },
  { codigo: 'CITAS', label: 'Citas' },
  { codigo: 'USUARIOS', label: 'Usuarios' },
  { codigo: 'PERSONAL', label: 'Personal' },
  { codigo: 'EMPRESAS', label: 'Empresas' },
  { codigo: 'NOTIFICACIONES', label: 'Notificaciones' },
  { codigo: 'ROLES', label: 'Gestión de roles' },
];

@Component({
  standalone: true,
  selector: 'sesa-roles-page',
  imports: [CommonModule, FormsModule, SesaCardComponent],
  templateUrl: './roles.page.html',
  styleUrl: './roles.page.scss',
})
export class RolesPageComponent implements OnInit {
  private readonly rolesService = inject(RolesService);

  roles: RolDto[] = [];
  modulosDisponibles = MODULOS_DISPONIBLES;
  loading = false;
  error: string | null = null;
  editingRol: string | null = null;
  editingModulos: Set<string> = new Set();
  saving = false;
  showCreateForm = false;
  newRolCodigo = '';
  newRolNombre = '';
  creating = false;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = null;
    this.rolesService.list().subscribe({
      next: (data) => {
        this.roles = data ?? [];
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.error || 'No se pudieron cargar los roles';
        this.loading = false;
      },
    });
  }

  modulosLabels(modulos: string[]): string {
    const labels: Record<string, string> = Object.fromEntries(
      MODULOS_DISPONIBLES.map((m) => [m.codigo, m.label])
    );
    return (modulos ?? []).map((m) => labels[m] || m).join(', ') || '—';
  }

  getModuloLabel(codigo: string): string {
    return MODULOS_DISPONIBLES.find((m) => m.codigo === codigo)?.label ?? codigo;
  }

  startEdit(r: RolDto): void {
    this.editingRol = r.codigo;
    this.editingModulos = new Set(r.modulos ?? []);
  }

  cancelEdit(): void {
    this.editingRol = null;
    this.editingModulos.clear();
  }

  toggleModulo(modulo: string): void {
    if (this.editingModulos.has(modulo)) {
      this.editingModulos.delete(modulo);
    } else {
      this.editingModulos.add(modulo);
    }
    this.editingModulos = new Set(this.editingModulos);
  }

  hasModulo(modulo: string): boolean {
    return this.editingModulos.has(modulo);
  }

  save(): void {
    if (!this.editingRol) return;
    this.saving = true;
    this.error = null;
    this.rolesService.updateModulos(this.editingRol, Array.from(this.editingModulos)).subscribe({
      next: () => {
        this.load();
        this.cancelEdit();
        this.saving = false;
      },
      error: (err) => {
        this.error = err?.error?.error || 'No se pudo guardar';
        this.saving = false;
      },
    });
  }

  isEditing(r: RolDto): boolean {
    return this.editingRol === r.codigo;
  }

  openCreate(): void {
    this.showCreateForm = true;
    this.newRolCodigo = '';
    this.newRolNombre = '';
  }

  closeCreate(event?: Event): void {
    event?.preventDefault();
    this.showCreateForm = false;
    this.newRolCodigo = '';
    this.newRolNombre = '';
  }

  createRole(): void {
    const codigo = this.newRolCodigo.trim().toUpperCase().replace(/\s+/g, '_');
    const nombre = this.newRolNombre.trim();
    if (!codigo || !nombre) return;
    this.creating = true;
    this.error = null;
    this.rolesService.create(codigo, nombre).subscribe({
      next: () => {
        this.load();
        this.closeCreate();
        this.creating = false;
      },
      error: (err) => {
        this.error = err?.error?.error || 'No se pudo crear el rol';
        this.creating = false;
      },
    });
  }
}
