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
import {
  PortalPacienteService,
  PortalConsultaDto,
} from '../../core/services/portal-paciente.service';
import { HistoriaClinicaDto } from '../../core/services/historia-clinica.service';

@Component({
  selector: 'sesa-portal-hc',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  template: `
    <div class="phc">

      <!-- Page Header -->
      <div class="phc__header">
        <h1 class="phc__title">Mi Historia Clínica</h1>
        <p class="phc__sub">Tu expediente médico completo</p>
      </div>

      <!-- Tabs -->
      <div class="phc__tabs" role="tablist">
        <button role="tab" class="phc__tab" [class.phc__tab--active]="tabActivo() === 'antecedentes'" (click)="tabActivo.set('antecedentes')">
          Antecedentes
        </button>
        <button role="tab" class="phc__tab" [class.phc__tab--active]="tabActivo() === 'consultas'" (click)="tabActivo.set('consultas')">
          Consultas
        </button>
      </div>

      <!-- Cargando -->
      @if (cargando()) {
        <div class="phc__list">
          @for (i of [1,2,3]; track i) {
            <div class="phc__skeleton"></div>
          }
        </div>
      }

      <!-- Tab Antecedentes -->
      @if (!cargando() && tabActivo() === 'antecedentes') {
        @if (historia()) {
          <div class="phc__section-wrap">
            <!-- Datos generales -->
            <div class="phc__card">
              <div class="phc__card-header phc__card-header--blue">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="10"/><path d="M12 8v4l3 3"/>
                </svg>
                <span>Información general</span>
              </div>
              <div class="phc__fields">
                @if (historia()?.grupoSanguineo) {
                  <div class="phc__field">
                    <span class="phc__field-label">Grupo sanguíneo</span>
                    <span class="phc__field-val phc__field-val--badge phc__field-val--red">{{ historia()?.grupoSanguineo }}</span>
                  </div>
                }
                <div class="phc__field">
                  <span class="phc__field-label">Estado HC</span>
                  <span class="phc__field-val phc__field-val--badge" [class.phc__field-val--green]="historia()?.estado === 'ACTIVO'">{{ historia()?.estado }}</span>
                </div>
                @if (historia()?.fechaApertura) {
                  <div class="phc__field">
                    <span class="phc__field-label">Fecha apertura</span>
                    <span class="phc__field-val">{{ historia()?.fechaApertura | date:'dd/MM/yyyy' }}</span>
                  </div>
                }
              </div>
            </div>

            <!-- Alergias -->
            @if (historia()?.alergiasGenerales) {
              <div class="phc__card">
                <div class="phc__card-header phc__card-header--red">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/>
                    <line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>
                  </svg>
                  <span>Alergias</span>
                </div>
                <p class="phc__text-block">{{ historia()?.alergiasGenerales }}</p>
              </div>
            }

            <!-- Antecedentes personales -->
            @if (historia()?.antecedentesPersonales) {
              <div class="phc__card">
                <div class="phc__card-header phc__card-header--blue">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
                  </svg>
                  <span>Antecedentes personales</span>
                </div>
                <p class="phc__text-block">{{ historia()?.antecedentesPersonales }}</p>
              </div>
            }

            <!-- Antecedentes familiares -->
            @if (historia()?.antecedentesFamiliares) {
              <div class="phc__card">
                <div class="phc__card-header phc__card-header--purple">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                    <circle cx="9" cy="7" r="4"/>
                    <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                    <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
                  </svg>
                  <span>Antecedentes familiares</span>
                </div>
                <p class="phc__text-block">{{ historia()?.antecedentesFamiliares }}</p>
              </div>
            }

            <!-- Sin datos -->
            @if (!historia()?.alergiasGenerales && !historia()?.antecedentesPersonales && !historia()?.antecedentesFamiliares) {
              <div class="phc__empty">
                <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/>
                </svg>
                <p>No hay antecedentes registrados</p>
              </div>
            }
          </div>
        } @else {
          <div class="phc__empty">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/>
            </svg>
            <p>No tienes historia clínica registrada aún</p>
          </div>
        }
      }

      <!-- Tab Consultas -->
      @if (!cargando() && tabActivo() === 'consultas') {
        @if (consultas().length > 0) {
          <div class="phc__list">
            @for (c of consultas(); track c.id) {
              <div class="phc__consulta-item" (click)="toggleConsulta(c.id)" [class.phc__consulta-item--open]="consultaAbierta() === c.id">
                <div class="phc__consulta-summary">
                  <div class="phc__consulta-dot"></div>
                  <div class="phc__consulta-info">
                    <span class="phc__consulta-fecha">{{ c.fecha | date:'dd MMM yyyy' }}</span>
                    <span class="phc__consulta-motivo">{{ c.motivo | slice:0:60 }}{{ c.motivo.length > 60 ? '…' : '' }}</span>
                    <span class="phc__consulta-medico">{{ c.medico }}</span>
                  </div>
                  <svg class="phc__consulta-chevron" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="6 9 12 15 18 9"/>
                  </svg>
                </div>
                @if (consultaAbierta() === c.id) {
                  <div class="phc__consulta-detail">
                    @if (c.diagnostico) {
                      <div class="phc__consulta-field">
                        <span class="phc__consulta-field-label">Diagnóstico</span>
                        <span class="phc__consulta-field-val">{{ c.diagnostico }}</span>
                      </div>
                    }
                    @if (c.codigoCie10) {
                      <div class="phc__consulta-field">
                        <span class="phc__consulta-field-label">CIE-10</span>
                        <span class="phc__consulta-field-val phc__consulta-field-val--code">{{ c.codigoCie10 }}</span>
                      </div>
                    }
                    @if (c.planTratamiento) {
                      <div class="phc__consulta-field">
                        <span class="phc__consulta-field-label">Plan de tratamiento</span>
                        <span class="phc__consulta-field-val">{{ c.planTratamiento }}</span>
                      </div>
                    }
                    @if (c.especialidad) {
                      <div class="phc__consulta-field">
                        <span class="phc__consulta-field-label">Especialidad</span>
                        <span class="phc__consulta-field-val">{{ c.especialidad }}</span>
                      </div>
                    }
                  </div>
                }
              </div>
            }
            @if (hayMasConsultas()) {
              <button class="phc__load-more" (click)="cargarMasConsultas()" [disabled]="cargandoMas()">
                {{ cargandoMas() ? 'Cargando…' : 'Cargar más consultas' }}
              </button>
            }
          </div>
        } @else {
          <div class="phc__empty">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/>
              <line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>
            </svg>
            <p>No hay consultas registradas</p>
          </div>
        }
      }

      <!-- Error -->
      @if (error()) {
        <div class="phc__error">
          <p>No se pudo cargar la información</p>
          <button (click)="cargar()">Reintentar</button>
        </div>
      }
    </div>
  `,
  styleUrl: './portal-historia-clinica.page.scss',
})
export class PortalHistoriaClinicaPageComponent implements OnInit {
  private readonly portalService = inject(PortalPacienteService);

  readonly cargando = signal(true);
  readonly cargandoMas = signal(false);
  readonly error = signal(false);
  readonly tabActivo = signal<'antecedentes' | 'consultas'>('antecedentes');
  readonly historia = signal<HistoriaClinicaDto | null>(null);
  readonly consultas = signal<PortalConsultaDto[]>([]);
  readonly consultaAbierta = signal<number | null>(null);
  readonly hayMasConsultas = signal(false);
  private paginaConsultas = 0;

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(false);

    this.portalService.getHistoriaClinica().subscribe({
      next: (hcs) => {
        this.historia.set(hcs[0] ?? null);
        this.cargarConsultas();
      },
      error: () => {
        this.cargando.set(false);
        this.error.set(true);
      },
    });
  }

  private cargarConsultas(): void {
    this.portalService.getConsultas(0, 10).subscribe({
      next: (res) => {
        this.consultas.set(res.content);
        this.hayMasConsultas.set(res.content.length < res.totalElements);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
      },
    });
  }

  cargarMasConsultas(): void {
    this.paginaConsultas++;
    this.cargandoMas.set(true);
    this.portalService.getConsultas(this.paginaConsultas, 10).subscribe({
      next: (res) => {
        this.consultas.update(prev => [...prev, ...res.content]);
        this.hayMasConsultas.set(
          (this.paginaConsultas + 1) * 10 < res.totalElements
        );
        this.cargandoMas.set(false);
      },
      error: () => { this.cargandoMas.set(false); },
    });
  }

  toggleConsulta(id: number): void {
    this.consultaAbierta.set(this.consultaAbierta() === id ? null : id);
  }
}
