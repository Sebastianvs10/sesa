import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
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
  fotoFile: File | null = null;
  firmaFile: File | null = null;
  form: PersonalRequestDto = {
    nombres: '', apellidos: '', cargo: '', servicio: '', turno: '',
    identificacion: '', primerNombre: '', segundoNombre: '', primerApellido: '', segundoApellido: '',
    celular: '', email: '', password: '', rol: '', institucionPrestadora: '', activo: true,
  };

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
      nombres: '',
      apellidos: '',
      cargo: '',
      servicio: '',
      turno: '',
      identificacion: '',
      primerNombre: '',
      segundoNombre: '',
      primerApellido: '',
      segundoApellido: '',
      celular: '',
      email: '',
      password: '',
      rol: '',
      institucionPrestadora: '',
      activo: true,
    };
  }

  openCreate(): void {
    this.editingId = null;
    this.showForm = true;
    this.form = this.getEmptyForm();
    this.fotoFile = null;
    this.firmaFile = null;
    this.saveError = null;
  }

  openEdit(p: PersonalDto): void {
    this.editingId = p.id;
    this.showForm = true;
    this.form = {
      nombres: p.nombres,
      apellidos: p.apellidos ?? '',
      cargo: p.cargo,
      servicio: p.servicio ?? '',
      turno: p.turno ?? '',
      identificacion: p.identificacion ?? '',
      primerNombre: p.primerNombre ?? '',
      segundoNombre: p.segundoNombre ?? '',
      primerApellido: p.primerApellido ?? '',
      segundoApellido: p.segundoApellido ?? '',
      celular: p.celular ?? '',
      email: p.email ?? '',
      password: '',
      rol: p.rol ?? '',
      institucionPrestadora: p.institucionPrestadora ?? '',
      activo: p.activo ?? true,
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
      this.saveError = 'Primer nombre, primer apellido y cargo son obligatorios';
      return;
    }
    if (!this.form.cargo?.trim()) {
      this.saveError = 'Cargo es obligatorio';
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
    if (!this.form.rol?.trim()) {
      this.saveError = 'Seleccione un rol';
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
      cargo: this.form.cargo.trim(),
      servicio: this.form.servicio?.trim() || undefined,
      turno: this.form.turno?.trim() || undefined,
      identificacion: this.form.identificacion?.trim() || undefined,
      primerNombre: this.form.primerNombre?.trim() || undefined,
      segundoNombre: this.form.segundoNombre?.trim() || undefined,
      primerApellido: this.form.primerApellido?.trim() || undefined,
      segundoApellido: this.form.segundoApellido?.trim() || undefined,
      celular: this.form.celular?.trim() || undefined,
      email: this.form.email.trim(),
      password: this.form.password?.trim() || undefined,
      rol: this.form.rol.trim(),
      institucionPrestadora: this.form.institucionPrestadora?.trim() || undefined,
      activo: this.form.activo ?? true,
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
          this.load();
          return;
        }
        forkJoin(uploads).subscribe({
          next: () => {
            this.saving = false;
            this.editingId = null;
            this.showForm = false;
            this.load();
          },
          error: (err) => {
            this.saveError = err.error?.message || err.message || 'Error al subir foto/firma';
            this.saving = false;
          },
        });
      },
      error: (err) => {
        this.saveError =
          err.error?.message || err.error?.error || err.message || 'Error al guardar';
        this.saving = false;
      },
    });
  }

  delete(p: PersonalDto): void {
    const nombre = [p.nombres, p.apellidos].filter(Boolean).join(' ');
    if (!confirm(`¿Eliminar a "${nombre}"?`)) return;
    const schema = this.isSuperAdmin ? this.selectedSchema : undefined;
    this.personalService.delete(p.id, schema).subscribe({
      next: () => this.load(),
      error: (e) => alert(e.error?.error || 'Error al eliminar'),
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
}

