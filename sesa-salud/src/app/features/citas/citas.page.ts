import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faCalendarDays, faCheck, faCircleCheck, faXmark } from '@fortawesome/free-solid-svg-icons';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { SesaFormFieldComponent } from '../../shared/components/sesa-form-field/sesa-form-field.component';
import { CitaDto, CitaService } from '../../core/services/cita.service';
import { PacienteDto, PacienteService } from '../../core/services/paciente.service';
import { PersonalDto, PersonalService } from '../../core/services/personal.service';

@Component({
  standalone: true,
  selector: 'sesa-citas-page',
  imports: [
    CommonModule,
    FormsModule,
    FontAwesomeModule,
    SesaCardComponent,
    SesaFormFieldComponent,
  ],
  templateUrl: './citas.page.html',
  styleUrl: './citas.page.scss',
})
export class CitasPageComponent implements OnInit {
  private readonly citaService = inject(CitaService);
  private readonly pacienteService = inject(PacienteService);
  private readonly personalService = inject(PersonalService);
  private readonly router = inject(Router);

  cargando = false;
  error: string | null = null;
  exito: string | null = null;

  pacientes: PacienteDto[] = [];
  profesionales: PersonalDto[] = [];
  citasHoy: CitaDto[] = [];
  especialidades = [
    'Medicina general',
    'Medicina interna',
    'Pediatría',
    'Ginecología',
    'Cardiología',
    'Neurología',
    'Ortopedia',
    'Dermatología',
  ];

  form = {
    pacienteId: null as number | null,
    profesionalId: null as number | null,
    servicio: '',
    fechaHora: '',
  };
  reprogramar = {
    citaId: null as number | null,
    nuevaFechaHora: '',
  };

  faCalendarDays = faCalendarDays;
  faCheck = faCheck;
  faCircleCheck = faCircleCheck;
  faXmark = faXmark;

  get fechaHoyFormateada(): string {
    const d = new Date();
    return d.toLocaleDateString('es-CO', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
  }

  get citasPendientes(): number {
    return this.citasHoy.filter(c => c.estado === 'PENDIENTE' || c.estado === 'CONFIRMADA').length;
  }

  ngOnInit(): void {
    this.cargarCatalogos();
    this.cargarCitasHoy();
  }

  private cargarCatalogos(): void {
    this.pacienteService.list(0, 500).subscribe({
      next: (res) => (this.pacientes = res.content ?? []),
      error: () => (this.pacientes = []),
    });
    this.personalService.list(0, 500).subscribe({
      next: (res) => (this.profesionales = res.content ?? []),
      error: () => (this.profesionales = []),
    });
  }

  private cargarCitasHoy(): void {
    this.cargando = true;
    this.error = null;
    const d = new Date();
    const hoy = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

    this.citaService.list(hoy).subscribe({
      next: (citas) => {
        this.citasHoy = citas ?? [];
        this.cargando = false;
      },
      error: (err) => {
        this.error = err?.error?.error || 'No se pudieron cargar las citas';
        this.cargando = false;
      },
    });
  }

  crearCita(): void {
    if (!this.form.pacienteId || !this.form.profesionalId || !this.form.servicio || !this.form.fechaHora) {
      this.error = 'Completa paciente, profesional, especialidad y fecha/hora';
      return;
    }
    this.error = null;
    this.exito = null;
    this.citaService.create({
      pacienteId: this.form.pacienteId,
      profesionalId: this.form.profesionalId,
      servicio: this.form.servicio,
      fechaHora: this.form.fechaHora,
      estado: 'PENDIENTE',
    }).subscribe({
      next: () => {
        this.exito = 'Cita creada. Continúa en historia clínica del paciente.';
        this.cargarCitasHoy();
      },
      error: (err) => {
        this.error = err?.error?.error || 'No se pudo crear la cita';
      },
    });
  }

  confirmarCita(cita: CitaDto): void {
    this.actualizarEstado(cita, 'CONFIRMADA');
  }

  marcarAtendido(cita: CitaDto): void {
    this.actualizarEstado(cita, 'ATENDIDO');
  }

  cancelarCita(cita: CitaDto): void {
    this.actualizarEstado(cita, 'CANCELADA');
  }

  reprogramarCita(): void {
    if (!this.reprogramar.citaId || !this.reprogramar.nuevaFechaHora) {
      this.error = 'Selecciona cita y nueva fecha/hora para reprogramar';
      return;
    }
    const cita = this.citasHoy.find((c) => c.id === this.reprogramar.citaId);
    if (!cita) {
      this.error = 'Cita no encontrada';
      return;
    }
    this.citaService.update(cita.id, {
      pacienteId: cita.pacienteId,
      profesionalId: cita.profesionalId,
      servicio: cita.servicio,
      fechaHora: this.reprogramar.nuevaFechaHora,
      estado: 'REPROGRAMADA',
      notas: cita.notas,
    }).subscribe({
      next: () => {
        this.exito = 'Cita reprogramada';
        this.reprogramar.citaId = null;
        this.reprogramar.nuevaFechaHora = '';
        this.cargarCitasHoy();
      },
      error: (err) => (this.error = err?.error?.error || 'No se pudo reprogramar'),
    });
  }

  private actualizarEstado(cita: CitaDto, estado: string): void {
    this.error = null;
    this.exito = null;
    this.citaService.update(cita.id, {
      pacienteId: cita.pacienteId,
      profesionalId: cita.profesionalId,
      servicio: cita.servicio,
      fechaHora: cita.fechaHora,
      estado,
      notas: cita.notas,
    }).subscribe({
      next: () => {
        this.exito = `Cita ${estado.toLowerCase()}`;
        this.cargarCitasHoy();
      },
      error: (err) => (this.error = err?.error?.error || 'No se pudo actualizar la cita'),
    });
  }

  formatHora(fechaHora?: string): string {
    if (!fechaHora) return '--:--';
    const fecha = new Date(fechaHora);
    if (Number.isNaN(fecha.getTime())) return '--:--';
    return fecha.toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit', hour12: false });
  }

  getEstadoClass(estado?: string): string {
    const e = (estado || 'PENDIENTE').toUpperCase();
    if (e === 'PENDIENTE') return 'sesa-badge-warning';
    if (e === 'CONFIRMADA') return 'sesa-badge-info';
    if (e === 'ATENDIDO') return 'sesa-badge-success';
    if (e === 'CANCELADA') return 'sesa-badge-danger';
    if (e === 'REPROGRAMADA') return 'sesa-badge-secondary';
    return 'sesa-badge-secondary';
  }

  abrirHistoriaDesdeCita(): void {
    if (!this.form.pacienteId) {
      this.error = 'Selecciona un paciente para continuar el flujo';
      return;
    }
    this.router.navigate(['/historia-clinica'], { queryParams: { pacienteId: this.form.pacienteId } });
  }

}

