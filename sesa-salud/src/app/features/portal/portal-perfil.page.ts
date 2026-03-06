/**
 * Autor: Ing. J Sebastian Vargas S
 */

import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  OnInit,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { PortalPacienteService } from '../../core/services/portal-paciente.service';
import { PacienteDto } from '../../core/services/paciente.service';

@Component({
  selector: 'sesa-portal-perfil',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  template: `
    <div class="pperf">

      <!-- Hero del perfil -->
      <div class="pperf__hero">
        <div class="pperf__hero-bg"></div>
        <div class="pperf__avatar-wrap">
          <div class="pperf__avatar">
            <span>{{ iniciales() }}</span>
          </div>
          <div class="pperf__avatar-ring"></div>
        </div>
        <div class="pperf__hero-info">
          <h1 class="pperf__nombre">{{ nombreCompleto() }}</h1>
          <div class="pperf__badges">
            @if (auth.rolActivo()) {
              <span class="pperf__badge pperf__badge--role">{{ auth.rolActivo() }}</span>
            }
            @if (paciente()?.epsNombre) {
              <span class="pperf__badge pperf__badge--eps">{{ paciente()?.epsNombre }}</span>
            }
          </div>
          <p class="pperf__email">{{ auth.currentUser()?.email }}</p>
        </div>
      </div>

      <!-- Loading -->
      @if (cargando()) {
        <div class="pperf__sections">
          @for (i of [1,2,3]; track i) {
            <div class="pperf__skeleton"></div>
          }
        </div>
      }

      <!-- Datos del paciente -->
      @if (!cargando() && paciente()) {
        <div class="pperf__sections">

          <!-- Identificación -->
          <div class="pperf__card">
            <div class="pperf__card-title">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="2" y="5" width="20" height="14" rx="2"/><line x1="2" y1="10" x2="22" y2="10"/>
              </svg>
              Identificación
            </div>
            @if (paciente()?.tipoDocumento || paciente()?.documento) {
              <div class="pperf__row">
                <span class="pperf__row-label">Documento</span>
                <span class="pperf__row-val">{{ paciente()?.tipoDocumento }} {{ paciente()?.documento }}</span>
              </div>
            }
            @if (paciente()?.fechaNacimiento) {
              <div class="pperf__row">
                <span class="pperf__row-label">Fecha de nacimiento</span>
                <span class="pperf__row-val">{{ paciente()?.fechaNacimiento | date:'dd/MM/yyyy' }}</span>
              </div>
            }
            @if (paciente()?.sexo) {
              <div class="pperf__row">
                <span class="pperf__row-label">Sexo</span>
                <span class="pperf__row-val">{{ paciente()?.sexo }}</span>
              </div>
            }
            @if (paciente()?.grupoSanguineo) {
              <div class="pperf__row">
                <span class="pperf__row-label">Grupo sanguíneo</span>
                <span class="pperf__row-val pperf__row-val--blood">{{ paciente()?.grupoSanguineo }}</span>
              </div>
            }
          </div>

          <!-- Contacto -->
          <div class="pperf__card">
            <div class="pperf__card-title">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07A19.5 19.5 0 0 1 4.69 12 19.79 19.79 0 0 1 1.58 3.59a2 2 0 0 1 1.99-2.18h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L7.91 9.91a16 16 0 0 0 6.12 6.12l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z"/>
              </svg>
              Contacto
            </div>
            @if (paciente()?.telefono) {
              <div class="pperf__row">
                <span class="pperf__row-label">Teléfono</span>
                <span class="pperf__row-val">{{ paciente()?.telefono }}</span>
              </div>
            }
            @if (paciente()?.email) {
              <div class="pperf__row">
                <span class="pperf__row-label">Email</span>
                <span class="pperf__row-val">{{ paciente()?.email }}</span>
              </div>
            }
            @if (paciente()?.direccion) {
              <div class="pperf__row">
                <span class="pperf__row-label">Dirección</span>
                <span class="pperf__row-val">{{ paciente()?.direccion }}</span>
              </div>
            }
            @if (paciente()?.municipioResidencia) {
              <div class="pperf__row">
                <span class="pperf__row-label">Municipio</span>
                <span class="pperf__row-val">{{ paciente()?.municipioResidencia }}, {{ paciente()?.departamentoResidencia }}</span>
              </div>
            }
          </div>

          <!-- Afiliación -->
          @if (paciente()?.epsNombre || paciente()?.regimenAfiliacion) {
            <div class="pperf__card">
              <div class="pperf__card-title">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>
                </svg>
                Afiliación
              </div>
              @if (paciente()?.epsNombre) {
                <div class="pperf__row">
                  <span class="pperf__row-label">EPS</span>
                  <span class="pperf__row-val">{{ paciente()?.epsNombre }}</span>
                </div>
              }
              @if (paciente()?.regimenAfiliacion) {
                <div class="pperf__row">
                  <span class="pperf__row-label">Régimen</span>
                  <span class="pperf__row-val">{{ paciente()?.regimenAfiliacion }}</span>
                </div>
              }
              @if (paciente()?.tipoUsuario) {
                <div class="pperf__row">
                  <span class="pperf__row-label">Tipo de usuario</span>
                  <span class="pperf__row-val">{{ paciente()?.tipoUsuario }}</span>
                </div>
              }
            </div>
          }

          <!-- Contacto de emergencia -->
          @if (paciente()?.contactoEmergenciaNombre) {
            <div class="pperf__card">
              <div class="pperf__card-title">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
                  <line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>
                </svg>
                Contacto de emergencia
              </div>
              <div class="pperf__row">
                <span class="pperf__row-label">Nombre</span>
                <span class="pperf__row-val">{{ paciente()?.contactoEmergenciaNombre }}</span>
              </div>
              @if (paciente()?.contactoEmergenciaTelefono) {
                <div class="pperf__row">
                  <span class="pperf__row-label">Teléfono</span>
                  <span class="pperf__row-val">{{ paciente()?.contactoEmergenciaTelefono }}</span>
                </div>
              }
            </div>
          }

          <!-- Acciones -->
          <div class="pperf__actions">
            <button class="pperf__btn-logout" (click)="cerrarSesion()">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/>
              </svg>
              Cerrar sesión
            </button>
          </div>

        </div>
      }

      @if (error()) {
        <div class="pperf__error">
          <p>No se pudo cargar el perfil</p>
          <button (click)="cargar()">Reintentar</button>
        </div>
      }
    </div>
  `,
  styleUrl: './portal-perfil.page.scss',
})
export class PortalPerfilPageComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly portalService = inject(PortalPacienteService);
  private readonly router = inject(Router);

  readonly cargando = signal(true);
  readonly error = signal(false);
  readonly paciente = signal<PacienteDto | null>(null);

  readonly iniciales = computed(() => {
    const nombre = this.auth.currentUser()?.nombreCompleto ?? '';
    return nombre.split(' ').slice(0, 2).map(n => n[0]).join('').toUpperCase() || '?';
  });

  readonly nombreCompleto = computed(() =>
    this.auth.currentUser()?.nombreCompleto ?? ''
  );

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(false);
    this.portalService.getMiPerfil().subscribe({
      next: (data) => {
        this.paciente.set(data);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
        this.error.set(true);
      },
    });
  }

  cerrarSesion(): void {
    this.auth.logout();
  }
}
