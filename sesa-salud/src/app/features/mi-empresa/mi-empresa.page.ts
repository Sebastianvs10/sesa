/**
 * Mi Empresa — rediseño premium con hero, drag-and-drop y animaciones.
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faBuilding } from '@fortawesome/free-solid-svg-icons';
import { SesaPageHeaderComponent } from '../../shared/components/sesa-page-header/sesa-page-header.component';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';
import { AuthService } from '../../core/services/auth.service';
import { EmpresaCurrentService } from '../../core/services/empresa-current.service';
import { EmpresaService, EmpresaDto } from '../../core/services/empresa.service';
import { FacturacionElectronicaConfigService, FacturacionElectronicaConfigDto } from '../../core/services/facturacion-electronica-config.service';
import { environment } from '../../../environments/environment';

/** Detecta si un string tiene formato UUID v4. */
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  standalone: true,
  selector: 'sesa-mi-empresa-page',
  imports: [CommonModule, FormsModule, FontAwesomeModule, SesaPageHeaderComponent],
  templateUrl: './mi-empresa.page.html',
  styleUrl: './mi-empresa.page.scss',
})
export class MiEmpresaPageComponent implements OnInit {
  readonly faBuilding = faBuilding;
  private readonly auth = inject(AuthService);
  private readonly empresaService = inject(EmpresaService);
  readonly empresaCurrent = inject(EmpresaCurrentService);
  private readonly toast = inject(SesaToastService);
  private readonly feConfigService = inject(FacturacionElectronicaConfigService);

  loading   = signal(false);
  error     = signal<string | null>(null);
  success   = signal(false);
  isDragOver = signal(false);
  selectedFile: File | null = null;
  /** URL temporal de previsualización del archivo seleccionado (antes de subir). */
  localPreviewUrl = signal<string | null>(null);

  /** Lista de empresas cargada solo cuando el usuario es SUPERADMINISTRADOR. */
  empresas = signal<EmpresaDto[]>([]);
  /** ID de empresa seleccionada por el SUPERADMINISTRADOR para subir el logo. */
  selectedEmpresaId = signal<number | null>(null);
  /** Empresa actual (cargada por getCurrent para ADMIN; usada en encabezado). */
  currentEmpresa = signal<EmpresaDto | null>(null);
  /**
   * URL del logo de la empresa seleccionada en el dropdown (solo SUPERADMINISTRADOR).
   * Es un estado LOCAL del componente: no modifica el sidebar ni el estado global.
   */
  selectedEmpresaLogoUrl = signal<string | null>(null);

  /** Empresa a mostrar en el hero: la actual (ADMIN) o la seleccionada (SUPERADMIN). */
  readonly headerEmpresa = computed(() =>
    this.isSuperAdmin() ? (this.selectedEmpresaId() ? this.empresas().find(e => e.id === this.selectedEmpresaId()!) ?? null : null) : this.currentEmpresa()
  );

  readonly isSuperAdmin = computed(() =>
    this.auth.currentRoles().includes('SUPERADMINISTRADOR')
  );

  /**
   * URL de logo para la preview del hero:
   * 1) Archivo local seleccionado (antes de subir).
   * 2) Logo de la empresa seleccionada en el dropdown (SUPERADMINISTRADOR).
   * 3) Logo del tenant actual (ADMIN, desde el servicio global).
   */
  readonly previewLogoUrl = computed(
    () =>
      this.localPreviewUrl() ??
      (this.isSuperAdmin()
        ? this.selectedEmpresaLogoUrl()
        : this.empresaCurrent.currentEmpresa().logoUrl)
  );

  get canUploadLogo(): boolean {
    const roles = this.auth.currentRoles();
    return roles.includes('ADMIN') || roles.includes('SUPERADMINISTRADOR');
  }

  // ── Facturación electrónica DIAN ────────────────────────────────
  feLoading = signal(false);
  feError = signal<string | null>(null);
  feSaving = signal(false);
  feConfig = signal<FacturacionElectronicaConfigDto>({});

  readonly feIsActiva = computed(() => this.feConfig().facturacionActiva ?? false);
  readonly feAmbienteLabel = computed(() => {
    const amb = this.feConfig().ambiente ?? 'HABILITACION';
    return amb === 'PRODUCCION' ? 'Producción' : 'Habilitación';
  });

  /** El botón de subir está habilitado cuando hay archivo Y, si es superadmin, también una empresa seleccionada. */
  get canSubmit(): boolean {
    if (!this.selectedFile) return false;
    if (this.isSuperAdmin() && !this.selectedEmpresaId()) return false;
    return true;
  }

  ngOnInit(): void {
    this.empresaCurrent.load();
    if (this.isSuperAdmin()) {
      this.loadEmpresas();
    } else {
      this.empresaService.getCurrent().subscribe({
        next: (e) => this.currentEmpresa.set(e),
        error: () => {},
      });
    }
    this.loadFeConfig();
  }

  private loadEmpresas(): void {
    this.empresaService.list(0, 100).subscribe({
      next: (page) => {
        this.empresas.set(page.content ?? []);
        // Auto-seleccionar si solo hay una empresa
        if (page.content?.length === 1) {
          this.selectedEmpresaId.set(page.content[0].id);
          this.updateSelectedPreview(page.content[0]);
        }
      },
      error: () => {},
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.setFile(file);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver.set(true);
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver.set(false);
    const file = event.dataTransfer?.files?.[0] ?? null;
    this.setFile(file);
  }

  setFile(file: File | null): void {
    this.selectedFile = file;
    this.error.set(null);
    this.success.set(false);
    // Previsualizar localmente antes de subir
    if (this.localPreviewUrl()) {
      URL.revokeObjectURL(this.localPreviewUrl()!);
    }
    if (file) {
      this.localPreviewUrl.set(URL.createObjectURL(file));
    } else {
      this.localPreviewUrl.set(null);
    }
  }

  onEmpresaChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const id = select.value ? +select.value : null;
    this.selectedEmpresaId.set(id);
    this.error.set(null);
    this.success.set(false);
    this.setFile(null);
    const empresa = id ? this.empresas().find(e => e.id === id) : null;
    this.updateSelectedPreview(empresa ?? null);
  }

  /**
   * Actualiza SOLO la preview local del componente según la empresa seleccionada.
   * No toca el sidebar ni el estado global: cada schema/empresa gestiona su propio logo.
   */
  private updateSelectedPreview(emp: EmpresaDto | null): void {
    const uuid = emp?.imagenUrl && UUID_RE.test(emp.imagenUrl) ? emp.imagenUrl : null;
    this.selectedEmpresaLogoUrl.set(
      uuid ? `${environment.apiUrl}/archivos/${uuid}` : null
    );
  }

  uploadLogo(): void {
    if (!this.selectedFile) {
      this.error.set('Selecciona una imagen (PNG, JPG, WebP o SVG).');
      return;
    }
    if (this.isSuperAdmin() && !this.selectedEmpresaId()) {
      this.error.set('Selecciona la empresa a la que deseas subir el logo.');
      return;
    }
    this.error.set(null);
    this.success.set(false);
    this.loading.set(true);

    const upload$ = this.isSuperAdmin()
      ? this.empresaService.uploadLogoById(this.selectedEmpresaId()!, this.selectedFile)
      : this.empresaService.uploadLogo(this.selectedFile);

    upload$.subscribe({
      next: (res) => {
        this.loading.set(false);
        this.success.set(true);
        this.selectedFile = null;
        if (this.localPreviewUrl()) {
          URL.revokeObjectURL(this.localPreviewUrl()!);
          this.localPreviewUrl.set(null);
        }
        if (this.isSuperAdmin()) {
          // Refrescar el UUID en la lista local de empresas para que la
          // preview muestre el nuevo logo sin necesidad de recargar.
          const id = this.selectedEmpresaId();
          this.empresas.update(list =>
            list.map(e => e.id === id ? { ...e, imagenUrl: res.uuid } : e)
          );
          this.selectedEmpresaLogoUrl.set(`${environment.apiUrl}/archivos/${res.uuid}`);
        }
        // Para ambos roles: actualizar el sidebar de la sesión actual.
        // patchUserLogoUuid() solo escribe LOGO_PERSIST_KEY si schema === 'public',
        // por lo que los ADMIN no contaminan la clave global del SUPERADMINISTRADOR.
        this.empresaCurrent.applyLogoUuid(res.uuid);
        this.toast.success('Logo actualizado correctamente.', 'Logo guardado');
      },
      error: (err) => {
        this.loading.set(false);
        const msg = err.error?.message || err.message || 'Error al subir el logo.';
        this.error.set(msg);
        this.toast.error(msg, 'Error');
      },
    });
  }

  // ── Facturación electrónica: configuración por empresa ──────────

  private loadFeConfig(): void {
    this.feLoading.set(true);
    this.feError.set(null);
    this.feConfigService.getConfig().subscribe({
      next: (cfg) => {
        this.feLoading.set(false);
        this.feConfig.set(cfg);
      },
      error: (err) => {
        this.feLoading.set(false);
        const msg = err.error?.message || err.message || 'No se pudo cargar la configuración de facturación electrónica.';
        this.feError.set(msg);
      },
    });
  }

  saveFeConfig(): void {
    const cfg = this.feConfig();
    if (!cfg) return;
    this.feSaving.set(true);
    this.feError.set(null);
    this.feConfigService.updateConfig(cfg).subscribe({
      next: (updated) => {
        this.feSaving.set(false);
        this.feConfig.set(updated);
        this.toast.success('Configuración de facturación electrónica guardada.', 'DIAN');
      },
      error: (err) => {
        this.feSaving.set(false);
        const msg = err.error?.message || err.message || 'Error al guardar la configuración de facturación electrónica.';
        this.feError.set(msg);
        this.toast.error(msg, 'DIAN');
      },
    });
  }

  toggleFeActiva(value: boolean): void {
    const current = this.feConfig() ?? {};
    this.feConfig.set({ ...current, facturacionActiva: value });
  }
}
