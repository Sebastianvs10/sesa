/**
 * Servicio de permisos RBAC — totalmente dinámico.
 * Los módulos se cargan EXCLUSIVAMENTE desde el backend (/roles/usuario-actual).
 * No existe ninguna matriz estática de permisos en el frontend:
 * el superadministrador controla todo desde la página de roles.
 * Autor: Ing. J Sebastian Vargas S
 */

import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, Subject } from 'rxjs';
import { take } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

export type Modulo =
  | 'DASHBOARD' | 'PACIENTES' | 'HISTORIA_CLINICA' | 'LABORATORIOS' | 'IMAGENES'
  | 'URGENCIAS' | 'HOSPITALIZACION' | 'FARMACIA' | 'FACTURACION' | 'CITAS'
  | 'USUARIOS' | 'PERSONAL' | 'EMPRESAS' | 'NOTIFICACIONES' | 'ROLES'
  | 'REPORTES' | 'AGENDA' | 'EVOLUCION_ENFERMERIA' | 'CONSULTA_MEDICA' | 'ODONTOLOGIA'
  | 'EBS';

/** Mapa módulo → ruta (para guards de navegación). */
const MODULO_ROUTE: Record<string, string> = {
  DASHBOARD:            '/dashboard',
  PACIENTES:            '/pacientes',
  HISTORIA_CLINICA:     '/historia-clinica',
  LABORATORIOS:         '/laboratorios',
  IMAGENES:             '/imagenes-diagnosticas',
  URGENCIAS:            '/urgencias',
  HOSPITALIZACION:      '/hospitalizacion',
  FARMACIA:             '/farmacia',
  FACTURACION:          '/facturacion',
  CITAS:                '/citas',
  USUARIOS:             '/usuarios',
  PERSONAL:             '/personal',
  EMPRESAS:             '/empresas',
  NOTIFICACIONES:       '/notificaciones',
  ROLES:                '/roles',
  REPORTES:             '/reportes',
  AGENDA:               '/agenda',
  EVOLUCION_ENFERMERIA: '/evolucion-enfermeria',
  CONSULTA_MEDICA:      '/consulta-medica',
  ODONTOLOGIA:          '/odontologia',
  EBS:                  '/ebs',
};

@Injectable({ providedIn: 'root' })
export class PermissionsService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly apiUrl = environment.apiUrl;

  private readonly _modulos = signal<Set<string>>(new Set());
  private readonly _loaded = signal(false);
  /** Emite cuando la carga de permisos termina (éxito o error). Usado por guards al recargar. */
  private readonly _loaded$ = new Subject<void>();

  /** Módulos activos del usuario actual (provenientes de la BD). */
  readonly modulos = this._modulos.asReadonly();

  /** true cuando los módulos ya fueron cargados desde el backend. */
  readonly loaded = this._loaded.asReadonly();

  /**
   * true mientras el frontend espera la respuesta del backend.
   * El sidebar muestra un esqueleto durante este estado.
   */
  readonly isLoading = computed(
    () => this.auth.isAuthenticated() && !this.auth.isSuperAdmin() && !this._loaded()
  );

  /**
   * Carga los módulos permitidos del usuario actual desde el backend.
   * Se llama al iniciar la app, al hacer login y al cambiar de rol activo.
   *
   * @param rolActivo Rol cuyo catálogo de módulos se quiere mostrar.
   *                  Si se omite, el frontend usa el `rolActivo` guardado en AuthService.
   *                  Si el backend recibe el parámetro, devuelve solo los módulos de ese rol.
   */
  load(rolActivo?: string): void {
    if (!this.auth.isAuthenticated()) {
      this._modulos.set(new Set());
      this._loaded.set(false);
      return;
    }
    // SUPERADMINISTRADOR no necesita cargar: tiene acceso total
    if (this.auth.isSuperAdmin()) {
      this._loaded.set(true);
      return;
    }
    // Determinar qué rol enviar: el parámetro explícito o el guardado en AuthService
    const rol = rolActivo ?? this.auth.rolActivo();
    const params = rol ? `?rol=${encodeURIComponent(rol)}` : '';

    this._loaded.set(false); // resetear para mostrar skeleton durante la recarga
    this.http.get<{ modulos: string[]; rolActivo?: string }>(`${this.apiUrl}/roles/usuario-actual${params}`).subscribe({
      next: (res) => {
        this._modulos.set(new Set(res.modulos ?? []));
        this._loaded.set(true);
        this._loaded$.next();
      },
      error: () => {
        this._modulos.set(new Set());
        this._loaded.set(true);
        this._loaded$.next();
      },
    });
  }

  /**
   * Observable que emite cuando los permisos están cargados.
   * Si aún no se han cargado, dispara load() y espera. Usado por roleGuard al recargar en una ruta protegida.
   */
  whenLoaded(): Observable<void> {
    if (this._loaded()) return of(undefined);
    if (!this.auth.isAuthenticated()) return of(undefined);
    if (this.auth.isSuperAdmin()) return of(undefined);
    this.load();
    return this._loaded$.pipe(take(1));
  }

  /** Limpia los módulos al cerrar sesión. */
  clear(): void {
    this._modulos.set(new Set());
    this._loaded.set(false);
  }

  /**
   * Indica si el usuario puede acceder al módulo (por código).
   * — SUPERADMINISTRADOR: acceso total inmediato (sin consultar señal).
   * — Otros roles: basado exclusivamente en lo que devuelve el backend.
   * — Mientras carga (isLoading): devuelve false (sidebar muestra skeleton).
   */
  canAccess(modulo: Modulo | string): boolean {
    if (!this.auth.isAuthenticated()) return false;
    if (this.auth.isSuperAdmin()) return true;
    if (!this._loaded()) return false;
    return this._modulos().has(modulo);
  }

  /** Indica si el usuario puede acceder a la ruta (por path). */
  canAccessRoute(path: string): boolean {
    if (!this.auth.isAuthenticated()) return false;
    if (this.auth.isSuperAdmin()) return true;
    for (const [mod, route] of Object.entries(MODULO_ROUTE)) {
      if (path.startsWith(route) && this.canAccess(mod)) return true;
    }
    return false;
  }

  /**
   * Solo roles con capacidad clínica pueden crear historias clínicas.
   * Con multi-rol, basta que UNO de los roles del usuario esté en la lista.
   */
  canCrearHistoriaClinica(): boolean {
    const allowedRoles = ['MEDICO', 'ADMIN', 'SUPERADMINISTRADOR'];
    const userRoles = this.auth.currentRoles();
    return this.canAccess('HISTORIA_CLINICA') &&
      userRoles.some(r => allowedRoles.includes(r));
  }

  canGestionarRoles(): boolean        { return this.auth.isSuperAdmin(); }
  canAccesEvolucionEnfermeria(): boolean { return this.canAccess('EVOLUCION_ENFERMERIA'); }
  canAccesAgenda(): boolean             { return this.canAccess('AGENDA'); }
  canAccesConsultaMedica(): boolean     { return this.canAccess('CONSULTA_MEDICA'); }
  canAccesOdontologia(): boolean        { return this.canAccess('ODONTOLOGIA'); }
  canVerFacturacion(): boolean          { return this.canAccess('FACTURACION'); }
  canAccesReportes(): boolean           { return this.canAccess('REPORTES'); }
  canGestionarUsuarios(): boolean       { return this.canAccess('USUARIOS') || this.auth.isSuperAdmin(); }

  isAdminOrSuperAdmin(): boolean {
    const role = this.auth.currentUser()?.role?.toUpperCase();
    return role === 'ADMIN' || role === 'SUPERADMINISTRADOR';
  }
}
