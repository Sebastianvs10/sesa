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
  PortalOrdenDto,
} from '../../core/services/portal-paciente.service';

@Component({
  selector: 'sesa-portal-ordenes',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  template: `
    <div class="pord">

      <!-- Page Header -->
      <div class="pord__header">
        <div class="pord__header-icon">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2"/>
            <rect x="9" y="3" width="6" height="4" rx="2"/><path d="M9 12h6M9 16h4"/>
          </svg>
        </div>
        <div>
          <h1 class="pord__title">Órdenes Médicas</h1>
          <p class="pord__sub">Procedimientos y exámenes ordenados</p>
        </div>
      </div>

      <!-- Filtros -->
      <div class="pord__filters">
        @for (f of filtros; track f.val) {
          <button class="pord__filter-chip" [class.pord__filter-chip--active]="filtroActivo() === f.val" (click)="filtroActivo.set(f.val)">
            {{ f.label }}
          </button>
        }
      </div>

      <!-- Skeletons -->
      @if (cargando()) {
        <div class="pord__list">
          @for (i of [1,2,3]; track i) {
            <div class="pord__skeleton"></div>
          }
        </div>
      }

      <!-- Lista -->
      @if (!cargando()) {
        @if (ordenesFiltradas().length > 0) {
          <div class="pord__list">
            @for (orden of ordenesFiltradas(); track orden.id) {
              <div class="pord__item">
                <div class="pord__item-top">
                  <div class="pord__tipo-chip" [class]="'pord__tipo-chip--' + getTipoClass(orden.tipo)">
                    {{ orden.tipo }}
                  </div>
                  <span class="pord__item-estado" [class]="'pord__item-estado--' + getEstadoClass(orden.estado)">
                    {{ orden.estado }}
                  </span>
                </div>

                <p class="pord__item-desc">{{ orden.descripcion }}</p>

                <div class="pord__item-meta">
                  <div class="pord__meta-item">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/>
                      <line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>
                    </svg>
                    <span>{{ orden.fecha | date:'dd/MM/yyyy' }}</span>
                  </div>
                  <div class="pord__meta-item">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
                    </svg>
                    <span>{{ orden.medico }}</span>
                  </div>
                  @if (orden.cups) {
                    <div class="pord__meta-item pord__meta-item--code">
                      <span>CUPS: {{ orden.cups }}</span>
                    </div>
                  }
                </div>

                @if (orden.diagnostico) {
                  <div class="pord__item-dx">
                    <span class="pord__item-dx-label">Diagnóstico asociado</span>
                    <span class="pord__item-dx-val">{{ orden.diagnostico }}</span>
                  </div>
                }
              </div>
            }

            @if (hayMas()) {
              <button class="pord__load-more" (click)="cargarMas()" [disabled]="cargandoMas()">
                {{ cargandoMas() ? 'Cargando…' : 'Ver más órdenes' }}
              </button>
            }
          </div>
        } @else {
          <div class="pord__empty">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2"/>
              <rect x="9" y="3" width="6" height="4" rx="2"/>
            </svg>
            <p>No hay órdenes médicas registradas</p>
          </div>
        }
      }

      @if (error()) {
        <div class="pord__error">
          <p>No se pudo cargar la información</p>
          <button (click)="cargar()">Reintentar</button>
        </div>
      }
    </div>
  `,
  styleUrl: './portal-ordenes.page.scss',
})
export class PortalOrdenesPageComponent implements OnInit {
  private readonly portalService = inject(PortalPacienteService);

  readonly cargando = signal(true);
  readonly cargandoMas = signal(false);
  readonly error = signal(false);
  readonly ordenes = signal<PortalOrdenDto[]>([]);
  readonly hayMas = signal(false);
  readonly filtroActivo = signal('todos');
  private pagina = 0;

  readonly filtros = [
    { label: 'Todas', val: 'todos' },
    { label: 'Laboratorio', val: 'LABORATORIO' },
    { label: 'Imagen', val: 'IMAGEN' },
    { label: 'Procedimiento', val: 'PROCEDIMIENTO' },
    { label: 'Interconsulta', val: 'INTERCONSULTA' },
  ];

  readonly ordenesFiltradas = () => {
    const ords = this.ordenes();
    const f = this.filtroActivo();
    return f === 'todos' ? ords : ords.filter(o => o.tipo === f);
  };

  getTipoClass(tipo: string): string {
    const map: Record<string, string> = {
      'LABORATORIO': 'lab',
      'IMAGEN': 'img',
      'PROCEDIMIENTO': 'proc',
      'INTERCONSULTA': 'inter',
    };
    return map[tipo] ?? 'default';
  }

  getEstadoClass(estado: string): string {
    const map: Record<string, string> = {
      'PENDIENTE': 'pending',
      'ATENDIDO': 'done',
      'CANCELADO': 'cancelled',
    };
    return map[estado] ?? 'default';
  }

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(false);
    this.pagina = 0;
    this.portalService.getOrdenes(0, 10).subscribe({
      next: (res) => {
        this.ordenes.set(res.content);
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
    this.portalService.getOrdenes(this.pagina, 10).subscribe({
      next: (res) => {
        this.ordenes.update(prev => [...prev, ...res.content]);
        this.hayMas.set((this.pagina + 1) * 10 < res.totalElements);
        this.cargandoMas.set(false);
      },
      error: () => { this.cargandoMas.set(false); },
    });
  }
}
