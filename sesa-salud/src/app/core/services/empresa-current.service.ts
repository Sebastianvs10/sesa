import { Injectable, inject, signal, computed } from '@angular/core';
import { AuthService } from './auth.service';
import { EmpresaService } from './empresa.service';
import { catchError, map, of, switchMap } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface CurrentEmpresa {
  nombre: string;
  logoUrl: string | null;
}

/** Regex para detectar si un string tiene formato UUID v4. */
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Injectable({ providedIn: 'root' })
export class EmpresaCurrentService {
  private readonly auth = inject(AuthService);
  private readonly empresaService = inject(EmpresaService);

  /**
   * Blob URLs creadas con URL.createObjectURL necesitan revocarse.
   * Las URLs directas (/archivos/{uuid}) no necesitan gestión de memoria.
   */
  private prevBlobUrl: string | undefined;

  private readonly state = signal<CurrentEmpresa>({ nombre: '', logoUrl: null });

  readonly currentEmpresa = this.state.asReadonly();

  readonly displayName = computed(() => {
    const n = this.state().nombre?.trim();
    return n || 'SESA Salud';
  });

  readonly hasLogo = computed(() => !!this.state().logoUrl);

  /**
   * Carga la empresa del tenant actual: nombre y logo.
   *
   * Estrategia por rol:
   *
   * ADMIN (schema propio):
   *   1. Aplicar inmediatamente el UUID del login response (sin latencia).
   *   2. Llamar a `GET /empresas/current` para refrescar siempre desde BD.
   *
   * SUPERADMINISTRADOR (schema "public"):
   *   1. Aplicar inmediatamente UUID del login response (ya viene de BD tras fix backend).
   *   2. Fallback: UUID persistido en localStorage (para retrocompatibilidad).
   *   3. Llamar a `GET /empresas` para refrescar desde BD en paralelo.
   */
  load(): void {
    const user = this.auth.currentUser();
    const schema = this.auth.currentSchema();
    const nombre = user?.empresaNombre?.trim() ?? '';

    // Paso 1: mostrar logo inmediatamente desde el login response (sin latencia)
    if (user?.empresaLogoUuid) {
      const logoUrl = `${environment.apiUrl}/archivos/${user.empresaLogoUuid}`;
      this.state.set({ nombre: nombre || 'SESA Salud', logoUrl });
    }

    if (!schema || schema === 'public') {
      // SUPERADMINISTRADOR: fallback a UUID persistido si login response no lo trajo
      if (!user?.empresaLogoUuid) {
        const persistedUuid = this.auth.getPersistedLogoUuid();
        if (persistedUuid) {
          this.state.set({ nombre: nombre || 'SESA Salud',
            logoUrl: `${environment.apiUrl}/archivos/${persistedUuid}` });
        } else {
          this.state.set({ nombre, logoUrl: null });
        }
      }
      // Paso 2: refrescar siempre desde BD para que los cambios se reflejen
      this.refreshFromDb(nombre);
      return;
    }

    // ADMIN: refrescar desde BD con getCurrent()
    this.empresaService.getCurrent().pipe(
      switchMap((emp) => {
        const nombreEmp = emp?.razonSocial?.trim() || nombre;
        const imagenUrl = emp?.imagenUrl;
        if (!imagenUrl) {
          return of({ nombre: nombreEmp, logoUrl: null as string | null });
        }
        if (UUID_RE.test(imagenUrl)) {
          return of({ nombre: nombreEmp, logoUrl: `${environment.apiUrl}/archivos/${imagenUrl}` });
        }
        // Legado: imagenUrl == "db" → blob fetch con auth header
        return this.empresaService.getLogoBlob().pipe(
          map((blob) => ({ nombre: nombreEmp, logoUrl: URL.createObjectURL(blob) as string })),
          catchError(() => of({ nombre: nombreEmp, logoUrl: null as string | null }))
        );
      }),
      catchError(() => of({ nombre, logoUrl: user?.empresaLogoUuid
          ? `${environment.apiUrl}/archivos/${user.empresaLogoUuid}`
          : null as string | null }))
    ).subscribe((result) => {
      this.revokePrevBlobUrl(result.logoUrl);
      this.state.set(result);
    });
  }

  /**
   * Refresca el logo del SUPERADMINISTRADOR directamente desde la BD.
   * Obtiene la lista de empresas y aplica el logo de la primera que tenga UUID.
   * Persiste el UUID fresco para las sesiones futuras.
   */
  private refreshFromDb(fallbackNombre: string): void {
    this.empresaService.list(0, 50).pipe(
      catchError(() => of(null))
    ).subscribe(page => {
      if (!page?.content?.length) return;
      const emp = page.content.find(e => e.imagenUrl && UUID_RE.test(e.imagenUrl ?? ''));
      if (!emp?.imagenUrl) {
        // Ninguna empresa tiene logo → limpiar si no había ninguno ya
        if (!this.state().logoUrl) {
          this.state.set({ nombre: fallbackNombre || 'SESA Salud', logoUrl: null });
        }
        return;
      }
      const logoUrl = `${environment.apiUrl}/archivos/${emp.imagenUrl}`;
      const nombreEmp = emp.razonSocial?.trim() || fallbackNombre || 'SESA Salud';
      this.revokePrevBlobUrl(logoUrl);
      this.state.set({ nombre: nombreEmp, logoUrl });
      // Persistir UUID fresco para retrocompatibilidad con sesiones sin backend actualizado
      this.auth.patchUserLogoUuid(emp.imagenUrl);
    });
  }

  refresh(): void {
    this.state.set({ nombre: '', logoUrl: null });
    this.load();
  }

  /** Borra el logo del estado global (p. ej. cuando se selecciona una empresa sin logo). */
  clearLogo(): void {
    this.state.set({ ...this.state(), logoUrl: null });
  }

  /**
   * Actualiza el logo tras un upload exitoso usando el UUID devuelto por el servidor.
   * - Actualiza el signal de estado (sidebar + preview se refrescan inmediatamente).
   * - Persiste el UUID en el usuario almacenado en localStorage para que sobreviva
   *   al logout/login sin necesitar otro GET.
   */
  applyLogoUuid(uuid: string): void {
    const logoUrl = `${environment.apiUrl}/archivos/${uuid}`;
    this.revokePrevBlobUrl(logoUrl);
    this.state.set({ ...this.state(), logoUrl });
    // Persistir en el usuario guardado para que load() lo use en el próximo login
    this.auth.patchUserLogoUuid(uuid);
  }

  /**
   * Construye la URL directa para un UUID de archivo.
   * Útil para mostrar el logo en otros contextos (PDF, email, etc.).
   */
  static buildArchivoUrl(uuid: string): string {
    return `${environment.apiUrl}/archivos/${uuid}`;
  }

  private revokePrevBlobUrl(newUrl: string | null): void {
    if (this.prevBlobUrl && this.prevBlobUrl !== newUrl) {
      URL.revokeObjectURL(this.prevBlobUrl);
      this.prevBlobUrl = undefined;
    }
    // Registrar solo si es blob URL (no UUID-based)
    if (newUrl && newUrl.startsWith('blob:')) {
      this.prevBlobUrl = newUrl;
    }
  }
}
