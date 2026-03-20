import { CommonModule } from '@angular/common';
import { Component, inject, computed, signal, OnInit, OnDestroy } from '@angular/core';
import { Router, RouterLink, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { Subscription } from 'rxjs';

export interface BreadcrumbItem {
  label: string;
  path: string;
}

@Component({
  standalone: true,
  selector: 'sesa-breadcrumb',
  imports: [CommonModule, RouterLink],
  templateUrl: './sesa-breadcrumb.component.html',
  styleUrl: './sesa-breadcrumb.component.scss',
})
export class SesaBreadcrumbComponent implements OnInit, OnDestroy {
  private readonly router = inject(Router);
  private sub: Subscription | null = null;

  private readonly urlSignal = signal(this.router.url);

  private readonly segmentLabels: Record<string, string> = {
    '': 'Inicio',
    dashboard: 'Dashboard',
    pacientes: 'Pacientes',
    nuevo: 'Nuevo',
    editar: 'Editar',
    'historia-clinica': 'Historia clínica',
    nueva: 'Nueva',
    laboratorios: 'Laboratorios',
    'imagenes-diagnosticas': 'Imágenes',
    urgencias: 'Urgencias',
    hospitalizacion: 'Hospitalización',
    farmacia: 'Farmacia',
    facturacion: 'Facturación',
    citas: 'Citas',
    'mi-empresa': 'Mi empresa',
    empresas: 'Empresas',
    personal: 'Personal',
    usuarios: 'Usuarios Adm',
    login: 'Iniciar sesión',
  };

  items = computed(() => {
    const url = this.urlSignal().split('?')[0];
    const segments = url.split('/').filter(Boolean);
    const result: BreadcrumbItem[] = [{ label: 'Inicio', path: '/dashboard' }];
    let path = '';
    for (const seg of segments) {
      path += '/' + seg;
      const label = this.segmentLabels[seg] ?? this.formatSegment(seg);
      result.push({ label, path });
    }
    return result;
  });

  ngOnInit(): void {
    this.sub = this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(() => this.urlSignal.set(this.router.url));
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  private formatSegment(seg: string): string {
    if (/^\d+$/.test(seg)) return 'Detalle';
    return seg.charAt(0).toUpperCase() + seg.slice(1).replace(/-/g, ' ');
  }
}
