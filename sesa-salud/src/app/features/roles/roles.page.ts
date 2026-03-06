/**
 * Gestión de roles del sistema (SUPERADMINISTRADOR).
 * Permite ver y editar los módulos por rol.
 * Autor: Ing. J Sebastian Vargas S
 */

import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RolesService, RolDto } from '../../core/services/roles.service';
import { PermissionsService } from '../../core/services/permissions.service';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';

export interface ModuloGrupo {
  grupo: string;
  icon: string;
  items: { codigo: string; label: string; icon: string }[];
}

export const MODULOS_GRUPOS: ModuloGrupo[] = [
  {
    grupo: 'Atención clínica',
    icon: '🏥',
    items: [
      { codigo: 'DASHBOARD',            label: 'Dashboard',               icon: '📊' },
      { codigo: 'PACIENTES',            label: 'Pacientes',               icon: '👥' },
      { codigo: 'HISTORIA_CLINICA',     label: 'Historia clínica',        icon: '📋' },
      { codigo: 'ODONTOLOGIA',          label: 'Odontología',             icon: '🦷' },
      { codigo: 'EVOLUCION_ENFERMERIA', label: 'Evolución de Enfermería', icon: '💉' },
      { codigo: 'URGENCIAS',            label: 'Urgencias',               icon: '🚑' },
      { codigo: 'HOSPITALIZACION',      label: 'Hospitalización',         icon: '🛏️' },
      { codigo: 'EBS',                  label: 'Equipos Básicos de Salud', icon: '🌱' },
    ],
  },
  {
    grupo: 'Diagnóstico y tratamiento',
    icon: '🔬',
    items: [
      { codigo: 'LABORATORIOS',  label: 'Laboratorios',           icon: '🧪' },
      { codigo: 'IMAGENES',      label: 'Imágenes diagnósticas',  icon: '🩻' },
      { codigo: 'FARMACIA',      label: 'Farmacia',               icon: '💊' },
    ],
  },
  {
    grupo: 'Programación y agenda',
    icon: '📅',
    items: [
      { codigo: 'CITAS',           label: 'Citas',            icon: '🗓️' },
      { codigo: 'AGENDA',          label: 'Agenda',            icon: '✅' },
      { codigo: 'CONSULTA_MEDICA', label: 'Consulta Médica',   icon: '🩺' },
    ],
  },
  {
    grupo: 'Gestión y administración',
    icon: '⚙️',
    items: [
      { codigo: 'FACTURACION',    label: 'Facturación',       icon: '🧾' },
      { codigo: 'REPORTES',       label: 'Reportes',          icon: '📈' },
      { codigo: 'USUARIOS',       label: 'Usuarios',          icon: '👤' },
      { codigo: 'PERSONAL',       label: 'Personal',          icon: '🪪' },
      { codigo: 'EMPRESAS',       label: 'Empresas',          icon: '🏢' },
      { codigo: 'NOTIFICACIONES', label: 'Notificaciones',    icon: '🔔' },
      { codigo: 'ROLES',          label: 'Gestión de roles',  icon: '🛡️' },
    ],
  },
];

// Lista plana para compatibilidad con lógica existente
const MODULOS_DISPONIBLES = MODULOS_GRUPOS.flatMap((g) => g.items.map((i) => ({ codigo: i.codigo, label: i.label })));

export interface RolMeta {
  icon: string;
  color: string;
  descripcion: string;
  categoria: string;
}

export const ROL_META: Record<string, RolMeta> = {
  SUPERADMINISTRADOR:  { icon: '👑', color: '#7c3aed', descripcion: 'Acceso total al sistema, configuración y superusuario.', categoria: 'Sistema' },
  ADMIN:               { icon: '🛡️', color: '#1f6ae1', descripcion: 'Administrador con acceso completo excepto gestión de roles.', categoria: 'Sistema' },
  COORDINADOR_MEDICO:  { icon: '🩺', color: '#0891b2', descripcion: 'Supervisión clínica: historias, laboratorios, citas, reportes y agenda.', categoria: 'Clínico' },
  MEDICO:              { icon: '👨‍⚕️', color: '#16a34a', descripcion: 'Atención médica completa: historia clínica, urgencias, hospitalización.', categoria: 'Clínico' },
  EBS:                 { icon: '🌱', color: '#059669', descripcion: 'Equipos Básicos de Salud: visitas domiciliarias, APS territorial.', categoria: 'APS' },
  COORDINADOR_TERRITORIAL: { icon: '🗺️', color: '#0d9488', descripcion: 'Asignación de microterritorios a equipos EBS y gestión territorial.', categoria: 'APS' },
  SUPERVISOR_APS:      { icon: '📊', color: '#047857', descripcion: 'Supervisión de Atención Primaria: dashboards y reportes EBS.', categoria: 'APS' },
  ODONTOLOGO:          { icon: '🦷', color: '#2563eb', descripcion: 'Atención odontológica: historia clínica, laboratorios e imágenes.', categoria: 'Clínico' },
  BACTERIOLOGO:        { icon: '🔬', color: '#d97706', descripcion: 'Procesamiento y resultado de exámenes de laboratorio.', categoria: 'Clínico' },
  ENFERMERO:           { icon: '💉', color: '#db2777', descripcion: 'Cuidado y seguimiento de pacientes en urgencias y hospitalización.', categoria: 'Enfermería' },
  JEFE_ENFERMERIA:     { icon: '🏥', color: '#e11d48', descripcion: 'Supervisión del equipo de enfermería: evoluciones, historia clínica y agenda.', categoria: 'Enfermería' },
  AUXILIAR_ENFERMERIA: { icon: '🩹', color: '#f43f5e', descripcion: 'Apoyo en urgencias y hospitalización bajo supervisión.', categoria: 'Enfermería' },
  PSICOLOGO:           { icon: '🧠', color: '#7c3aed', descripcion: 'Atención psicológica: historia clínica y citas.', categoria: 'Clínico' },
  REGENTE_FARMACIA:    { icon: '💊', color: '#059669', descripcion: 'Dispensación y gestión del inventario farmacéutico.', categoria: 'Farmacia' },
  RECEPCIONISTA:       { icon: '📞', color: '#0284c7', descripcion: 'Registro de pacientes, agendamiento de citas y facturación básica.', categoria: 'Administrativo' },
};

@Component({
  standalone: true,
  selector: 'sesa-roles-page',
  imports: [CommonModule, FormsModule, SesaCardComponent],
  templateUrl: './roles.page.html',
  styleUrl: './roles.page.scss',
})
export class RolesPageComponent implements OnInit {
  private readonly rolesService = inject(RolesService);
  private readonly permissionsService = inject(PermissionsService);

  roles: RolDto[] = [];
  modulosDisponibles = MODULOS_DISPONIBLES;
  modulosGrupos = MODULOS_GRUPOS;
  rolMeta = ROL_META;
  loading = false;
  error: string | null = null;
  editingRol: string | null = null;
  editingModulos: Set<string> = new Set();
  saving = false;
  showCreateForm = false;
  newRolCodigo = '';
  newRolNombre = '';
  creating = false;

  getRolMeta(codigo: string): RolMeta {
    return this.rolMeta[codigo] ?? { icon: '👤', color: '#64748b', descripcion: 'Rol personalizado del sistema.', categoria: 'Personalizado' };
  }

  getCategorias(): string[] {
    return [...new Set(this.roles.map((r) => this.getRolMeta(r.codigo).categoria))];
  }

  getRolesByCategoria(cat: string): RolDto[] {
    return this.roles.filter((r) => this.getRolMeta(r.codigo).categoria === cat);
  }

  getModuloIcon(codigo: string): string {
    for (const g of MODULOS_GRUPOS) {
      const found = g.items.find((i) => i.codigo === codigo);
      if (found) return found.icon;
    }
    return '📦';
  }

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
        // Refrescar permisos del sidebar para reflejar los cambios en tiempo real
        this.permissionsService.load();
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
