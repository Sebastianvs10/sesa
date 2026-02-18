import { Injectable, inject, signal, computed } from '@angular/core';
import { AuthService } from './auth.service';
import { EmpresaService } from './empresa.service';
import { catchError, map, of, switchMap } from 'rxjs';

export interface CurrentEmpresa {
  nombre: string;
  logoUrl: string | null;
}

@Injectable({ providedIn: 'root' })
export class EmpresaCurrentService {
  private readonly auth = inject(AuthService);
  private readonly empresaService = inject(EmpresaService);
  private prevLogoUrl: string | undefined;

  private readonly state = signal<CurrentEmpresa>({ nombre: '', logoUrl: null });

  readonly currentEmpresa = this.state.asReadonly();

  /** Nombre a mostrar (empresa o fallback SESA). */
  readonly displayName = computed(() => {
    const n = this.state().nombre?.trim();
    return n || 'SESA Salud';
  });

  /** Si hay logo de empresa para mostrar. */
  readonly hasLogo = computed(() => !!this.state().logoUrl);

  /**
   * Carga la empresa del tenant actual: nombre y logo (si existe).
   * Debe llamarse cuando el usuario está autenticado y en el shell.
   */
  load(): void {
    const schema = this.auth.currentSchema();
    const user = this.auth.currentUser();
    if (!schema || schema === 'public') {
      this.state.set({ nombre: user?.empresaNombre ?? '', logoUrl: null });
      return;
    }
    const nombreFromUser = user?.empresaNombre ?? '';
    this.empresaService.getCurrent().pipe(
      switchMap((emp) => {
        const nombre = emp?.razonSocial?.trim() || nombreFromUser;
        if (!emp?.imagenUrl) return of({ nombre, logoUrl: null as string | null });
        return this.empresaService.getLogoBlob().pipe(
          map((blob) => ({ nombre, logoUrl: URL.createObjectURL(blob) as string }))
        );
      }),
      catchError(() => of({ nombre: nombreFromUser, logoUrl: null as string | null }))
    ).subscribe((result) => {
      this.revokePreviousUrl();
      if (result.logoUrl) this.prevLogoUrl = result.logoUrl;
      this.state.set(result);
    });
  }

  /** Refresca logo y nombre (p. ej. después de subir un nuevo logo). */
  refresh(): void {
    this.revokePreviousUrl();
    this.state.set({ nombre: '', logoUrl: null });
    this.load();
  }

  private revokePreviousUrl(): void {
    if (this.prevLogoUrl) {
      URL.revokeObjectURL(this.prevLogoUrl);
      this.prevLogoUrl = undefined;
    }
  }
}
