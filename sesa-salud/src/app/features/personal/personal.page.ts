/**
 * Gestión de Personal — confirm dialog, toast CRUD, skeleton loading.
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
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
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';
import { SesaConfirmDialogService } from '../../shared/components/sesa-confirm-dialog/sesa-confirm-dialog.component';

@Component({
  standalone: true,
  selector: 'sesa-personal-page',
  imports: [CommonModule, FormsModule, SesaCardComponent],
  templateUrl: './personal.page.html',
  styleUrl: './personal.page.scss',
})
export class PersonalPageComponent implements OnInit {
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
  rolesPersonal = ROLES_PERSONAL;
  /** Roles seleccionados en el formulario (multi-rol). */
  selectedRoles: string[] = [];
  fotoFile: File | null = null;
  firmaFile: File | null = null;
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
    this.empresaService.list(0, 500).subscribe({
      next: (res) => {
        this.empresas = res.content ?? [];
        this.loadingEmpresas = false;
        if (!this.selectedSchema) {
          this.selectedSchema = this.empresas.length > 0
            ? this.empresas[0].schemaName
            : 'public';
          this.load();
        }
      },
      error: () => {
        this.loadingEmpresas = false;
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
    this.form = this.getEmptyForm();
    this.selectedRoles = [];
    this.fotoFile = null;
    this.firmaFile = null;
    this.saveError = null;
  }

  openEdit(p: PersonalDto): void {
    this.editingId = p.id;
    this.showForm = true;
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
    this.saveError = null;
  }

  onFotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.fotoFile = input.files?.[0] ?? null;
  }

  onFirmaSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.firmaFile = input.files?.[0] ?? null;
  }

  cancelForm(): void {
    this.editingId = null;
    this.showForm = false;
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
          this.saving = false;
          this.editingId = null;
          this.showForm = false;
          this.toast.success(isCreate ? 'Personal creado.' : 'Personal actualizado.', 'Guardado');
          this.load();
          return;
        }
        forkJoin(uploads).subscribe({
          next: () => {
            this.saving = false;
            this.editingId = null;
            this.showForm = false;
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
      ODONTOLOGO: 'odontologo',
      BACTERIOLOGO: 'bacteriologo',
      ENFERMERO: 'enfermero',
      JEFE_ENFERMERIA: 'jefe-enfermeria',
      AUXILIAR_ENFERMERIA: 'auxiliar',
      PSICOLOGO: 'psicologo',
      REGENTE_FARMACIA: 'farmacia',
      RECEPCIONISTA: 'recepcionista',
      ADMIN: 'admin',
      SUPERADMINISTRADOR: 'super',
    };
    return map[(rol ?? '').toUpperCase()] || 'default';
  }
}

