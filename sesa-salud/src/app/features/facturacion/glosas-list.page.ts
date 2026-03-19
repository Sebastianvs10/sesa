/**
 * S9: Listado de glosas y reporte recuperación de cartera.
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { GlosaService, GlosaDto, RecuperacionCarteraDto } from '../../core/services/glosa.service';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';

@Component({
  standalone: true,
  selector: 'sesa-glosas-list-page',
  imports: [CommonModule, FormsModule, RouterLink, SesaCardComponent],
  templateUrl: './glosas-list.page.html',
  styleUrl: './glosas-list.page.scss',
})
export class GlosasListPageComponent implements OnInit {
  private readonly glosaService = inject(GlosaService);
  private readonly route = inject(ActivatedRoute);
  private readonly toast = inject(SesaToastService);

  glosas = signal<GlosaDto[]>([]);
  reporte = signal<RecuperacionCarteraDto | null>(null);
  loading = signal(true);
  loadingReporte = signal(false);

  filtroEstado = '';
  filtroDesde = '';
  filtroHasta = '';
  filtroFacturaId: number | null = null;

  ngOnInit(): void {
    this.filtroFacturaId = this.route.snapshot.queryParams['facturaId'] ? Number(this.route.snapshot.queryParams['facturaId']) : null;
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    const params: { estado?: string; desde?: string; hasta?: string; facturaId?: number } = {};
    if (this.filtroEstado) params.estado = this.filtroEstado;
    if (this.filtroDesde) params.desde = this.filtroDesde + 'T00:00:00Z';
    if (this.filtroHasta) params.hasta = this.filtroHasta + 'T23:59:59Z';
    if (this.filtroFacturaId) params.facturaId = this.filtroFacturaId;
    this.glosaService.list(params).subscribe({
      next: (list) => { this.glosas.set(list); this.loading.set(false); },
      error: () => { this.toast.error('Error al cargar glosas'); this.loading.set(false); },
    });
  }

  cargarReporte(): void {
    this.loadingReporte.set(true);
    const desde = this.filtroDesde ? this.filtroDesde + 'T00:00:00Z' : undefined;
    const hasta = this.filtroHasta ? this.filtroHasta + 'T23:59:59Z' : undefined;
    this.glosaService.recuperacionCartera(desde, hasta).subscribe({
      next: (r) => { this.reporte.set(r); this.loadingReporte.set(false); },
      error: () => { this.toast.error('Error al cargar reporte'); this.loadingReporte.set(false); },
    });
  }

  estadoLabel(estado: string): string {
    const map: Record<string, string> = {
      PENDIENTE: 'Pendiente',
      ENVIADO: 'Enviado',
      ACEPTADO: 'Aceptado',
      RECHAZADO: 'Rechazado',
    };
    return map[estado] ?? estado;
  }
}
