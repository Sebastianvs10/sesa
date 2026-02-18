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
  email: string;
  nombreCompleto: string;
  role: string;
  /** Esquema del tenant (empresa) del usuario */
  schema?: string;
  /** Nombre de la empresa (razón social) para mostrar en la UI */
  empresaNombre?: string;
}

export interface PasswordResetRequestDto {
  email: string;
}

export interface PasswordResetConfirmDto {
  token: string;
  newPassword: string;
}

const TOKEN_KEY = 'sesa_access_token';
const USER_KEY = 'sesa_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = environment.apiUrl;

  private tokenSignal = signal<string | null>(this.getStoredToken());
  private userSignal = signal<LoginResponse | null>(this.getStoredUser());

  isAuthenticated = computed(() => !!this.tokenSignal());
  currentUser = computed(() => this.userSignal());
  token = computed(() => this.tokenSignal());
  /** Schema (tenant) del usuario actual; todas las peticiones API usan este tenant vía JWT */
  currentSchema = computed(() => this.userSignal()?.schema ?? null);
  /** Indica si el usuario actual es SUPERADMINISTRADOR */
  isSuperAdmin = computed(() => this.userSignal()?.role === 'SUPERADMINISTRADOR');

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/auth/login`, credentials).pipe(
      tap((res) => {
        this.tokenSignal.set(res.accessToken);
        this.userSignal.set({ ...res, schema: res.schema ?? 'public' });
        if (res.accessToken) {
          localStorage.setItem(TOKEN_KEY, res.accessToken);
          localStorage.setItem(USER_KEY, JSON.stringify({ ...res, schema: res.schema ?? 'public' }));
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

  requestPasswordReset(dto: PasswordResetRequestDto): Observable<{ message: string; token?: string }> {
    return this.http.post<{ message: string; token?: string }>(`${this.apiUrl}/auth/password/request-reset`, dto);
  }

  resetPassword(dto: PasswordResetConfirmDto): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/auth/password/reset`, dto);
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

  logout(): void {
    this.tokenSignal.set(null);
    this.userSignal.set(null);
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
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
}
