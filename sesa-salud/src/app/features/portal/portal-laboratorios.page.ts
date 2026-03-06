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
  PortalLaboratorioDto,
} from '../../core/services/portal-paciente.service';

@Component({
  selector: 'sesa-portal-laboratorios',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  template: `
    <div class="plab">

      <!-- Page Header -->
      <div class="plab__header">
        <div class="plab__header-icon">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 3H5a2 2 0 0 0-2 2v4m6-6h10a2 2 0 0 1 2 2v4M9 3v11l-2 3a1 1 0 0 0 .85 1.5h8.3A1 1 0 0 0 17 17l-2-3V3"/>
          </svg>
        </div>
        <div>
          <h1 class="plab__title">Laboratorios</h1>
          <p class="plab__sub">Resultados de exámenes</p>
        </div>
      </div>

      <!-- Filtros de estado -->
      <div class="plab__filters">
        @for (f of filtros; track f.val) {
          <button class="plab__filter-chip" [class.plab__filter-chip--active]="filtroActivo() === f.val" (click)="filtroActivo.set(f.val)">
            {{ f.label }}
          </button>
        }
      </div>

      <!-- Skeletons -->
      @if (cargando()) {
        <div class="plab__list">
          @for (i of [1,2,3,4]; track i) {
            <div class="plab__skeleton"></div>
          }
        </div>
      }

      <!-- Lista de resultados -->
      @if (!cargando()) {
        @if (laboratoriosFiltrados().length > 0) {
          <div class="plab__list">
            @for (lab of laboratoriosFiltrados(); track lab.id) {
              <div class="plab__item" [class.plab__item--critico]="lab.critico">
                <div class="plab__item-header">
                  <div class="plab__item-status-dot" [class.plab__item-status-dot--done]="lab.estado === 'RESULTADO'" [class.plab__item-status-dot--pending]="lab.estado !== 'RESULTADO'"></div>
                  <div class="plab__item-info">
                    <span class="plab__item-nombre">{{ lab.nombreExamen }}</span>
                    <span class="plab__item-fecha">{{ lab.fechaSolicitud | date:'dd/MM/yyyy' }}</span>
                  </div>
                  <span class="plab__item-badge" [class.plab__item-badge--done]="lab.estado === 'RESULTADO'" [class.plab__item-badge--pending]="lab.estado !== 'RESULTADO'">
                    {{ lab.estado === 'RESULTADO' ? 'Resultado' : 'Pendiente' }}
                  </span>
                  @if (lab.critico) {
                    <span class="plab__item-critico-tag">⚠ Crítico</span>
                  }
                </div>

                @if (lab.resultado) {
                  <div class="plab__item-resultado">
                    <div class="plab__resultado-row">
                      <div class="plab__resultado-col">
                        <span class="plab__resultado-label">Resultado</span>
                        <span class="plab__resultado-val" [class.plab__resultado-val--critico]="lab.critico">{{ lab.resultado }}</span>
                      </div>
                      @if (lab.valorReferencia) {
                        <div class="plab__resultado-col">
                          <span class="plab__resultado-label">Valor referencia</span>
                          <span class="plab__resultado-val plab__resultado-val--ref">{{ lab.valorReferencia }}</span>
                        </div>
                      }
                    </div>
                    <div class="plab__resultado-medico">
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
                      </svg>
                      <span>{{ lab.medico }}</span>
                      @if (lab.fechaResultado) {
                        <span class="plab__resultado-fecha">· {{ lab.fechaResultado | date:'dd/MM/yyyy' }}</span>
                      }
                    </div>
                  </div>
                } @else {
                  <div class="plab__item-pending-msg">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
                    </svg>
                    <span>Resultado en proceso · Médico: {{ lab.medico }}</span>
                  </div>
                }
              </div>
            }

            @if (hayMas()) {
              <button class="plab__load-more" (click)="cargarMas()" [disabled]="cargandoMas()">
                {{ cargandoMas() ? 'Cargando…' : 'Ver más resultados' }}
              </button>
            }
          </div>
        } @else {
          <div class="plab__empty">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 3H5a2 2 0 0 0-2 2v4m6-6h10a2 2 0 0 1 2 2v4M9 3v11l-2 3a1 1 0 0 0 .85 1.5h8.3A1 1 0 0 0 17 17l-2-3V3"/>
            </svg>
            <p>No hay resultados de laboratorio</p>
          </div>
        }
      }

      @if (error()) {
        <div class="plab__error">
          <p>No se pudo cargar la información</p>
          <button (click)="cargar()">Reintentar</button>
        </div>
      }
    </div>
  `,
  styleUrl: './portal-laboratorios.page.scss',
})
export class PortalLaboratoriosPageComponent implements OnInit {
  private readonly portalService = inject(PortalPacienteService);

  readonly cargando = signal(true);
  readonly cargandoMas = signal(false);
  readonly error = signal(false);
  readonly laboratorios = signal<PortalLaboratorioDto[]>([]);
  readonly hayMas = signal(false);
  readonly filtroActivo = signal<string>('todos');
  private pagina = 0;

  readonly filtros = [
    { label: 'Todos', val: 'todos' },
    { label: 'Resultado', val: 'RESULTADO' },
    { label: 'Pendiente', val: 'PENDIENTE' },
    { label: 'Críticos', val: 'criticos' },
  ];

  readonly laboratoriosFiltrados = () => {
    const labs = this.laboratorios();
    const f = this.filtroActivo();
    if (f === 'todos') return labs;
    if (f === 'criticos') return labs.filter(l => l.critico);
    return labs.filter(l => l.estado === f);
  };

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(false);
    this.pagina = 0;
    this.portalService.getLaboratorios(0, 10).subscribe({
      next: (res) => {
        this.laboratorios.set(res.content);
        this.hayMas.set(res.content.length < res.totalElements);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
        this.error.set(true);
      },
    });
  }

  cargarMas(): void {
    this.pagina++;
    this.cargandoMas.set(true);
    this.portalService.getLaboratorios(this.pagina, 10).subscribe({
      next: (res) => {
        this.laboratorios.update(prev => [...prev, ...res.content]);
        this.hayMas.set((this.pagina + 1) * 10 < res.totalElements);
        this.cargandoMas.set(false);
      },
      error: () => { this.cargandoMas.set(false); },
    });
  }
}
