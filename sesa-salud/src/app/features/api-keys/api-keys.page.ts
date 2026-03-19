/**
 * S12: Página de administración de API Keys para integradores.
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { ApiKeyService, ApiKeyResponse, ApiKeyCreateResponse } from '../../core/services/api-key.service';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';
import { environment } from '../../../environments/environment';

@Component({
  standalone: true,
  selector: 'sesa-api-keys-page',
  imports: [CommonModule, FormsModule, SesaCardComponent],
  templateUrl: './api-keys.page.html',
  styleUrl: './api-keys.page.scss',
})
export class ApiKeysPageComponent implements OnInit {
  private readonly apiKeyService = inject(ApiKeyService);
  private readonly toast = inject(SesaToastService);

  list = signal<ApiKeyResponse[]>([]);
  createdKey = signal<ApiKeyCreateResponse | null>(null);
  guardando = signal(false);
  cargando = signal(false);
  error: string | null = null;
  search = signal('');
  statusFilter = signal<'all' | 'active' | 'inactive'>('all');

  form = { nombre: '', permisos: 'LABORATORIO' };

  apiDocsUrl = `${environment.apiUrl?.replace('/api', '') || ''}/swagger-ui.html`;
  readonly totalKeys = computed(() => this.list().length);
  readonly activeKeys = computed(() => this.list().filter((k) => k.activo).length);
  readonly inactiveKeys = computed(() => this.list().filter((k) => !k.activo).length);
  readonly filteredKeys = computed(() => {
    const query = this.search().trim().toLowerCase();
    const status = this.statusFilter();
    return this.list().filter((k) => {
      const statusOk =
        status === 'all' || (status === 'active' && k.activo) || (status === 'inactive' && !k.activo);
      if (!statusOk) return false;
      if (!query) return true;
      return (
        k.nombreIntegrador.toLowerCase().includes(query) ||
        k.permisos.toLowerCase().includes(query)
      );
    });
  });

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.apiKeyService.listar().subscribe({
      next: (res) => {
        this.list.set(res ?? []);
        this.cargando.set(false);
      },
      error: (err) => {
        this.error = err?.error?.error || 'Error al cargar API Keys';
        this.cargando.set(false);
        this.toast.error(this.error ?? 'Error al cargar API Keys', 'Error');
      },
    });
  }

  crear(): void {
    if (!this.form.nombre.trim()) {
      this.toast.warning('Ingrese el nombre del integrador', 'Validación');
      return;
    }
    this.guardando.set(true);
    this.error = null;
    this.apiKeyService.crear(this.form.nombre, this.form.permisos).subscribe({
      next: (res) => {
        this.createdKey.set(res);
        this.form.nombre = '';
        this.guardando.set(false);
        this.cargar();
        this.toast.success('API Key creada. Copie la clave; no se mostrará de nuevo.', 'API Key');
      },
      error: (err) => {
        this.error = err?.error?.error || 'Error al crear API Key';
        this.toast.error(this.error!, 'Error');
        this.guardando.set(false);
      },
    });
  }

  desactivar(id: number): void {
    if (!confirm('¿Desactivar esta API Key? Las peticiones con esta clave dejarán de funcionar.')) return;
    this.apiKeyService.desactivar(id).subscribe({
      next: () => {
        this.cargar();
        this.toast.success('API Key desactivada', 'Listo');
      },
      error: (err) => this.toast.error(err?.error?.error || 'Error', 'Error'),
    });
  }

  async copiarClaveCreada(): Promise<void> {
    const value = this.createdKey()?.apiKeyRaw;
    if (!value) return;
    try {
      await navigator.clipboard.writeText(value);
      this.toast.success('Clave copiada al portapapeles', 'Copiar');
    } catch {
      this.toast.error('No se pudo copiar la clave', 'Copiar');
    }
  }

  clearFilters(): void {
    this.search.set('');
    this.statusFilter.set('all');
  }

  setStatusFilter(status: 'all' | 'active' | 'inactive'): void {
    this.statusFilter.set(status);
  }

  formatPermisos(permisos: string): string[] {
    return (permisos || '')
      .split(',')
      .map((p) => p.trim())
      .filter(Boolean);
  }
}
