/**
 * Modal premium "Mi Perfil" — muestra la información personal del usuario autenticado.
 * Patrón: servicio signal + outlet global (igual que sesa-confirm-dialog).
 * Autor: Ing. J Sebastian Vargas S
 */

import { Component, inject, Injectable, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';

@Injectable({ providedIn: 'root' })
export class SesaPerfilModalService {
  readonly visible = signal(false);
  readonly leaving = signal(false);

  open(): void {
    this.leaving.set(false);
    this.visible.set(true);
  }

  close(): void {
    this.leaving.set(true);
    setTimeout(() => {
      this.visible.set(false);
      this.leaving.set(false);
    }, 280);
  }
}

@Component({
  standalone: true,
  selector: 'sesa-perfil-modal-outlet',
  imports: [CommonModule],
  template: `
    @if (svc.visible()) {
      <div
        class="spm-backdrop"
        [class.spm-backdrop--leaving]="svc.leaving()"
        (click)="svc.close()"
        role="presentation"
      >
        <div
          class="spm-modal"
          [class.spm-modal--leaving]="svc.leaving()"
          role="dialog"
          aria-modal="true"
          aria-labelledby="spm-title"
          (click)="$event.stopPropagation()"
        >
          <!-- Botón cerrar -->
          <button type="button" class="spm-close" (click)="svc.close()" aria-label="Cerrar perfil">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" aria-hidden="true">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>

          <!-- Cover con gradiente -->
          <div class="spm-cover">
            <div class="spm-cover-pattern"></div>
            <div class="spm-cover-glow"></div>
          </div>

          <!-- Avatar flotante sobre el cover -->
          <div class="spm-avatar-wrap">
            <div class="spm-avatar">
              <span class="spm-avatar-initials">{{ userInitials }}</span>
            </div>
            <div class="spm-avatar-ring"></div>
          </div>

          <!-- Cuerpo del modal -->
          <div class="spm-body">
            <h2 class="spm-name" id="spm-title">{{ auth.currentUser()?.nombreCompleto ?? 'Usuario' }}</h2>
            <span class="spm-role-badge" [class]="'spm-role-badge--' + roleKey">
              <span class="spm-role-dot"></span>
              {{ roleLabel }}
            </span>

            <!-- Separador decorativo -->
            <div class="spm-divider">
              <span class="spm-divider-line"></span>
              <span class="spm-divider-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                  <circle cx="12" cy="7" r="4"/>
                </svg>
              </span>
              <span class="spm-divider-line"></span>
            </div>

            <!-- Filas de información -->
            <ul class="spm-info-list">
              <li class="spm-info-item">
                <span class="spm-info-icon spm-info-icon--email">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <rect x="2" y="4" width="20" height="16" rx="3"/>
                    <path d="M22 7l-10 7L2 7"/>
                  </svg>
                </span>
                <div class="spm-info-content">
                  <span class="spm-info-label">Correo electrónico</span>
                  <span class="spm-info-value">{{ auth.currentUser()?.email ?? '—' }}</span>
                </div>
              </li>

              @if (auth.currentUser()?.empresaNombre) {
                <li class="spm-info-item">
                  <span class="spm-info-icon spm-info-icon--empresa">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                      <rect x="2" y="7" width="20" height="15" rx="2"/>
                      <path d="M16 7V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2"/>
                      <line x1="12" y1="12" x2="12" y2="16"/>
                      <line x1="10" y1="14" x2="14" y2="14"/>
                    </svg>
                  </span>
                  <div class="spm-info-content">
                    <span class="spm-info-label">Empresa</span>
                    <span class="spm-info-value">{{ auth.currentUser()?.empresaNombre }}</span>
                  </div>
                </li>
              }

              @if (auth.currentSchema()) {
                <li class="spm-info-item">
                  <span class="spm-info-icon spm-info-icon--schema">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                      <ellipse cx="12" cy="5" rx="9" ry="3"/>
                      <path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3"/>
                      <path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5"/>
                    </svg>
                  </span>
                  <div class="spm-info-content">
                    <span class="spm-info-label">Esquema / Tenant</span>
                    <span class="spm-info-value spm-info-value--mono">{{ auth.currentSchema() }}</span>
                  </div>
                </li>
              }

              <li class="spm-info-item">
                <span class="spm-info-icon spm-info-icon--id">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <rect x="2" y="5" width="20" height="14" rx="2"/>
                    <path d="M16 10a2 2 0 1 1-4 0 2 2 0 0 1 4 0"/>
                    <path d="M22 10h-2M4 10H2M12 16v2"/>
                  </svg>
                </span>
                <div class="spm-info-content">
                  <span class="spm-info-label">ID de usuario</span>
                  <span class="spm-info-value spm-info-value--mono">#{{ auth.currentUser()?.userId ?? '—' }}</span>
                </div>
              </li>
            </ul>

            <!-- Footer -->
            <div class="spm-footer">
              <div class="spm-status-badge">
                <span class="spm-status-dot"></span>
                Sesión activa
              </div>
              <button type="button" class="spm-btn-close" (click)="svc.close()">
                Cerrar
              </button>
            </div>
          </div>
        </div>
      </div>
    }
  `,
  styleUrl: './sesa-perfil-modal.component.scss',
})
export class SesaPerfilModalOutletComponent {
  svc = inject(SesaPerfilModalService);
  auth = inject(AuthService);

  get userInitials(): string {
    const user = this.auth.currentUser();
    if (!user?.nombreCompleto) return '?';
    const parts = user.nombreCompleto.trim().split(/\s+/);
    if (parts.length >= 2) return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    return (parts[0][0] ?? '?').toUpperCase();
  }

  get roleLabel(): string {
    const role = this.auth.currentUser()?.role ?? '';
    const labels: Record<string, string> = {
      SUPERADMINISTRADOR: 'Superadministrador',
      ADMINISTRADOR: 'Administrador',
      MEDICO: 'Médico',
      ENFERMERO: 'Enfermero / Enfermera',
      RECEPCIONISTA: 'Recepcionista',
      FARMACEUTICO: 'Farmacéutico',
      LABORATORISTA: 'Laboratorista',
      CONTADOR: 'Contador',
    };
    return labels[role] ?? role;
  }

  get roleKey(): string {
    const role = this.auth.currentUser()?.role ?? 'DEFAULT';
    const map: Record<string, string> = {
      SUPERADMINISTRADOR: 'superadmin',
      ADMINISTRADOR: 'admin',
      MEDICO: 'medico',
      ENFERMERO: 'enfermero',
      RECEPCIONISTA: 'recepcionista',
      FARMACEUTICO: 'farmaceutico',
      LABORATORISTA: 'laboratorista',
      CONTADOR: 'contador',
    };
    return map[role] ?? 'default';
  }
}
