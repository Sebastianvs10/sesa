/**
 * Gestión de Personal — confirm dialog, toast CRUD, skeleton loading.
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule } from '@angular/common';
import { Component, ElementRef, HostListener, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faUsers, faPlus } from '@fortawesome/free-solid-svg-icons';
import { SesaPageHeaderComponent } from '../../shared/components/sesa-page-header/sesa-page-header.component';
import { forkJoin, Observable } from 'rxjs';
import {
  PersonalService,
  PersonalDto,
  PersonalRequestDto,
  PageResponse,
  ROLES_PERSONAL,
} from '../../core/services/personal.service';
import { EmpresaService, EmpresaDto } from '../../core/services/empresa.service';
import { AuthService } from '../../core/services/auth.service';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';
import { SesaConfirmDialogService } from '../../shared/components/sesa-confirm-dialog/sesa-confirm-dialog.component';
import {
  SesaComboboxSelectComponent,
  SesaComboboxOption,
} from '../../shared/components/sesa-combobox-select/sesa-combobox-select.component';

@Component({
  standalone: true,
  selector: 'sesa-personal-page',
  imports: [
    CommonModule,
    FormsModule,
    FontAwesomeModule,
    SesaPageHeaderComponent,
    SesaComboboxSelectComponent,
  ],
  templateUrl: './personal.page.html',
  styleUrl: './personal.page.scss',
})
export class PersonalPageComponent implements OnInit, OnDestroy {
  @ViewChild('wizardHeadingTarget') private wizardHeadingTarget?: ElementRef<HTMLElement>;

  readonly faUsers = faUsers;
  readonly faPlus = faPlus;
  list: PersonalDto[] = [];
  totalElements = 0;
  page = 0;
  size = 20;
  searchQ = '';
  loading = false;
  error: string | null = null;
  saving = false;
  saveError: string | null = null;

  empresas: EmpresaDto[] = [];
  loadingEmpresas = false;
  selectedSchema: string | null = null;
  /** Solo SUPERADMINISTRADOR puede cambiar de empresa; el resto usa la empresa asociada al usuario */
  isSuperAdmin = false;

  editingId: number | null = null;
  showForm = false;
  /** Paso del formulario (0–3): reduce carga cognitiva en alta/edición. */
  formStep = 0;
  readonly formSteps: ReadonlyArray<{ id: string; title: string; subtitle: string }> = [
    { id: 'identity', title: 'Identidad', subtitle: 'Documento y nombre completo del integrante' },
    { id: 'contact', title: 'Contacto y acceso', subtitle: 'Medios de contacto y credenciales en el sistema' },
    { id: 'roles', title: 'Rol y archivos', subtitle: 'Permisos, foto y firma del profesional' },
    { id: 'compliance', title: 'Normativo y estado', subtitle: 'RETHUS, RIPS, ubicación, vínculo y activación' },
  ];
  readonly lastFormStepIndex = 3;
  /** Porcentaje de la barra fina bajo el wizard (paso actual / total). */
  get wizardProgressPercent(): number {
    return ((this.formStep + 1) / (this.lastFormStepIndex + 1)) * 100;
  }

  readonly comboDocTipoOptions: SesaComboboxOption[] = [
    { value: '', label: 'Seleccione tipo de documento' },
    { value: 'CC', label: 'CC — Cédula de Ciudadanía' },
    { value: 'CE', label: 'CE — Cédula de Extranjería' },
    { value: 'PA', label: 'PA — Pasaporte' },
    { value: 'PEP', label: 'PEP — Permiso Especial de Permanencia' },
    { value: 'TI', label: 'TI — Tarjeta de Identidad' },
    { value: 'RC', label: 'RC — Registro Civil' },
  ];

  readonly comboSexoOptions: SesaComboboxOption[] = [
    { value: '', label: 'Seleccione sexo biológico' },
    { value: 'M', label: 'Masculino' },
    { value: 'F', label: 'Femenino' },
  ];

  readonly comboVinculacionOptions: SesaComboboxOption[] = [
    { value: '', label: 'Seleccione tipo de vinculación' },
    { value: 'PLANTA', label: 'Planta' },
    { value: 'CONTRATO', label: 'Contrato' },
    { value: 'PRESTADOR', label: 'Prestador de servicios' },
    { value: 'CONVENIO', label: 'Convenio / Rotatorio' },
  ];

  empresaComboOptions: SesaComboboxOption[] = [];

  private refreshEmpresaComboOptions(): void {
    if (this.loadingEmpresas) {
      this.empresaComboOptions = [];
      return;
    }
    const opts: SesaComboboxOption[] = [
      { value: '', label: 'Seleccione empresa…' },
      { value: 'public', label: 'Public (administración)' },
    ];
    for (const e of this.empresas) {
      opts.push({
        value: e.schemaName,
        label: `${e.razonSocial} (${e.schemaName})`,
      });
    }
    this.empresaComboOptions = opts;
  }

  get schemaSelectValue(): string {
    return this.selectedSchema ?? '';
  }

  onEmpresaComboChange(v: string): void {
    this.selectedSchema = v === '' ? null : v;
    this.onEmpresaChange();
  }

  rolesPersonal = ROLES_PERSONAL;
  /** Roles seleccionados en el formulario (multi-rol). */
  selectedRoles: string[] = [];
  fotoFile: File | null = null;
  firmaFile: File | null = null;
  /** URL para el atributo `src` de la vista previa (p. ej. blob:). */
  fotoPreviewUrl: string | null = null;
  firmaPreviewUrl: string | null = null;
  private fotoPreviewObjectUrl: string | null = null;
  private firmaPreviewObjectUrl: string | null = null;
  form: PersonalRequestDto = {
    nombres: '', apellidos: '',
    tipoDocumento: '', identificacion: '',
    primerNombre: '', segundoNombre: '', primerApellido: '', segundoApellido: '',
    celular: '', email: '', password: '',
    rol: '', roles: [], activo: true,
    tarjetaProfesional: '', especialidadFormal: '', numeroRethus: '',
    fechaNacimiento: '', sexo: '',
    municipio: '', departamento: '',
    tipoVinculacion: '', fechaIngreso: '', fechaRetiro: '',
  };

  private readonly toast = inject(SesaToastService);
  private readonly confirmDialog = inject(SesaConfirmDialogService);

  constructor(
    private personalService: PersonalService,
    private empresaService: EmpresaService,
    private authService: AuthService,
  ) {}

  ngOnDestroy(): void {
    this.revokeArchivoPreviews();
    this.unlockIntegranteModalScroll();
  }

  private revokeArchivoPreviews(): void {
    this.clearFotoPreview();
    this.clearFirmaPreview();
  }

  private clearFotoPreview(): void {
    if (this.fotoPreviewObjectUrl) {
      URL.revokeObjectURL(this.fotoPreviewObjectUrl);
      this.fotoPreviewObjectUrl = null;
    }
    this.fotoPreviewUrl = null;
  }

  private clearFirmaPreview(): void {
    if (this.firmaPreviewObjectUrl) {
      URL.revokeObjectURL(this.firmaPreviewObjectUrl);
      this.firmaPreviewObjectUrl = null;
    }
    this.firmaPreviewUrl = null;
  }

  private setFotoPreviewFromBlob(blob: Blob): void {
    this.clearFotoPreview();
    if (!blob || blob.size === 0) return;
    this.fotoPreviewObjectUrl = URL.createObjectURL(blob);
    this.fotoPreviewUrl = this.fotoPreviewObjectUrl;
  }

  private setFirmaPreviewFromBlob(blob: Blob): void {
    this.clearFirmaPreview();
    if (!blob || blob.size === 0) return;
    this.firmaPreviewObjectUrl = URL.createObjectURL(blob);
    this.firmaPreviewUrl = this.firmaPreviewObjectUrl;
  }

  /** Carga foto/firma guardadas (BYTEA) al abrir edición. */
  private loadExistingArchivosPreview(personalId: number): void {
    this.loadExistingFotoPreview(personalId);
    this.loadExistingFirmaPreview(personalId);
  }

  private loadExistingFotoPreview(personalId: number): void {
    const schema = this.isSuperAdmin ? this.selectedSchema ?? undefined : undefined;
    this.personalService.getFotoBlob(personalId, schema).subscribe({
      next: (blob) => {
        if (this.editingId !== personalId || !this.showForm) return;
        this.setFotoPreviewFromBlob(blob);
      },
      error: () => {},
    });
  }

  private loadExistingFirmaPreview(personalId: number): void {
    const schema = this.isSuperAdmin ? this.selectedSchema ?? undefined : undefined;
    this.personalService.getFirmaBlob(personalId, schema).subscribe({
      next: (blob) => {
        if (this.editingId !== personalId || !this.showForm) return;
        this.setFirmaPreviewFromBlob(blob);
      },
      error: () => {},
    });
  }

  @HostListener('document:keydown.escape', ['$event'])
  onIntegranteModalEscape(e: KeyboardEvent): void {
    if (!this.showForm || this.saving) {
      return;
    }
    e.preventDefault();
    this.cancelForm();
  }

  ngOnInit(): void {
    const user = this.authService.currentUser();
    this.isSuperAdmin = user?.role === 'SUPERADMINISTRADOR';
    if (this.isSuperAdmin) {
      this.loadEmpresas();
    } else {
      this.selectedSchema = user?.schema ?? null;
      this.load();
    }
  }

  loadEmpresas(): void {
    this.loadingEmpresas = true;
    this.refreshEmpresaComboOptions();
    this.empresaService.list(0, 500).subscribe({
      next: (res) => {
        this.empresas = res.content ?? [];
        this.loadingEmpresas = false;
        this.refreshEmpresaComboOptions();
        if (!this.selectedSchema) {
          this.selectedSchema = this.empresas.length > 0
            ? this.empresas[0].schemaName
            : 'public';
          this.load();
        }
      },
      error: () => {
        this.loadingEmpresas = false;
        this.refreshEmpresaComboOptions();
      },
    });
  }

  onEmpresaChange(): void {
    this.page = 0;
    this.load();
  }

  load(): void {
    if (this.isSuperAdmin && !this.selectedSchema) {
      this.list = [];
      this.totalElements = 0;
      this.loading = false;
      return;
    }
    this.loading = true;
    this.error = null;
    const schema = this.isSuperAdmin ? this.selectedSchema : undefined;
    this.personalService
      .list(this.page, this.size, this.searchQ || undefined, schema)
      .subscribe({
        next: (res: PageResponse<PersonalDto>) => {
          this.list = res.content ?? [];
          this.totalElements = res.totalElements ?? 0;
          this.loading = false;
        },
        error: (err) => {
          this.error =
            err.error?.error || err.message || 'Error al cargar personal';
          this.loading = false;
        },
      });
  }

  search(): void {
    this.page = 0;
    this.load();
  }

  clearSearch(): void {
    this.searchQ = '';
    this.page = 0;
    this.load();
  }

  getEmptyForm(): PersonalRequestDto {
    return {
      nombres: '', apellidos: '',
      tipoDocumento: '', identificacion: '',
      primerNombre: '', segundoNombre: '', primerApellido: '', segundoApellido: '',
      celular: '', email: '', password: '',
      rol: '', roles: [], activo: true,
      tarjetaProfesional: '', especialidadFormal: '', numeroRethus: '',
      fechaNacimiento: '', sexo: '',
      municipio: '', departamento: '',
      tipoVinculacion: '', fechaIngreso: '', fechaRetiro: '',
    };
  }

  /** Devuelve true si el rol dado está en los roles seleccionados. */
  isRolSelected(rol: string): boolean {
    return this.selectedRoles.includes(rol);
  }

  /** Alterna la selección de un rol en el formulario. */
  toggleRol(rol: string): void {
    const idx = this.selectedRoles.indexOf(rol);
    if (idx === -1) {
      this.selectedRoles = [...this.selectedRoles, rol];
    } else {
      this.selectedRoles = this.selectedRoles.filter(r => r !== rol);
    }
    this.form.roles = [...this.selectedRoles];
    this.form.rol = this.selectedRoles[0] ?? '';
  }

  openCreate(): void {
    this.editingId = null;
    this.showForm = true;
    this.lockIntegranteModalScroll();
    this.formStep = 0;
    this.form = this.getEmptyForm();
    this.selectedRoles = [];
    this.fotoFile = null;
    this.firmaFile = null;
    this.revokeArchivoPreviews();
    this.saveError = null;
    setTimeout(() => this.focusWizardStepContext(), 0);
  }

  openEdit(p: PersonalDto): void {
    this.editingId = p.id;
    this.showForm = true;
    this.lockIntegranteModalScroll();
    this.formStep = 0;
    this.selectedRoles = p.roles ? [...p.roles] : (p.rol ? [p.rol] : []);
    this.form = {
      nombres: p.nombres,
      apellidos: p.apellidos ?? '',
      tipoDocumento: p.tipoDocumento ?? '',
      identificacion: p.identificacion ?? '',
      primerNombre: p.primerNombre ?? '',
      segundoNombre: p.segundoNombre ?? '',
      primerApellido: p.primerApellido ?? '',
      segundoApellido: p.segundoApellido ?? '',
      celular: p.celular ?? '',
      email: p.email ?? '',
      password: '',
      rol: this.selectedRoles[0] ?? '',
      roles: [...this.selectedRoles],
      activo: p.activo ?? true,
      tarjetaProfesional: p.tarjetaProfesional ?? '',
      especialidadFormal: p.especialidadFormal ?? '',
      numeroRethus: p.numeroRethus ?? '',
      fechaNacimiento: p.fechaNacimiento ?? '',
      sexo: p.sexo ?? '',
      municipio: p.municipio ?? '',
      departamento: p.departamento ?? '',
      tipoVinculacion: p.tipoVinculacion ?? '',
      fechaIngreso: p.fechaIngreso ?? '',
      fechaRetiro: p.fechaRetiro ?? '',
    };
    this.fotoFile = null;
    this.firmaFile = null;
    this.revokeArchivoPreviews();
    this.saveError = null;
    this.loadExistingArchivosPreview(p.id);
    setTimeout(() => this.focusWizardStepContext(), 0);
  }

  onFotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.fotoFile = file;
    if (file) {
      this.clearFotoPreview();
      this.fotoPreviewObjectUrl = URL.createObjectURL(file);
      this.fotoPreviewUrl = this.fotoPreviewObjectUrl;
    } else if (this.editingId != null) {
      this.loadExistingFotoPreview(this.editingId);
    } else {
      this.clearFotoPreview();
    }
  }

  onFirmaSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.firmaFile = file;
    if (file) {
      this.clearFirmaPreview();
      this.firmaPreviewObjectUrl = URL.createObjectURL(file);
      this.firmaPreviewUrl = this.firmaPreviewObjectUrl;
    } else if (this.editingId != null) {
      this.loadExistingFirmaPreview(this.editingId);
    } else {
      this.clearFirmaPreview();
    }
  }

  cancelForm(): void {
    this.revokeArchivoPreviews();
    this.fotoFile = null;
    this.firmaFile = null;
    this.editingId = null;
    this.showForm = false;
    this.formStep = 0;
    this.unlockIntegranteModalScroll();
  }

  private lockIntegranteModalScroll(): void {
    document.body.style.overflow = 'hidden';
  }

  private unlockIntegranteModalScroll(): void {
    document.body.style.overflow = '';
  }

  /** Vuelve a un paso anterior (desde la barra de progreso). */
  goToFormStep(i: number): void {
    if (i === this.formStep || i < 0 || i > this.lastFormStepIndex) return;
    if (i < this.formStep) {
      this.formStep = i;
      this.saveError = null;
      this.scrollFormPanelIntoView();
      this.focusWizardStepContext();
    }
  }

  previousFormStep(): void {
    if (this.formStep <= 0) return;
    this.formStep--;
    this.saveError = null;
    this.scrollFormPanelIntoView();
    this.focusWizardStepContext();
  }

  nextFormStep(): void {
    if (!this.validateFormStep(this.formStep)) return;
    if (this.formStep < this.lastFormStepIndex) {
      this.formStep++;
      this.saveError = null;
      this.scrollFormPanelIntoView();
      this.focusWizardStepContext();
    }
  }

  /**
   * Validación por paso antes de avanzar (mismas reglas que el guardado final donde aplica).
   */
  validateFormStep(step: number): boolean {
    switch (step) {
      case 0:
        if (!this.form.primerNombre?.trim() || !this.form.primerApellido?.trim()) {
          this.saveError = 'Indique primer nombre y primer apellido para continuar.';
          return false;
        }
        return true;
      case 1:
        if (!this.form.email?.trim()) {
          this.saveError = 'El correo electrónico es obligatorio.';
          return false;
        }
        if (this.editingId == null && !this.form.password?.trim()) {
          this.saveError = 'Ingrese la contraseña de acceso para continuar.';
          return false;
        }
        return true;
      case 2:
        if (this.selectedRoles.length === 0) {
          this.saveError = 'Seleccione al menos un rol para continuar.';
          return false;
        }
        return true;
      default:
        return true;
    }
  }

  private scrollFormPanelIntoView(): void {
    queueMicrotask(() => {
      const scrollEl = document.querySelector('.pp-integrante-modal-scroll');
      if (scrollEl) {
        scrollEl.scrollTo({ top: 0, behavior: 'smooth' });
      } else {
        document.querySelector('.pp-form-panel')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    });
  }

  /**
   * Tras cambiar de paso, enfoca el bloque de contexto del paso (teclado / lectores de pantalla).
   */
  private focusWizardStepContext(): void {
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        const el = this.wizardHeadingTarget?.nativeElement;
        el?.focus({ preventScroll: true });
      });
    });
  }

  save(): void {
    const nombres = [this.form.primerNombre, this.form.segundoNombre].filter(Boolean).join(' ').trim()
      || this.form.nombres?.trim();
    const apellidos = [this.form.primerApellido, this.form.segundoApellido].filter(Boolean).join(' ').trim()
      || this.form.apellidos?.trim();
    if (!nombres || !this.form.primerApellido?.trim()) {
      this.saveError = 'Primer nombre y primer apellido son obligatorios';
      return;
    }
    if (!this.form.email?.trim()) {
      this.saveError = 'Correo electrónico es obligatorio';
      return;
    }
    if (this.editingId == null && !this.form.password?.trim()) {
      this.saveError = 'La contraseña es obligatoria para el acceso';
      return;
    }
    if (this.selectedRoles.length === 0) {
      this.saveError = 'Seleccione al menos un rol';
      return;
    }
    if (this.isSuperAdmin && !this.selectedSchema) {
      this.saveError = 'Seleccione una empresa';
      return;
    }
    this.saving = true;
    this.saveError = null;
    const payload: PersonalRequestDto = {
      nombres,
      apellidos: apellidos || undefined,
      tipoDocumento: this.form.tipoDocumento?.trim() || undefined,
      identificacion: this.form.identificacion?.trim() || undefined,
      primerNombre: this.form.primerNombre?.trim() || undefined,
      segundoNombre: this.form.segundoNombre?.trim() || undefined,
      primerApellido: this.form.primerApellido?.trim() || undefined,
      segundoApellido: this.form.segundoApellido?.trim() || undefined,
      celular: this.form.celular?.trim() || undefined,
      email: this.form.email.trim(),
      password: this.form.password?.trim() || undefined,
      rol: this.selectedRoles[0],
      roles: [...this.selectedRoles],
      activo: this.form.activo ?? true,
      tarjetaProfesional: this.form.tarjetaProfesional?.trim() || undefined,
      especialidadFormal: this.form.especialidadFormal?.trim() || undefined,
      numeroRethus: this.form.numeroRethus?.trim() || undefined,
      fechaNacimiento: this.form.fechaNacimiento?.trim() || undefined,
      sexo: this.form.sexo?.trim() || undefined,
      municipio: this.form.municipio?.trim() || undefined,
      departamento: this.form.departamento?.trim() || undefined,
      tipoVinculacion: this.form.tipoVinculacion?.trim() || undefined,
      fechaIngreso: this.form.fechaIngreso?.trim() || undefined,
      fechaRetiro: this.form.fechaRetiro?.trim() || undefined,
    };
    const schema = this.isSuperAdmin ? this.selectedSchema : undefined;
    const isCreate = this.editingId == null;
    const req =
      isCreate
        ? this.personalService.create(payload, schema)
        : this.personalService.update(this.editingId!, payload, schema);
    req.subscribe({
      next: (createdOrUpdated) => {
        const id = createdOrUpdated.id;
        const uploads: Array<Observable<void>> = [];
        if (this.fotoFile) {
          uploads.push(this.personalService.uploadFoto(id, this.fotoFile, schema));
        }
        if (this.firmaFile) {
          uploads.push(this.personalService.uploadFirma(id, this.firmaFile, schema));
        }
        if (uploads.length === 0) {
          this.revokeArchivoPreviews();
          this.fotoFile = null;
          this.firmaFile = null;
          this.saving = false;
          this.editingId = null;
          this.showForm = false;
          this.unlockIntegranteModalScroll();
          this.toast.success(isCreate ? 'Personal creado.' : 'Personal actualizado.', 'Guardado');
          this.load();
          return;
        }
        forkJoin(uploads).subscribe({
          next: () => {
            this.revokeArchivoPreviews();
            this.fotoFile = null;
            this.firmaFile = null;
            this.saving = false;
            this.editingId = null;
            this.showForm = false;
            this.unlockIntegranteModalScroll();
            this.toast.success(isCreate ? 'Personal creado.' : 'Personal actualizado.', 'Guardado');
            this.load();
          },
          error: (err) => {
            this.saveError = err.error?.message || err.message || 'Error al subir foto/firma';
            this.saving = false;
            this.toast.error(this.saveError!, 'Error');
          },
        });
      },
      error: (err) => {
        this.saveError =
          err.error?.message || err.error?.error || err.message || 'Error al guardar';
        this.saving = false;
        this.toast.error(this.saveError!, 'Error');
      },
    });
  }

  async delete(p: PersonalDto): Promise<void> {
    const nombre = [p.nombres, p.apellidos].filter(Boolean).join(' ');
    const ok = await this.confirmDialog.confirm({
      title: 'Eliminar personal',
      message: `¿Estás seguro de eliminar a "${nombre}"? Esta acción no se puede deshacer.`,
      type: 'danger',
    });
    if (!ok) return;
    const schema = this.isSuperAdmin ? this.selectedSchema : undefined;
    this.personalService.delete(p.id, schema).subscribe({
      next: () => {
        this.toast.success(`"${nombre}" eliminado correctamente.`, 'Eliminado');
        this.load();
      },
      error: (e) => {
        const msg = e.error?.error || 'Error al eliminar';
        this.toast.error(msg, 'Error');
      },
    });
  }

  nextPage(): void {
    if ((this.page + 1) * this.size < this.totalElements) {
      this.page++;
      this.load();
    }
  }

  prevPage(): void {
    if (this.page > 0) {
      this.page--;
      this.load();
    }
  }

  fullName(p: PersonalDto): string {
    return [p.nombres, p.apellidos].filter(Boolean).join(' ');
  }

  initials(p: PersonalDto): string {
    const first = (p.nombres || '').trim().charAt(0).toUpperCase();
    const last  = (p.apellidos || '').trim().charAt(0).toUpperCase();
    return (first + last) || '?';
  }

  avatarStyle(name: string): Record<string, string> {
    const palettes: [string, string][] = [
      ['#1f6ae1', '#2bb0a6'],
      ['#7c3aed', '#a855f7'],
      ['#059669', '#10b981'],
      ['#d97706', '#f59e0b'],
      ['#dc2626', '#f87171'],
      ['#0891b2', '#38bdf8'],
      ['#db2777', '#f472b6'],
    ];
    const idx = (name.charCodeAt(0) || 0) % palettes.length;
    const [from, to] = palettes[idx];
    return { background: `linear-gradient(135deg, ${from}, ${to})` };
  }

  roleClass(rol: string | undefined): string {
    const map: Record<string, string> = {
      MEDICO: 'medico',
      COORDINADOR_MEDICO: 'coordinador',
      EBS: 'ebs',
      COORDINADOR_TERRITORIAL: 'coordinador-territorial',
      SUPERVISOR_APS: 'supervisor-aps',
      ODONTOLOGO: 'odontologo',
      BACTERIOLOGO: 'bacteriologo',
      ENFERMERO: 'enfermero',
      JEFE_ENFERMERIA: 'jefe-enfermeria',
      AUXILIAR_ENFERMERIA: 'auxiliar',
      PSICOLOGO: 'psicologo',
      REGENTE_FARMACIA: 'farmacia',
      FACTURACION: 'recepcionista',
      RECEPCIONISTA: 'recepcionista',
      ADMIN: 'admin',
      SUPERADMINISTRADOR: 'super',
    };
    return map[(rol ?? '').toUpperCase()] || 'default';
  }
}

