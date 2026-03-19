/**
 * Autor: Ing. J Sebastian Vargas S
 */

import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  OnInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import {
  PortalPacienteService,
  PortalResumenDto,
} from '../../core/services/portal-paciente.service';

@Component({
  selector: 'sesa-portal-dashboard',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="portal-home">

      <!-- Hero / Saludo ─────────────────────────────────────────────── -->
      <section class="portal-hero">
        <div class="portal-hero__bg-blur"></div>
        <div class="portal-hero__content">
          <div class="portal-hero__avatar">
            <span>{{ initiales() }}</span>
          </div>
          <div class="portal-hero__text">
            <p class="portal-hero__greeting">{{ saludo() }},</p>
            <h1 class="portal-hero__name">{{ nombreCorto() }}</h1>
            <p class="portal-hero__date">{{ fechaHoy() }}</p>
          </div>
        </div>
        @if (!cargando() && resumen()?.proximaCita) {
          <div class="portal-hero__prox-cita">
            <div class="portal-hero__prox-cita-icon">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>
              </svg>
            </div>
            <div class="portal-hero__prox-cita-text">
              <span class="portal-hero__prox-cita-label">Próxima cita</span>
              <span class="portal-hero__prox-cita-val">
                {{ resumen()?.proximaCita?.fecha | date:'dd/MM/yyyy' }} · {{ resumen()?.proximaCita?.hora }}
              </span>
            </div>
            <span class="portal-hero__prox-cita-badge">{{ resumen()?.proximaCita?.especialidad }}</span>
          </div>
          @if (resumen()?.proximaCita?.id) {
            <a [routerLink]="['/portal/cita', resumen()?.proximaCita?.id, 'cuestionario']" class="portal-hero__cuestionario-link">
              Completar cuestionario pre-consulta
            </a>
          }
        }
      </section>

      <!-- Stats Cards ────────────────────────────────────────────────── -->
      @if (cargando()) {
        <section class="portal-stats">
          @for (i of [1,2,3]; track i) {
            <div class="portal-stat-card portal-stat-card--skeleton"></div>
          }
        </section>
      } @else {
        <section class="portal-stats">
          <a routerLink="/portal/historia-clinica" class="portal-stat-card portal-stat-card--hc">
            <div class="portal-stat-card__icon">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/>
                <line x1="12" y1="18" x2="12" y2="12"/><line x1="9" y1="15" x2="15" y2="15"/>
              </svg>
            </div>
            <div class="portal-stat-card__body">
              <span class="portal-stat-card__val">{{ resumen()?.totalHistorias ?? '—' }}</span>
              <span class="portal-stat-card__label">Historias</span>
            </div>
            <svg class="portal-stat-card__arrow" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="9 18 15 12 9 6"/>
            </svg>
          </a>

          <a routerLink="/portal/laboratorios" class="portal-stat-card portal-stat-card--lab">
            <div class="portal-stat-card__icon">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M9 3H5a2 2 0 0 0-2 2v4m6-6h10a2 2 0 0 1 2 2v4M9 3v11l-2 3a1 1 0 0 0 .85 1.5h8.3A1 1 0 0 0 17 17l-2-3V3"/>
              </svg>
            </div>
            <div class="portal-stat-card__body">
              <span class="portal-stat-card__val">{{ resumen()?.totalLaboratorios ?? '—' }}</span>
              <span class="portal-stat-card__label">Laboratorios</span>
            </div>
            <svg class="portal-stat-card__arrow" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="9 18 15 12 9 6"/>
            </svg>
          </a>

          <a routerLink="/portal/ordenes" class="portal-stat-card portal-stat-card--ord">
            <div class="portal-stat-card__icon">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2"/>
                <rect x="9" y="3" width="6" height="4" rx="2"/><path d="M9 12h6M9 16h4"/>
              </svg>
            </div>
            <div class="portal-stat-card__body">
              <span class="portal-stat-card__val">{{ resumen()?.totalOrdenes ?? '—' }}</span>
              <span class="portal-stat-card__label">Órdenes</span>
            </div>
            <svg class="portal-stat-card__arrow" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="9 18 15 12 9 6"/>
            </svg>
          </a>
        </section>
      }

      <!-- Última consulta ────────────────────────────────────────────── -->
      @if (!cargando() && resumen()?.ultimaConsulta) {
        <section class="portal-section">
          <h2 class="portal-section__title">Última consulta</h2>
          <div class="portal-consulta-card">
            <div class="portal-consulta-card__header">
              <div class="portal-consulta-card__avatar">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
                </svg>
              </div>
              <div class="portal-consulta-card__info">
                <span class="portal-consulta-card__medico">{{ resumen()?.ultimaConsulta?.medico }}</span>
                <span class="portal-consulta-card__fecha">{{ resumen()?.ultimaConsulta?.fecha | date:'dd MMM yyyy' }}</span>
              </div>
            </div>
            <p class="portal-consulta-card__motivo">{{ resumen()?.ultimaConsulta?.motivo }}</p>
            @if (resumen()?.ultimaConsulta?.diagnostico) {
              <div class="portal-consulta-card__dx">
                <span class="portal-consulta-card__dx-label">Diagnóstico</span>
                <span class="portal-consulta-card__dx-val">{{ resumen()?.ultimaConsulta?.diagnostico }}</span>
              </div>
            }
          </div>
        </section>
      }

      <!-- Accesos rápidos ────────────────────────────────────────────── -->
      <section class="portal-section">
        <h2 class="portal-section__title">Accesos rápidos</h2>
        <div class="portal-quicklinks">
          <a routerLink="/portal/historia-clinica" class="portal-quicklink">
            <div class="portal-quicklink__icon portal-quicklink__icon--blue">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/>
              </svg>
            </div>
            <span>Historia clínica</span>
          </a>
          <a routerLink="/portal/laboratorios" class="portal-quicklink">
            <div class="portal-quicklink__icon portal-quicklink__icon--green">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M9 3H5a2 2 0 0 0-2 2v4m6-6h10a2 2 0 0 1 2 2v4M9 3v11l-2 3a1 1 0 0 0 .85 1.5h8.3A1 1 0 0 0 17 17l-2-3V3"/>
              </svg>
            </div>
            <span>Laboratorios</span>
          </a>
          <a routerLink="/portal/ordenes" class="portal-quicklink">
            <div class="portal-quicklink__icon portal-quicklink__icon--purple">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2"/>
                <rect x="9" y="3" width="6" height="4" rx="2"/>
              </svg>
            </div>
            <span>Órdenes médicas</span>
          </a>
          <a routerLink="/portal/perfil" class="portal-quicklink">
            <div class="portal-quicklink__icon portal-quicklink__icon--orange">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
              </svg>
            </div>
            <span>Mi perfil</span>
          </a>
        </div>
      </section>

      <!-- Error banner ──────────────────────────────────────────────── -->
      @if (error()) {
        <div class="portal-error-banner">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
          <span>No se pudo cargar la información. Verifica tu conexión.</span>
          <button (click)="cargarResumen()">Reintentar</button>
        </div>
      }

    </div>
  `,
  styleUrl: './portal-dashboard.page.scss',
})
export class PortalDashboardPageComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly portalService = inject(PortalPacienteService);

  readonly cargando = signal(true);
  readonly resumen = signal<PortalResumenDto | null>(null);
  readonly error = signal(false);

  readonly initiales = () => {
    const nombre = this.auth.currentUser()?.nombreCompleto ?? '';
    return nombre.split(' ').slice(0, 2).map(n => n[0]).join('').toUpperCase() || '?';
  };

  readonly nombreCorto = () => {
    const nombre = this.auth.currentUser()?.nombreCompleto ?? '';
    return nombre.split(' ')[0] ?? nombre;
  };

  readonly saludo = () => {
    const h = new Date().getHours();
    if (h < 12) return 'Buenos días';
    if (h < 18) return 'Buenas tardes';
    return 'Buenas noches';
  };

  readonly fechaHoy = () =>
    new Date().toLocaleDateString('es-CO', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });

  ngOnInit(): void {
    this.cargarResumen();
  }

  cargarResumen(): void {
    this.cargando.set(true);
    this.error.set(false);
    this.portalService.getResumen().subscribe({
      next: (data) => {
        this.resumen.set(data);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
        this.error.set(true);
      },
    });
  }
}
