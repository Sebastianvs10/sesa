import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { PacienteService, PacienteDto } from '../../core/services/paciente.service';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { SesaFormFieldComponent } from '../../shared/components/sesa-form-field/sesa-form-field.component';

@Component({
  standalone: true,
  selector: 'sesa-buscar-paciente-page',
  imports: [CommonModule, FormsModule, SesaCardComponent, SesaFormFieldComponent],
  templateUrl: './buscar-paciente.page.html',
  styleUrl: './buscar-paciente.page.scss',
})
export class BuscarPacientePageComponent {
  private readonly pacienteService = inject(PacienteService);
  private readonly router = inject(Router);

  // Búsqueda
  documento = '';
  pacientesEncontrados: PacienteDto[] = [];
  buscando = false;
  errorBusqueda: string | null = null;
  mostrarFormularioCrear = false;

  // Creación
  nuevoDocumento = '';
  nuevoNombre = '';
  nuevoApellido = '';
  nuevoTelefono = '';
  nuevoSexo = '';
  nuevaFechaNacimiento = '';
  nuevoTipoDocumento = 'CC';
  creando = false;
  errorCreacion: string | null = null;

  buscarPaciente(): void {
    if (!this.documento.trim()) {
      this.errorBusqueda = 'Ingresa un número de documento';
      return;
    }

    this.buscando = true;
    this.errorBusqueda = null;
    this.pacientesEncontrados = [];

    this.pacienteService.list(0, 100, this.documento.trim()).subscribe({
      next: (res) => {
        this.pacientesEncontrados = res.content ?? [];
        if (this.pacientesEncontrados.length === 0) {
          this.errorBusqueda = 'No se encontró paciente. ¿Crear uno nuevo?';
          this.mostrarFormularioCrear = true;
        }
        this.buscando = false;
      },
      error: (err) => {
        this.errorBusqueda = err.error?.error || 'Error al buscar';
        this.buscando = false;
      },
    });
  }

  seleccionarPaciente(paciente: PacienteDto): void {
    this.router.navigate(['/historia-clinica', paciente.id, 'nueva']);
  }

  crearPaciente(): void {
    if (!this.nuevoDocumento.trim() || !this.nuevoNombre.trim()) {
      this.errorCreacion = 'Documento y nombre son requeridos';
      return;
    }

    this.creando = true;
    this.errorCreacion = null;

    this.pacienteService
      .create({
        tipoDocumento: this.nuevoTipoDocumento,
        documento: this.nuevoDocumento.trim(),
        nombres: this.nuevoNombre.trim(),
        apellidos: this.nuevoApellido.trim() || undefined,
        telefono: this.nuevoTelefono.trim() || undefined,
        sexo: this.nuevoSexo || undefined,
        fechaNacimiento: this.nuevaFechaNacimiento || undefined,
        activo: true,
      })
      .subscribe({
        next: (paciente) => {
          this.creando = false;
          this.router.navigate(['/historia-clinica', paciente.id, 'nueva']);
        },
        error: (err) => {
          this.errorCreacion = err.error?.error || 'Error al crear paciente';
          this.creando = false;
        },
      });
  }

  toggleFormularioCrear(): void {
    this.mostrarFormularioCrear = !this.mostrarFormularioCrear;
    this.errorCreacion = null;
  }

  calcularEdad(fechaNacimiento?: string): number | null {
    if (!fechaNacimiento) return null;
    const hoy = new Date();
    const nacimiento = new Date(fechaNacimiento);
    let edad = hoy.getFullYear() - nacimiento.getFullYear();
    const mesActual = hoy.getMonth();
    const mesNacimiento = nacimiento.getMonth();
    if (mesActual < mesNacimiento || (mesActual === mesNacimiento && hoy.getDate() < nacimiento.getDate())) {
      edad--;
    }
    return edad;
  }
}
