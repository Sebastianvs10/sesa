/**
 * Mapa interactivo de hogares EBS (Leaflet). Marcadores por hogar con coordenadas.
 * Autor: Ing. J Sebastian Vargas S
 */

import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  effect,
  ElementRef,
  input,
  output,
  signal,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import type { EbsHouseholdSummary, EbsRiskLevel } from '../../core/services/ebs.service';

const COLOMBIA_CENTER: [number, number] = [4.5709, -74.2973];
const DEFAULT_ZOOM = 10;

@Component({
  standalone: true,
  selector: 'sesa-ebs-territorios-map',
  imports: [CommonModule],
  templateUrl: './ebs-territorios-map.component.html',
  styleUrl: './ebs-territorios-map.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EbsTerritoriosMapComponent implements AfterViewInit {
  @ViewChild('mapContainer', { static: true }) mapContainer!: ElementRef<HTMLDivElement>;

  /** Hogares a mostrar (solo los que tengan lat/lng). */
  households = input<EbsHouseholdSummary[]>([]);
  /** Si true, ajustar vista a los marcadores. */
  fitBounds = input<boolean>(true);

  readonly householdSelect = output<EbsHouseholdSummary>();
  readonly mapReady = signal(false);

  private map: L.Map | null = null;
  private markers: L.Marker[] = [];

  constructor() {
    effect(() => {
      this.households();
      this.mapReady();
      if (this.map) this.updateMarkers();
    });
  }

  ngAfterViewInit(): void {
    this.initMap();
  }

  private initMap(): void {
    if (typeof window === 'undefined' || !this.mapContainer?.nativeElement) return;
    import('leaflet').then((L) => {
      this.map = L.map(this.mapContainer.nativeElement, {
        center: COLOMBIA_CENTER,
        zoom: DEFAULT_ZOOM,
        zoomControl: true,
      });
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap',
        maxZoom: 19,
      }).addTo(this.map);
      this.mapReady.set(true);
      queueMicrotask(() => this.updateMarkers());
    });
  }

  /** Actualiza marcadores en el mapa (se invoca desde effect al cambiar households). */
  private updateMarkers(): void {
    if (typeof window === 'undefined' || !this.map) return;
    this.markers.forEach((m) => {
      m.remove();
    });
    this.markers = [];
    const list = this.households() ?? [];
    const withCoords = list.filter((h) => h.latitude != null && h.longitude != null);
    if (withCoords.length === 0) return;

    import('leaflet').then((L) => {
      const bounds: L.LatLngBoundsLiteral = [];
      withCoords.forEach((h) => {
        const lat = Number(h.latitude);
        const lng = Number(h.longitude);
        const marker = L.marker([lat, lng], {
          icon: this.iconForRisk(h.riskLevel, L),
        })
          .on('click', () => this.householdSelect.emit(h))
          .addTo(this.map!);
        marker.bindTooltip(this.tooltipContent(h), {
          permanent: false,
          direction: 'top',
          className: 'ebs-map-tooltip',
        });
        this.markers.push(marker);
        bounds.push([lat, lng]);
      });
      if (this.fitBounds() && bounds.length > 0 && this.map) {
        this.map.fitBounds(bounds as L.LatLngBoundsExpression, { padding: [40, 40], maxZoom: 16 });
      }
    });
  }

  private iconForRisk(risk?: EbsRiskLevel, L?: typeof import('leaflet')): L.DivIcon {
    if (!L) return { options: {} } as L.DivIcon;
    const color =
      risk === 'ALTO' || risk === 'MUY_ALTO'
        ? '#dc3545'
        : risk === 'MEDIO'
          ? '#fd7e14'
          : risk === 'BAJO'
            ? '#198754'
            : '#6c757d';
    return L.divIcon({
      className: 'ebs-map-marker',
      html: `<span style="background-color:${color};width:14px;height:14px;border-radius:50%;border:2px solid #fff;box-shadow:0 1px 3px rgba(0,0,0,0.3);display:inline-block;"></span>`,
      iconSize: [14, 14],
      iconAnchor: [7, 7],
    });
  }

  private tooltipContent(h: EbsHouseholdSummary): string {
    const addr = (h.addressText || 'Sin dirección').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    const risk = h.riskLevel ? `Riesgo: ${h.riskLevel}` : '';
    const last = h.lastVisitDate ? `Última visita: ${new Date(h.lastVisitDate).toLocaleDateString('es-CO')}` : 'Sin visitas';
    return `<div class="ebs-map-tooltip-inner"><strong>${addr}</strong><br/><small>${risk}</small><br/><small>${last}</small><br/><em>Clic para acciones</em></div>`;
  }
}
