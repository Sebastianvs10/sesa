/**
 * Servicio de permisos RBAC (control de acceso por roles).
 * Matriz de permisos sincronizada con backend.
 * Autor: Ing. J Sebastian Vargas S
 */

import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

export type Modulo = 'DASHBOARD' | 'PACIENTES' | 'HISTORIA_CLINICA' | 'LABORATORIOS' | 'IMAGENES' | 'URGENCIAS' | 'HOSPITALIZACION' | 'FARMACIA' | 'FACTURACION' | 'CITAS' | 'USUARIOS' | 'PERSONAL' | 'EMPRESAS' | 'NOTIFICACIONES' | 'ROLES' | 'REPORTES';

const MODULO_ROUTE: Record<string, string> = {
  DASHBOARD: '/dashboard',
  PACIENTES: '/pacientes',
  HISTORIA_CLINICA: '/historia-clinica',
  LABORATORIOS: '/laboratorios',
  IMAGENES: '/imagenes-diagnosticas',
  URGENCIAS: '/urgencias',
  HOSPITALIZACION: '/hospitalizacion',
  FARMACIA: '/farmacia',
  FACTURACION: '/facturacion',
  CITAS: '/citas',
  USUARIOS: '/usuarios',
  PERSONAL: '/personal',
  EMPRESAS: '/empresas',
  NOTIFICACIONES: '/notificaciones',
  ROLES: '/roles',
  REPORTES: '/reportes',
};

/** Matriz rol -> módulos permitidos (sincronizada con backend PermissionService) */
const ROLE_MODULOS: Record<string, Set<Modulo>> = {
  SUPERADMINISTRADOR: new Set(['DASHBOARD', 'PACIENTES', 'HISTORIA_CLINICA', 'LABORATORIOS', 'IMAGENES', 'URGENCIAS', 'HOSPITALIZACION', 'FARMACIA', 'FACTURACION', 'CITAS', 'USUARIOS', 'PERSONAL', 'EMPRESAS', 'NOTIFICACIONES', 'ROLES', 'REPORTES']),
  ADMIN: new Set(['DASHBOARD', 'PACIENTES', 'HISTORIA_CLINICA', 'LABORATORIOS', 'IMAGENES', 'URGENCIAS', 'HOSPITALIZACION', 'FARMACIA', 'FACTURACION', 'CITAS', 'USUARIOS', 'PERSONAL', 'EMPRESAS', 'NOTIFICACIONES', 'REPORTES']),
  MEDICO: new Set(['DASHBOARD', 'PACIENTES', 'HISTORIA_CLINICA', 'LABORATORIOS', 'IMAGENES', 'URGENCIAS', 'HOSPITALIZACION', 'FARMACIA', 'CITAS']),
  ODONTOLOGO: new Set(['DASHBOARD', 'PACIENTES', 'HISTORIA_CLINICA', 'LABORATORIOS', 'IMAGENES', 'URGENCIAS', 'HOSPITALIZACION', 'FARMACIA', 'CITAS']),
  BACTERIOLOGO: new Set(['DASHBOARD', 'PACIENTES', 'LABORATORIOS']),
  ENFERMERO: new Set(['DASHBOARD', 'PACIENTES', 'HISTORIA_CLINICA', 'URGENCIAS', 'HOSPITALIZACION', 'CITAS']),
  JEFE_ENFERMERIA: new Set(['DASHBOARD', 'PACIENTES', 'HISTORIA_CLINICA', 'URGENCIAS', 'HOSPITALIZACION', 'CITAS']),
  AUXILIAR_ENFERMERIA: new Set(['DASHBOARD', 'PACIENTES', 'URGENCIAS', 'HOSPITALIZACION']),
  PSICOLOGO: new Set(['DASHBOARD', 'PACIENTES', 'HISTORIA_CLINICA', 'CITAS']),
  REGENTE_FARMACIA: new Set(['DASHBOARD', 'FARMACIA', 'PACIENTES']),
  RECEPCIONISTA: new Set(['DASHBOARD', 'PACIENTES', 'CITAS', 'FACTURACION']),
};

@Injectable({ providedIn: 'root' })
export class PermissionsService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly apiUrl = environment.apiUrl;

  private modulosSignal = signal<Set<string>>(new Set());
  private loadedSignal = signal(false);

  get modulos() {
    return this.modulosSignal();
  }
  get loaded() {
    return this.loadedSignal();
  }

  /** Carga los módulos permitidos desde API (para página de roles) */
  load(): void {
    if (!this.auth.isAuthenticated()) {
      this.modulosSignal.set(new Set());
      this.loadedSignal.set(true);
      return;
    }
    this.http.get<{ modulos: string[] }>(`${this.apiUrl}/roles/usuario-actual`).subscribe({
      next: (res) => {
        this.modulosSignal.set(new Set(res.modulos ?? []));
        this.loadedSignal.set(true);
      },
      error: () => {
        this.modulosSignal.set(new Set());
        this.loadedSignal.set(true);
      },
    });
  }

  /** Indica si el usuario puede acceder al módulo (por código). Usa matriz local. */
  canAccess(modulo: Modulo | string): boolean {
    if (!this.auth.isAuthenticated()) return false;
    const role = this.auth.currentUser()?.role?.toUpperCase();
    if (!role) return false;
    if (role === 'SUPERADMINISTRADOR') return true;
    const modulos = ROLE_MODULOS[role];
    return modulos?.has(modulo as Modulo) ?? false;
  }

  /** Indica si el usuario puede acceder a la ruta (por path) */
  canAccessRoute(path: string): boolean {
    if (!this.auth.isAuthenticated()) return false;
    if (this.auth.isSuperAdmin()) return true;
    for (const [mod, route] of Object.entries(MODULO_ROUTE)) {
      if (path.startsWith(route) && this.canAccess(mod)) return true;
    }
    return false;
  }

  /** Indica si el usuario puede crear historia clínica (MEDICO, ODONTOLOGO, ADMIN) */
  canCrearHistoriaClinica(): boolean {
    const role = this.auth.currentUser()?.role;
    return this.canAccess('HISTORIA_CLINICA') && ['MEDICO', 'ODONTOLOGO', 'ADMIN', 'SUPERADMINISTRADOR'].includes(role ?? '');
  }

  /** Indica si el usuario puede gestionar roles (SUPERADMINISTRADOR) */
  canGestionarRoles(): boolean {
    return this.auth.isSuperAdmin();
  }

  /** Indica si el usuario puede ver facturación */
  canVerFacturacion(): boolean {
    return this.canAccess('FACTURACION');
  }

  /** Indica si el usuario puede ver usuarios (crear/editar) */
  canGestionarUsuarios(): boolean {
    return this.canAccess('USUARIOS') || this.auth.isSuperAdmin();
  }
}
