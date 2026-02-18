import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { PacienteDto, PacienteService } from '../../core/services/paciente.service';
import { PersonalDto, PersonalService } from '../../core/services/personal.service';
import { CitaDto, CitaService } from '../../core/services/cita.service';
import { ConsultaDto, ConsultaService } from '../../core/services/consulta.service';
import { OrdenClinicaDto, OrdenClinicaService } from '../../core/services/orden-clinica.service';
import { FacturaDto, FacturaService } from '../../core/services/factura.service';
import { ReporteResumenDto, ReporteService } from '../../core/services/reporte.service';

@Component({
  standalone: true,
  selector: 'sesa-flujo-clinico-page',
  imports: [CommonModule, FormsModule, SesaCardComponent],
  templateUrl: './flujo-clinico.page.html',
  styleUrl: './flujo-clinico.page.scss',
})
export class FlujoClinicoPageComponent implements OnInit {
  private readonly pacienteService = inject(PacienteService);
  private readonly personalService = inject(PersonalService);
  private readonly citaService = inject(CitaService);
  private readonly consultaService = inject(ConsultaService);
  private readonly ordenService = inject(OrdenClinicaService);
  private readonly facturaService = inject(FacturaService);
  private readonly reporteService = inject(ReporteService);

  pacientes: PacienteDto[] = [];
  profesionales: PersonalDto[] = [];
  selectedPacienteId: number | null = null;

  citas: CitaDto[] = [];
  consultas: ConsultaDto[] = [];
  ordenes: OrdenClinicaDto[] = [];
  facturas: FacturaDto[] = [];
  resumen: ReporteResumenDto | null = null;

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

  catalogoLaboratorios = [
    'Hemograma completo',
    'Química sanguínea',
    'Perfil lipídico',
    'Glicemia',
    'TSH',
    'Parcial de orina',
  ];

  catalogoMedicamentos = [
    'Acetaminofén 500 mg',
    'Ibuprofeno 400 mg',
    'Amoxicilina 500 mg',
    'Omeprazol 20 mg',
    'Losartán 50 mg',
    'Metformina 850 mg',
  ];

  loading = false;
  error: string | null = null;
  ok: string | null = null;

  citaForm = {
    profesionalId: null as number | null,
    servicio: '',
    fechaHora: '',
  };

  consultaForm = {
    citaId: null as number | null,
    profesionalId: null as number | null,
    motivoConsulta: '',
    enfermedadActual: '',
    antecedentesPersonales: '',
    antecedentesFamiliares: '',
    alergias: '',
  };

  ordenForm = {
    consultaId: null as number | null,
    tipo: 'LABORATORIO',
    plantilla: '' as string,
    detalle: '',
    valorEstimado: '' as string,
  };

  facturaForm = {
    ordenId: null as number | null,
    valorTotal: '' as string,
    descripcion: '',
  };

  ngOnInit(): void {
    this.bootstrap();
  }

  bootstrap(): void {
    this.loading = true;
    this.error = null;
    this.ok = null;

    this.pacienteService.list(0, 300).subscribe({
      next: (pRes) => {
        this.pacientes = pRes.content ?? [];
        this.personalService.list(0, 300).subscribe({
          next: (perRes) => {
            this.profesionales = perRes.content ?? [];
            this.refreshResumen();
            this.loading = false;
          },
          error: (err) => this.setError(err, 'No se pudo cargar personal'),
        });
      },
      error: (err) => this.setError(err, 'No se pudo cargar pacientes'),
    });
  }

  onPacienteChange(): void {
    this.ok = null;
    this.error = null;
    this.citas = [];
    this.consultas = [];
    this.ordenes = [];
    this.facturas = [];
    this.consultaForm.citaId = null;
    this.consultaForm.profesionalId = null;
    this.ordenForm.consultaId = null;
    this.ordenForm.plantilla = '';
    this.facturaForm.ordenId = null;
    if (!this.selectedPacienteId) return;
    this.cargarFlujoPaciente(this.selectedPacienteId);
  }

  cargarFlujoPaciente(pacienteId: number): void {
    this.citaService.list().subscribe({
      next: (citas) => {
        this.citas = (citas ?? []).filter((c) => c.pacienteId === pacienteId);
        if (this.citas.length > 0) {
          const citaPendiente = this.citas.find((c) => (c.estado || '').toUpperCase() !== 'ATENDIDO') ?? this.citas[0];
          this.consultaForm.citaId = citaPendiente.id;
          this.consultaForm.profesionalId = citaPendiente.profesionalId;
        }
        this.consultaService.listByPaciente(pacienteId).subscribe({
          next: (consultas) => {
            this.consultas = consultas ?? [];
            if (this.consultas.length > 0) {
              this.ordenForm.consultaId = this.consultas[0].id;
            }
            this.ordenService.listByPaciente(pacienteId).subscribe({
              next: (ordenes) => {
                this.ordenes = ordenes ?? [];
                if (this.ordenes.length > 0) {
                  this.facturaForm.ordenId = this.ordenes[0].id;
                }
                this.facturaService.listByPaciente(pacienteId).subscribe({
                  next: (facturas) => {
                    this.facturas = facturas ?? [];
                  },
                  error: (err) => this.setError(err, 'No se pudieron cargar facturas'),
                });
              },
              error: (err) => this.setError(err, 'No se pudieron cargar órdenes'),
            });
          },
          error: (err) => this.setError(err, 'No se pudieron cargar consultas'),
        });
      },
      error: (err) => this.setError(err, 'No se pudieron cargar citas'),
    });
  }

  crearCita(): void {
    if (!this.selectedPacienteId || !this.citaForm.profesionalId || !this.citaForm.servicio || !this.citaForm.fechaHora) {
      this.error = 'Completa paciente, profesional, servicio y fecha/hora para crear cita';
      return;
    }
    this.error = null;
    this.ok = null;
    this.citaService.create({
      pacienteId: this.selectedPacienteId,
      profesionalId: this.citaForm.profesionalId,
      servicio: this.citaForm.servicio.trim(),
      fechaHora: this.citaForm.fechaHora,
      estado: 'PENDIENTE',
    }).subscribe({
      next: (cita) => {
        this.ok = 'Cita creada';
        this.citaForm.servicio = '';
        this.citaForm.fechaHora = '';
        this.consultaForm.citaId = cita.id;
        this.consultaForm.profesionalId = cita.profesionalId;
        this.cargarFlujoPaciente(this.selectedPacienteId!);
        this.refreshResumen();
      },
      error: (err) => this.setError(err, 'No se pudo crear cita'),
    });
  }

  crearConsulta(): void {
    if (!this.selectedPacienteId || !this.consultaForm.profesionalId || !this.consultaForm.motivoConsulta) {
      this.error = 'Completa profesional y motivo de consulta';
      return;
    }
    this.error = null;
    this.ok = null;
    this.consultaService.create({
      pacienteId: this.selectedPacienteId,
      profesionalId: this.consultaForm.profesionalId,
      citaId: this.consultaForm.citaId || undefined,
      motivoConsulta: this.consultaForm.motivoConsulta.trim(),
      enfermedadActual: this.consultaForm.enfermedadActual.trim() || undefined,
      antecedentesPersonales: this.consultaForm.antecedentesPersonales.trim() || undefined,
      antecedentesFamiliares: this.consultaForm.antecedentesFamiliares.trim() || undefined,
      alergias: this.consultaForm.alergias.trim() || undefined,
    }).subscribe({
      next: (consulta) => {
        this.ok = 'Consulta creada';
        this.consultaForm.motivoConsulta = '';
        this.consultaForm.enfermedadActual = '';
        this.consultaForm.antecedentesPersonales = '';
        this.consultaForm.antecedentesFamiliares = '';
        this.consultaForm.alergias = '';
        this.ordenForm.consultaId = consulta.id;
        this.cargarFlujoPaciente(this.selectedPacienteId!);
        this.refreshResumen();
      },
      error: (err) => this.setError(err, 'No se pudo crear consulta'),
    });
  }

  crearOrden(): void {
    if (!this.selectedPacienteId || !this.ordenForm.consultaId || !this.ordenForm.tipo) {
      this.error = 'Selecciona consulta y tipo de orden';
      return;
    }
    this.error = null;
    this.ok = null;
    this.ordenService.create({
      pacienteId: this.selectedPacienteId,
      consultaId: this.ordenForm.consultaId,
      tipo: this.ordenForm.tipo,
      detalle: (this.ordenForm.detalle.trim() || this.ordenForm.plantilla.trim()) || undefined,
      estado: 'PENDIENTE',
      valorEstimado: this.ordenForm.valorEstimado ? Number(this.ordenForm.valorEstimado) : undefined,
    }).subscribe({
      next: (orden) => {
        this.ok = 'Orden creada';
        this.facturaForm.ordenId = orden.id;
        this.ordenForm.plantilla = '';
        this.ordenForm.detalle = '';
        this.ordenForm.valorEstimado = '';
        this.cargarFlujoPaciente(this.selectedPacienteId!);
        this.refreshResumen();
      },
      error: (err) => this.setError(err, 'No se pudo crear orden'),
    });
  }

  crearFactura(): void {
    if (!this.selectedPacienteId || !this.facturaForm.valorTotal) {
      this.error = 'Ingresa el valor total para facturar';
      return;
    }
    this.error = null;
    this.ok = null;
    this.facturaService.create({
      pacienteId: this.selectedPacienteId,
      ordenId: this.facturaForm.ordenId || undefined,
      valorTotal: Number(this.facturaForm.valorTotal),
      estado: 'PENDIENTE',
      descripcion: this.facturaForm.descripcion.trim() || undefined,
    }).subscribe({
      next: () => {
        this.ok = 'Factura creada';
        this.facturaForm.valorTotal = '';
        this.facturaForm.descripcion = '';
        this.cargarFlujoPaciente(this.selectedPacienteId!);
        this.refreshResumen();
      },
      error: (err) => this.setError(err, 'No se pudo crear factura'),
    });
  }

  refreshResumen(): void {
    this.reporteService.resumen().subscribe({
      next: (res) => this.resumen = res,
      error: () => this.resumen = null,
    });
  }

  labelPaciente(p: PacienteDto): string {
    return `${p.nombres} ${p.apellidos ?? ''}`.trim();
  }

  onCitaSeleccionada(citaId: number | null): void {
    if (!citaId) return;
    const cita = this.citas.find((c) => c.id === citaId);
    if (!cita) return;
    this.consultaForm.profesionalId = cita.profesionalId;
  }

  onConsultaSeleccionada(consultaId: number | null): void {
    if (!consultaId) return;
    this.ordenForm.consultaId = consultaId;
  }

  onOrdenSeleccionada(ordenId: number | null): void {
    if (!ordenId) return;
    this.facturaForm.ordenId = ordenId;
  }

  onTipoOrdenChange(): void {
    this.ordenForm.plantilla = '';
    this.ordenForm.detalle = '';
  }

  aplicarPlantillaOrden(): void {
    if (!this.ordenForm.plantilla) return;
    if (this.ordenForm.tipo === 'LABORATORIO') {
      this.ordenForm.detalle = `Solicitar laboratorio: ${this.ordenForm.plantilla}`;
      return;
    }
    if (this.ordenForm.tipo === 'MEDICAMENTO') {
      this.ordenForm.detalle = `Medicamento indicado: ${this.ordenForm.plantilla}`;
      return;
    }
    this.ordenForm.detalle = this.ordenForm.plantilla;
  }

  private setError(err: any, fallback: string): void {
    this.loading = false;
    this.error = err?.error?.error || err?.message || fallback;
  }
}
