import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, of, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresInMs: number;
  userId: number;
  /** ID del registro Personal vinculado al usuario (para filtrar citas, consultas, etc.) */
  personalId?: number;
  email: string;
  nombreCompleto: string;
  /** Rol primario (SUPERADMINISTRADOR > ADMIN > primero) — retrocompatibilidad. */
  role: string;
  /** Todos los roles asignados al usuario. */
  roles?: string[];
  /**
   * Rol activo seleccionado en la sesión actual. El usuario puede cambiar entre sus roles
   * sin necesidad de reautenticarse; se persiste en localStorage.
   */
  rolActivo?: string;
  /** Esquema del tenant (empresa) del usuario */
  schema?: string;
  /** Nombre de la empresa (razón social) para mostrar en la UI */
  empresaNombre?: string;
  /**
   * UUID del logo de la empresa (si existe). El frontend construye la URL directa:
   * `${apiUrl}/archivos/{empresaLogoUuid}` sin necesitar un GET extra.
   */
  empresaLogoUuid?: string;
}

const TOKEN_KEY       = 'sesa_access_token';
const USER_KEY        = 'sesa_user';
const ROL_ACTIVO_KEY  = 'sesa_rol_activo';
/**
 * Clave independiente para el UUID del logo de empresa.
 * NO se borra al hacer logout, de modo que el SUPERADMINISTRADOR
 * sigue viendo el logo tras re-autenticarse.
 */
const LOGO_PERSIST_KEY = 'sesa_logo_persist';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = environment.apiUrl;

  private tokenSignal     = signal<string | null>(this.getStoredToken());
  private userSignal      = signal<LoginResponse | null>(this.getStoredUser());
  private rolActivoSignal = signal<string | null>(this.getStoredRolActivo());

  /**
   * Decodifica el claim `exp` del JWT (sin librería) y verifica si ya expiró.
   * El token se divide en tres partes base64url; la segunda es el payload con los claims.
   */
  isTokenExpired(token: string): boolean {
    try {
      const payloadB64 = token.split('.')[1];
      if (!payloadB64) return true;
      // base64url → base64 estándar
      const json = atob(payloadB64.replace(/-/g, '+').replace(/_/g, '/'));
      const payload = JSON.parse(json) as { exp?: number };
      if (!payload?.exp) return false;
      // exp está en segundos; Date.now() en milisegundos
      return Date.now() >= payload.exp * 1000;
    } catch {
      return true;
    }
  }

  /**
   * Verdadero solo si existe un token Y ese token no ha expirado.
   * Así, un JWT caducado en localStorage no pasa los guards.
   */
  isAuthenticated = computed(() => {
    const t = this.tokenSignal();
    if (!t) return false;
    return !this.isTokenExpired(t);
  });

  currentUser    = computed(() => this.userSignal());
  token          = computed(() => this.tokenSignal());
  /** Schema (tenant) del usuario actual; todas las peticiones API usan este tenant vía JWT */
  currentSchema  = computed(() => this.userSignal()?.schema ?? null);

  /**
   * Todos los roles del usuario actual.
   * Usa el array `roles` del backend (multi-rol) con fallback al campo `role` antiguo.
   */
  currentRoles = computed<string[]>(() => {
    const u = this.userSignal();
    if (!u) return [];
    if (u.roles && u.roles.length > 0) return u.roles;
    return u.role ? [u.role] : [];
  });

  /**
   * Rol activo en la sesión actual. El usuario puede cambiarlo entre sus roles asignados
   * sin reautenticarse. Determina la vista del menú lateral y el modo de trabajo.
   */
  rolActivo = computed<string>(() => {
    const stored = this.rolActivoSignal();
    if (stored && this.currentRoles().includes(stored)) return stored;
    const u = this.userSignal();
    return u?.rolActivo ?? u?.role ?? (this.currentRoles()[0] ?? '');
  });

  /** Indica si el usuario actual tiene el rol SUPERADMINISTRADOR. */
  isSuperAdmin = computed(() => this.currentRoles().includes('SUPERADMINISTRADOR'));

  constructor(
    private http: HttpClient,
    private router: Router
  ) {
    // Si el token guardado ya expiró al abrir la app, limpiar la sesión inmediatamente
    // para que los guards redirijan al login en lugar de esperar el primer 401 del API.
    const stored = this.tokenSignal();
    if (stored && this.isTokenExpired(stored)) {
      this.clearStoredSession();
    }
  }

  /**
   * Actualiza el campo `empresaLogoUuid` en el usuario almacenado en memoria y localStorage.
   *
   * `LOGO_PERSIST_KEY` solo se escribe cuando el usuario es SUPERADMINISTRADOR (schema "public").
   * Los usuarios ADMIN tienen su logo garantizado por el login response + getCurrent();
   * si escribieran en esta clave contaminarían la sesión posterior del SUPERADMINISTRADOR.
   */
  patchUserLogoUuid(uuid: string): void {
    const current = this.userSignal();
    if (current) {
      const updated = { ...current, empresaLogoUuid: uuid };
      this.userSignal.set(updated);
      localStorage.setItem(USER_KEY, JSON.stringify(updated));
      // Solo persistir la clave global si es SUPERADMINISTRADOR
      if ((current.schema ?? 'public') === 'public') {
        localStorage.setItem(LOGO_PERSIST_KEY, uuid);
      }
    }
  }

  /** UUID del logo del SUPERADMINISTRADOR guardado de forma persistente (sobrevive logout/login). */
  getPersistedLogoUuid(): string | null {
    return localStorage.getItem(LOGO_PERSIST_KEY);
  }

  /** Limpia el estado en memoria y en localStorage sin navegar. */
  private clearStoredSession(): void {
    this.tokenSignal.set(null);
    this.userSignal.set(null);
    this.rolActivoSignal.set(null);
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem(ROL_ACTIVO_KEY);
  }

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/auth/login`, credentials).pipe(
      tap((res) => {
        const user = { ...res, schema: res.schema ?? 'public', rolActivo: res.rolActivo ?? res.role };
        this.tokenSignal.set(res.accessToken);
        this.userSignal.set(user);
        this.rolActivoSignal.set(user.rolActivo ?? null);
        if (res.accessToken) {
          localStorage.setItem(TOKEN_KEY, res.accessToken);
          localStorage.setItem(USER_KEY, JSON.stringify(user));
          localStorage.setItem(ROL_ACTIVO_KEY, user.rolActivo ?? '');
        }
      }),
      catchError((err) => {
        // Loguear el error completo en consola para debugging
        console.error('Error en login:', err);
        // Relanzar el error para que lo maneje el componente
        return throwError(() => new Error(
          this.getErrorMessage(err) || 'Error de autenticación'
        ));
      })
    );
  }

  private getErrorMessage(error: any): string {
    if (!error) {
      return 'Error desconocido';
    }

    // Error HTTP
    if (error.status === 401 || error.status === 403) {
      return 'Correo o contraseña incorrectos.';
    }

    if (error.status === 0) {
      return 'No se pudo conectar al servidor. Verifica que esté en ejecución.';
    }

    // Render (plan free): el servicio duerme; el proxy puede responder 504 si el arranque supera el tiempo límite.
    if (error.status === 504) {
      return 'El servidor tardó demasiado en responder (p. ej. servicio en frío). Espera 1 minuto, recarga y vuelve a intentar.';
    }

    if (error.status >= 500) {
      return 'Error en el servidor. Por favor, intenta más tarde.';
    }

    // Error con mensaje del servidor
    if (error.error?.error) {
      return error.error.error;
    }

    if (error.error?.message) {
      return error.error.message;
    }

    // Error genérico
    return 'Error de conexión. Verifica tu conexión a internet.';
  }

  /**
   * Cambia el rol activo en la sesión actual sin necesidad de reautenticarse.
   * Solo se puede cambiar a roles que el usuario tenga asignados.
   */
  switchRole(rol: string): void {
    const norm = (r: string) => r.toUpperCase().replace(/^ROLE_/, '');
    const target = norm(rol);
    const match = this.currentRoles().find((r) => norm(r) === target);
    if (!match) return;
    this.rolActivoSignal.set(match);
    localStorage.setItem(ROL_ACTIVO_KEY, match);
    const current = this.userSignal();
    if (current) {
      const updated = { ...current, rolActivo: match };
      this.userSignal.set(updated);
      localStorage.setItem(USER_KEY, JSON.stringify(updated));
    }
  }

  logout(): void {
    this.clearStoredSession();
    this.router.navigate(['/login']);
  }

  getStoredToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  private getStoredUser(): LoginResponse | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
      const parsed = JSON.parse(raw) as LoginResponse;
      if (parsed && !parsed.schema) parsed.schema = 'public';
      return parsed;
    } catch {
      return null;
    }
  }

  private getStoredRolActivo(): string | null {
    return localStorage.getItem(ROL_ACTIVO_KEY);
  }
}
