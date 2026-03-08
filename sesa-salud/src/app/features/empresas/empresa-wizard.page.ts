import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  faPenToSquare,
  faBuilding,
  faArrowLeft,
  faChevronRight,
  faClipboardList,
  faPhone,
  faBuildingColumns,
  faGear,
  faUsers,
  faMobileScreen,
  faGlobe,
  faTriangleExclamation,
  faUser,
  faLock,
  faFloppyDisk,
  faWandMagicSparkles,
} from '@fortawesome/free-solid-svg-icons';
import { EmpresaService, EmpresaCreateRequest, AdminUserRequest, ModuloDto } from '../../core/services/empresa.service';
import { IgacService, IgacDepartamento, IgacMunicipio } from '../../core/services/igac.service';
import { SesaFormFieldComponent } from '../../shared/components/sesa-form-field/sesa-form-field.component';
import { SesaPageHeaderComponent } from '../../shared/components/sesa-page-header/sesa-page-header.component';

@Component({
  standalone: true,
  selector: 'sesa-empresa-wizard-page',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    FontAwesomeModule,
    SesaFormFieldComponent,
    SesaPageHeaderComponent,
  ],
  templateUrl: './empresa-wizard.page.html',
  styleUrl: './empresa-wizard.page.scss',
})
export class EmpresaWizardPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly empresaService = inject(EmpresaService);
  private readonly igacService = inject(IgacService);

  faPenToSquare = faPenToSquare;
  faBuilding = faBuilding;
  faArrowLeft = faArrowLeft;
  faChevronRight = faChevronRight;
  faClipboardList = faClipboardList;
  faPhone = faPhone;
  faBuildingColumns = faBuildingColumns;
  faGear = faGear;
  faUsers = faUsers;
  faMobileScreen = faMobileScreen;
  faGlobe = faGlobe;
  faTriangleExclamation = faTriangleExclamation;
  faUser = faUser;
  faLock = faLock;
  faFloppyDisk = faFloppyDisk;
  faWandMagicSparkles = faWandMagicSparkles;

  step = 0;
  isEdit = false;
  id: number | null = null;
  loading = false;
  error: string | null = null;

  /** Catálogo cargado desde el backend (BD) */
  modulos: ModuloDto[] = [];
  loadingModulos = true;
  modulosError: string | null = null;

  /** Departamentos y municipios desde IGAC (BD) */
  departamentos = signal<IgacDepartamento[]>([]);
  municipios = signal<IgacMunicipio[]>([]);
  loadingMunicipios = false;

  /** Códigos seleccionados */
  moduloCodigos: string[] = [];
  submoduloCodigos: string[] = [];

  /** Para colapsar/expandir submódulos por módulo */
  expandedModulos: Set<string> = new Set();

  formGeneral: FormGroup;
  formAdmin: FormGroup;

  steps = [
    'Información general de la empresa',
    'Módulos y submódulos',
    'Número de usuarios',
    'Usuario administrador',
  ];

  constructor() {
    this.formGeneral = this.fb.group({
      schemaName: ['', [Validators.required, Validators.pattern(/^[a-z0-9_]+$/)]],
      razonSocial: ['', [Validators.required]],
      telefono: [''],
      segundoTelefono: [''],
      identificacion: [''],
      direccionEmpresa: [''],
      tipoDocumento: [''],
      regimen: [''],
      numeroDivipola: [''],
      pais: ['Colombia'],
      departamento: [''],
      municipio: [''],
      usuarioMovilLimit: [0, [Validators.min(0)]],
      usuarioWebLimit: [0, [Validators.min(0)]],
    });
    this.formAdmin = this.fb.group({
      identificacion: ['', [Validators.required]],
      primerNombre: ['', [Validators.required]],
      segundoNombre: [''],
      primerApellido: ['', [Validators.required]],
      segundoApellido: [''],
      telefonoCelular: ['', [Validators.required]],
      correo: ['', [Validators.required, Validators.email]],
      contraseña: ['', [Validators.required, Validators.minLength(6)]],
      repetirContraseña: ['', [Validators.required]],
    });
  }

  ngOnInit(): void {
    this.loadDepartamentos();
    this.loadingModulos = true;
    this.modulosError = null;
    this.empresaService.getModulos().subscribe({
      next: (mods) => {
        this.modulos = mods ?? [];
        this.loadingModulos = false;
        this.loadEmpresaIfEdit();
      },
      error: (err) => {
        this.modulos = [];
        this.loadingModulos = false;
        this.modulosError = err.status === 403
          ? 'Sin permiso para cargar módulos. Inicie sesión como administrador.'
          : err.error?.error || err.message || 'No se pudo cargar el catálogo de módulos desde la base de datos.';
        this.loadEmpresaIfEdit();
      },
    });
  }

  private loadDepartamentos(): void {
    this.igacService.listDepartamentos().subscribe({
      next: (list) => {
        this.departamentos.set(list ?? []);
        if (this.formGeneral.get('departamento')?.value) {
          this.loadMunicipiosForCurrentDepartamento();
        }
      },
      error: () => this.departamentos.set([]),
    });
  }

  onDepartamentoChange(): void {
    const nombreDepto = this.formGeneral.get('departamento')?.value;
    this.formGeneral.patchValue({ municipio: '' });
    this.municipios.set([]);
    if (!nombreDepto) return;
    const depto = this.departamentos().find((d) => d.nombre === nombreDepto);
    if (!depto?.codigoDane) return;
    this.loadingMunicipios = true;
    this.igacService.listMunicipios(depto.codigoDane).subscribe({
      next: (list) => {
        this.municipios.set(list ?? []);
        this.loadingMunicipios = false;
      },
      error: () => {
        this.municipios.set([]);
        this.loadingMunicipios = false;
      },
    });
  }

  private loadMunicipiosForCurrentDepartamento(): void {
    const nombreDepto = this.formGeneral.get('departamento')?.value;
    if (!nombreDepto) return;
    const depto = this.departamentos().find((d) => d.nombre === nombreDepto);
    if (!depto?.codigoDane) return;
    this.igacService.listMunicipios(depto.codigoDane).subscribe({
      next: (list) => this.municipios.set(list ?? []),
      error: () => this.municipios.set([]),
    });
  }

  private loadEmpresaIfEdit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam && idParam !== 'nueva') {
      this.isEdit = true;
      this.id = +idParam;
      this.empresaService.get(this.id).subscribe({
        next: (e) => {
          this.formAdmin.patchValue({
            correo: e.adminCorreo ?? '',
            identificacion: e.adminIdentificacion ?? '',
            primerNombre: e.adminPrimerNombre ?? '',
            segundoNombre: e.adminSegundoNombre ?? '',
            primerApellido: e.adminPrimerApellido ?? '',
            segundoApellido: e.adminSegundoApellido ?? '',
            telefonoCelular: e.adminCelular ?? '',
          });
          if (this.isEdit) {
            this.formAdmin.get('contraseña')?.clearValidators();
            this.formAdmin.get('contraseña')?.updateValueAndValidity();
            this.formAdmin.get('repetirContraseña')?.clearValidators();
            this.formAdmin.get('repetirContraseña')?.updateValueAndValidity();
            this.formAdmin.get('identificacion')?.clearValidators();
            this.formAdmin.get('identificacion')?.updateValueAndValidity();
            this.formAdmin.get('primerNombre')?.clearValidators();
            this.formAdmin.get('primerNombre')?.updateValueAndValidity();
            this.formAdmin.get('primerApellido')?.clearValidators();
            this.formAdmin.get('primerApellido')?.updateValueAndValidity();
            this.formAdmin.get('telefonoCelular')?.clearValidators();
            this.formAdmin.get('telefonoCelular')?.updateValueAndValidity();
          }
          this.formGeneral.patchValue({
            schemaName: e.schemaName,
            razonSocial: e.razonSocial,
            telefono: e.telefono,
            segundoTelefono: e.segundoTelefono,
            identificacion: e.identificacion,
            direccionEmpresa: e.direccionEmpresa,
            tipoDocumento: e.tipoDocumento,
            regimen: e.regimen,
            numeroDivipola: e.numeroDivipola,
            pais: e.pais ?? 'Colombia',
            departamento: e.departamento,
            municipio: e.municipio,
            usuarioMovilLimit: e.usuarioMovilLimit ?? 0,
            usuarioWebLimit: e.usuarioWebLimit ?? 0,
          });
          this.moduloCodigos = e.moduloCodigos ?? [];
          this.submoduloCodigos = e.submoduloCodigos ?? [];
          this.loadMunicipiosForCurrentDepartamento();
        },
        error: () => this.router.navigate(['/empresas']),
      });
    }
  }

  /* ========== Módulo toggle (top-level) ========== */
  toggleModulo(codigo: string): void {
    const state = this.getModuloCheckState(codigo);
    // Si ya tiene alguna selección (partial o full), deseleccionar todo
    if (state === 'partial' || state === 'full') {
      // Deseleccionar módulo y todos sus submódulos
      this.moduloCodigos = this.moduloCodigos.filter(c => c !== codigo);
      const mod = this.modulos.find(m => m.codigo === codigo);
      if (mod) {
        const toRemove = new Set(mod.submodulos.map(s => s.codigo));
        this.submoduloCodigos = this.submoduloCodigos.filter(c => !toRemove.has(c));
      }
      this.expandedModulos.delete(codigo);
    } else {
      // Seleccionar módulo y todos sus submódulos (sin autoexpandir)
      this.moduloCodigos.push(codigo);
      const mod = this.modulos.find(m => m.codigo === codigo);
      if (mod) {
        for (const sub of mod.submodulos) {
          if (!this.submoduloCodigos.includes(sub.codigo)) {
            this.submoduloCodigos.push(sub.codigo);
          }
        }
      }
    }
  }

  /* ========== Submódulo toggle ========== */
  toggleSubmodulo(moduloCodigo: string, subCodigo: string): void {
    const si = this.submoduloCodigos.indexOf(subCodigo);
    if (si >= 0) {
      this.submoduloCodigos.splice(si, 1);
    } else {
      this.submoduloCodigos.push(subCodigo);
      // Asegurar que el módulo padre está seleccionado
      if (!this.moduloCodigos.includes(moduloCodigo)) {
        this.moduloCodigos.push(moduloCodigo);
      }
    }
  }

  /* ========== Expansión de submódulos ========== */
  toggleExpanded(codigo: string): void {
    if (this.expandedModulos.has(codigo)) {
      this.expandedModulos.delete(codigo);
    } else {
      this.expandedModulos.add(codigo);
    }
  }

  isExpanded(codigo: string): boolean {
    return this.expandedModulos.has(codigo);
  }

  expandAllModulos(): void {
    for (const m of this.modulos) {
      if (m.submodulos.length > 0) this.expandedModulos.add(m.codigo);
    }
  }

  collapseAllModulos(): void {
    this.expandedModulos.clear();
  }

  /** Seleccionar todos los módulos y sus submódulos (desde BD). */
  seleccionarTodosModulos(): void {
    this.moduloCodigos = this.modulos.map(m => m.codigo);
    this.submoduloCodigos = this.modulos.flatMap(m => m.submodulos.map(s => s.codigo));
    this.expandAllModulos();
  }

  /** Deseleccionar todos los módulos y submódulos. */
  deseleccionarTodosModulos(): void {
    this.moduloCodigos = [];
    this.submoduloCodigos = [];
    this.collapseAllModulos();
  }

  /* ========== Helpers de selección ========== */
  /** true si el módulo tiene al menos un submódulo seleccionado */
  isModuloSelected(codigo: string): boolean {
    return this.countSelectedSubs(codigo) > 0;
  }

  isSubmoduloSelected(codigo: string): boolean {
    return this.submoduloCodigos.includes(codigo);
  }

  /** Cuántos submódulos seleccionados tiene un módulo */
  countSelectedSubs(moduloCodigo: string): number {
    const mod = this.modulos.find(m => m.codigo === moduloCodigo);
    if (!mod) return 0;
    return mod.submodulos.filter(s => this.submoduloCodigos.includes(s.codigo)).length;
  }

  /** Estado del checkbox del módulo: 'none' | 'partial' | 'full' */
  getModuloCheckState(codigo: string): 'none' | 'partial' | 'full' {
    const mod = this.modulos.find(m => m.codigo === codigo);
    if (!mod || mod.submodulos.length === 0) return 'none';
    const selected = this.countSelectedSubs(codigo);
    if (selected === 0) return 'none';
    if (selected === mod.submodulos.length) return 'full';
    return 'partial';
  }

  /* ========== Wizard navigation ========== */
  goStep(s: number): void {
    if (s >= 0 && s < this.steps.length) this.step = s;
  }

  /** Porcentaje de progreso (0-100) para la barra y el texto */
  progressPercent(): number {
    return Math.round(((this.step + 1) / this.steps.length) * 100);
  }

  next(): void {
    if (this.step < this.steps.length - 1) this.step++;
    else this.submit();
  }

  submit(): void {
    this.error = null;
    if (this.formGeneral.invalid) {
      this.formGeneral.markAllAsTouched();
      this.error = 'Complete los campos obligatorios de los pasos anteriores.';
      return;
    }
    if (this.step === 3 && this.formAdmin.invalid) {
      if (this.formAdmin.get('contraseña')?.value !== this.formAdmin.get('repetirContraseña')?.value) {
        this.formAdmin.get('repetirContraseña')?.setErrors({ mismatch: true });
      }
      this.formAdmin.markAllAsTouched();
      this.error = 'Complete todos los campos obligatorios del usuario administrador (identificación, nombres, apellidos, teléfono celular, correo y contraseña).';
      return;
    }
    const general = this.formGeneral.getRawValue();
    const admin = this.formAdmin.getRawValue();
    let adminUser: AdminUserRequest;
    if (this.isEdit) {
      adminUser = {
        identificacion: admin.identificacion || '—',
        primerNombre: admin.primerNombre || '—',
        segundoNombre: admin.segundoNombre || '',
        primerApellido: admin.primerApellido || '—',
        segundoApellido: admin.segundoApellido || '',
        telefonoCelular: admin.telefonoCelular || '—',
        correo: admin.correo || '',
        contraseña: 'no-change',
      };
    } else {
      if (admin.contraseña !== admin.repetirContraseña) {
        this.formAdmin.get('repetirContraseña')?.setErrors({ mismatch: true });
        return;
      }
      adminUser = {
        identificacion: admin.identificacion,
        primerNombre: admin.primerNombre,
        segundoNombre: admin.segundoNombre || undefined,
        primerApellido: admin.primerApellido,
        segundoApellido: admin.segundoApellido || undefined,
        telefonoCelular: admin.telefonoCelular,
        correo: admin.correo,
        contraseña: admin.contraseña,
      };
    }
    const req: EmpresaCreateRequest = {
      ...general,
      moduloCodigos: this.moduloCodigos,
      submoduloCodigos: this.submoduloCodigos,
      adminUser,
    };
    this.loading = true;
    if (this.isEdit && this.id != null) {
      this.empresaService.update(this.id, req).subscribe({
        next: () => {
          this.loading = false;
          this.router.navigate(['/empresas']);
        },
        error: (e) => {
          this.error = e.error?.error || e.message || 'Error al actualizar';
          this.loading = false;
        },
      });
    } else {
      this.empresaService.create(req).subscribe({
        next: () => {
          this.loading = false;
          this.router.navigate(['/empresas']);
        },
        error: (e) => {
          if (e.status === 403) {
            this.error = 'Sin permiso para crear empresas. Cierre sesión e inicie con la cuenta de administrador (admin@sesa.local / Admin123!).';
          } else {
            this.error = e.error?.error || e.message || 'Error al crear empresa';
          }
          this.loading = false;
        },
      });
    }
  }

  getSchemaError(): string {
    const ctrl = this.formGeneral.get('schemaName');
    if (!ctrl?.touched || !ctrl?.invalid) return '';
    if (ctrl.hasError('required')) return 'Requerido';
    if (ctrl.hasError('pattern')) return 'Solo minúsculas, números y guión bajo';
    return 'Requerido';
  }
}
