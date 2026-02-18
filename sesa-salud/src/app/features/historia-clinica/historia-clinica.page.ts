import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { PacienteService, PacienteDto } from '../../core/services/paciente.service';
import { HistoriaClinicaService, HistoriaClinicaDto } from '../../core/services/historia-clinica.service';
import { AuthService } from '../../core/services/auth.service';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { SesaFormFieldComponent } from '../../shared/components/sesa-form-field/sesa-form-field.component';
import { SesaDataTableComponent } from '../../shared/components/sesa-data-table/sesa-data-table.component';
import { CitaDto, CitaService } from '../../core/services/cita.service';
import { ConsultaDto, ConsultaService } from '../../core/services/consulta.service';
import { OrdenClinicaDto, OrdenClinicaService } from '../../core/services/orden-clinica.service';
import { FacturaDto, FacturaService } from '../../core/services/factura.service';
import { ReporteResumenDto, ReporteService } from '../../core/services/reporte.service';
import { PersonalDto, PersonalService } from '../../core/services/personal.service';
import { DoloresPanelComponent } from './dolores-panel/dolores-panel.component';

type HistoriaTab = 'historia' | 'notas' | 'medicamentos' | 'documentos' | 'dolores';

@Component({
  standalone: true,
  selector: 'sesa-historia-clinica-page',
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    SesaCardComponent,
    SesaFormFieldComponent,
    SesaDataTableComponent,
    DoloresPanelComponent,
  ],
  templateUrl: './historia-clinica.page.html',
  styleUrl: './historia-clinica.page.scss',
})
export class HistoriaClinicaPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly pacienteService = inject(PacienteService);
  private readonly historiaService = inject(HistoriaClinicaService);
  private readonly authService = inject(AuthService);
  private readonly citaService = inject(CitaService);
  private readonly consultaService = inject(ConsultaService);
  private readonly ordenService = inject(OrdenClinicaService);
  private readonly facturaService = inject(FacturaService);
  private readonly reporteService = inject(ReporteService);
  private readonly personalService = inject(PersonalService);

  selectedPatient: PacienteDto | null = null;
  historiaClinica: HistoriaClinicaDto | null = null;
  profesionales: PersonalDto[] = [];
  citasPaciente: CitaDto[] = [];
  consultasPaciente: ConsultaDto[] = [];
  misConsultas: ConsultaDto[] = [];
  ordenesPaciente: OrdenClinicaDto[] = [];
  facturasPaciente: FacturaDto[] = [];
  resumen: ReporteResumenDto | null = null;
  cargando = true;
  error: string | null = null;
  exito: string | null = null;

  activeTab: HistoriaTab = 'historia';

  consultasTableColumns = [
    { key: 'fecha', label: 'Fecha', width: '90px' },
    { key: 'motivo', label: 'Motivo de consulta' },
    { key: 'servicio', label: 'Servicio' },
    { key: 'profesional', label: 'Profesional' },
  ];

  consultasTableData: Record<string, unknown>[] = [];
  ordenesTableColumns = [
    { key: 'tipo', label: 'Tipo' },
    { key: 'detalle', label: 'Detalle' },
    { key: 'estado', label: 'Estado', width: '110px' },
    { key: 'valor', label: 'Valor', width: '100px' },
  ];
  ordenesTableData: Record<string, unknown>[] = [];
  facturasTableColumns = [
    { key: 'fecha', label: 'Fecha', width: '110px' },
    { key: 'orden', label: 'Orden', width: '90px' },
    { key: 'estado', label: 'Estado', width: '110px' },
    { key: 'valor', label: 'Valor', width: '110px' },
  ];
  facturasTableData: Record<string, unknown>[] = [];

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
    plantilla: '',
    detalle: '',
    valorEstimado: '',
  };

  facturaForm = {
    ordenId: null as number | null,
    valorTotal: '',
    descripcion: '',
  };

  get canCreateHistoria(): boolean {
    const role = this.authService.currentUser()?.role ?? '';
    return role === 'MEDICO' || role === 'ADMIN' || role === 'SUPERADMINISTRADOR';
  }

  get canStartNewHistoria(): boolean {
    return !this.historiaClinica && this.canCreateHistoria;
  }

  ngOnInit(): void {
    this.personalService.list(0, 300).subscribe({
      next: (res) => (this.profesionales = res.content ?? []),
      error: () => (this.profesionales = []),
    });

    this.route.queryParams.subscribe((params) => {
      const pacienteId = params['pacienteId'];
      if (pacienteId) {
        this.loadPaciente(parseInt(pacienteId, 10));
      } else {
        this.loadMisConsultas();
      }
    });
  }

  private loadMisConsultas(): void {
    this.cargando = true;
    this.error = null;
    this.consultaService.listMisConsultas(0, 50).subscribe({
      next: (list) => {
        this.misConsultas = list ?? [];
        this.cargando = false;
      },
      error: (err) => {
        this.error = err?.error?.error || 'Error al cargar consultas';
        this.misConsultas = [];
        this.cargando = false;
      },
    });
  }

  private loadPaciente(pacienteId: number): void {
    this.cargando = true;
    this.pacienteService.get(pacienteId).subscribe({
      next: (paciente) => {
        this.selectedPatient = paciente;
        this.loadHistoria(pacienteId);
      },
      error: (err) => {
        this.error = err.error?.error || 'Error al cargar paciente';
        this.cargando = false;
      },
    });
  }

  private loadHistoria(pacienteId: number): void {
    this.historiaService.getByPacienteOrNull(pacienteId).subscribe({
      next: (historia) => {
        this.historiaClinica = historia;
        this.loadFlujoClinico(pacienteId);
      },
      error: () => {
        this.historiaClinica = null;
        this.loadFlujoClinico(pacienteId);
      },
    });
  }

  private loadFlujoClinico(pacienteId: number): void {
    this.citaService.list().subscribe({
      next: (citas) => {
        this.citasPaciente = (citas ?? []).filter((c) => c.pacienteId === pacienteId);
        this.consultaService.listByPaciente(pacienteId, 0, 100).subscribe({
          next: (consultas) => {
            this.consultasPaciente = Array.isArray(consultas) ? consultas : [];
            this.ordenService.listByPaciente(pacienteId).subscribe({
              next: (ordenes) => {
                this.ordenesPaciente = ordenes ?? [];
                this.facturaService.listByPaciente(pacienteId).subscribe({
                  next: (facturas) => {
                    this.facturasPaciente = facturas ?? [];
                    this.mapTables();
                    this.autoSeleccionarFlujo();
                    this.refreshResumen();
                    this.cargando = false;
                  },
                  error: (err) => this.setLoadError(err, 'Error al cargar facturas'),
                });
              },
              error: (err) => this.setLoadError(err, 'Error al cargar órdenes'),
            });
          },
          error: (err) => this.setLoadError(err, 'Error al cargar consultas'),
        });
      },
      error: (err) => this.setLoadError(err, 'Error al cargar citas'),
    });
  }

  private setLoadError(err: any, fallback: string): void {
    this.error = err?.error?.error || fallback;
    this.cargando = false;
  }

  private mapTables(): void {
    this.consultasTableData = this.consultasPaciente.map((c) => ({
      fecha: this.formatFecha(c.fechaConsulta),
      motivo: c.motivoConsulta || '—',
      servicio: this.getServicioCita(c.citaId),
      profesional: c.profesionalNombre || 'Sin asignar',
    }));

    this.ordenesTableData = this.ordenesPaciente.map((o) => ({
      tipo: o.tipo,
      detalle: o.detalle || '—',
      estado: o.estado || 'PENDIENTE',
      valor: o.valorEstimado != null ? `$ ${o.valorEstimado}` : '—',
    }));

    this.facturasTableData = this.facturasPaciente.map((f) => ({
      fecha: this.formatFecha(f.fechaFactura),
      orden: f.ordenId ?? '—',
      estado: f.estado || 'PENDIENTE',
      valor: `$ ${f.valorTotal}`,
    }));
  }

  private autoSeleccionarFlujo(): void {
    if (this.citasPaciente.length > 0 && !this.consultaForm.citaId) {
      const pendiente = this.citasPaciente.find((c) => (c.estado || '').toUpperCase() !== 'ATENDIDO');
      const cita = pendiente ?? this.citasPaciente[0];
      this.consultaForm.citaId = cita.id;
      this.consultaForm.profesionalId = cita.profesionalId;
    }
    if (this.consultasPaciente.length > 0 && !this.ordenForm.consultaId) {
      this.ordenForm.consultaId = this.consultasPaciente[0].id;
    }
    if (this.ordenesPaciente.length > 0 && !this.facturaForm.ordenId) {
      this.facturaForm.ordenId = this.ordenesPaciente[0].id;
    }
  }

  setTab(tab: HistoriaTab): void {
    this.activeTab = tab;
    this.error = null;
    this.exito = null;
  }

  irVerDetalle(): void {
    this.setTab('historia');
  }

  irAgregarEvolucion(): void {
    this.setTab('notas');
  }

  cerrarHC(): void {
    if (!this.historiaClinica?.id) return;
    this.error = null;
    this.exito = null;
    this.historiaService.update(this.historiaClinica.id, { estado: 'CERRADA' }).subscribe({
      next: (hc) => {
        this.historiaClinica = hc;
        this.exito = 'Historia clínica cerrada correctamente';
      },
      error: (err) => {
        this.error = err?.error?.error || 'No se pudo cerrar la historia clínica';
      },
    });
  }

  imprimirExportar(): void {
    window.print();
  }

  iniciarNuevaHistoria(): void {
    if (this.selectedPatient) {
      this.router.navigate(['/historia-clinica', this.selectedPatient.id, 'nueva']);
    }
  }

  calculateAge(fechaNacimiento: string): number {
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

  onCitaChange(citaId: number | null): void {
    if (!citaId) return;
    const cita = this.citasPaciente.find((c) => c.id === citaId);
    if (!cita) return;
    this.consultaForm.profesionalId = cita.profesionalId;
  }

  onConsultaChange(consultaId: number | null): void {
    if (!consultaId) return;
    this.ordenForm.consultaId = consultaId;
  }

  onOrdenChange(ordenId: number | null): void {
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

  crearConsulta(): void {
    if (!this.selectedPatient?.id || !this.consultaForm.profesionalId || !this.consultaForm.motivoConsulta.trim()) {
      this.error = 'Completa profesional y motivo de consulta';
      return;
    }
    this.error = null;
    this.exito = null;

    this.consultaService.create({
      pacienteId: this.selectedPatient.id,
      profesionalId: this.consultaForm.profesionalId,
      citaId: this.consultaForm.citaId || undefined,
      motivoConsulta: this.consultaForm.motivoConsulta.trim(),
      enfermedadActual: this.consultaForm.enfermedadActual.trim() || undefined,
      antecedentesPersonales: this.consultaForm.antecedentesPersonales.trim() || undefined,
      antecedentesFamiliares: this.consultaForm.antecedentesFamiliares.trim() || undefined,
      alergias: this.consultaForm.alergias.trim() || undefined,
    }).subscribe({
      next: (consulta) => {
        this.exito = 'Consulta creada y enlazada al flujo';
        this.consultaForm.motivoConsulta = '';
        this.consultaForm.enfermedadActual = '';
        this.consultaForm.antecedentesPersonales = '';
        this.consultaForm.antecedentesFamiliares = '';
        this.consultaForm.alergias = '';
        this.ordenForm.consultaId = consulta.id;
        this.loadFlujoClinico(this.selectedPatient!.id);
      },
      error: (err) => {
        this.error = err?.error?.error || 'No se pudo crear consulta';
      },
    });
  }

  crearOrden(): void {
    if (!this.selectedPatient?.id || !this.ordenForm.consultaId || !this.ordenForm.tipo) {
      this.error = 'Selecciona consulta y tipo de orden';
      return;
    }
    this.error = null;
    this.exito = null;
    this.ordenService.create({
      pacienteId: this.selectedPatient.id,
      consultaId: this.ordenForm.consultaId,
      tipo: this.ordenForm.tipo,
      detalle: (this.ordenForm.detalle.trim() || this.ordenForm.plantilla.trim()) || undefined,
      estado: 'PENDIENTE',
      valorEstimado: this.ordenForm.valorEstimado ? Number(this.ordenForm.valorEstimado) : undefined,
    }).subscribe({
      next: (orden) => {
        this.exito = 'Orden creada';
        this.facturaForm.ordenId = orden.id;
        this.ordenForm.plantilla = '';
        this.ordenForm.detalle = '';
        this.ordenForm.valorEstimado = '';
        this.loadFlujoClinico(this.selectedPatient!.id);
      },
      error: (err) => {
        this.error = err?.error?.error || 'No se pudo crear orden';
      },
    });
  }

  crearFactura(): void {
    if (!this.selectedPatient?.id || !this.facturaForm.valorTotal) {
      this.error = 'Ingresa valor total para facturar';
      return;
    }
    this.error = null;
    this.exito = null;
    this.facturaService.create({
      pacienteId: this.selectedPatient.id,
      ordenId: this.facturaForm.ordenId || undefined,
      valorTotal: Number(this.facturaForm.valorTotal),
      estado: 'PENDIENTE',
      descripcion: this.facturaForm.descripcion.trim() || undefined,
    }).subscribe({
      next: () => {
        this.exito = 'Factura creada';
        this.facturaForm.valorTotal = '';
        this.facturaForm.descripcion = '';
        this.loadFlujoClinico(this.selectedPatient!.id);
      },
      error: (err) => {
        this.error = err?.error?.error || 'No se pudo crear factura';
      },
    });
  }

  refreshResumen(): void {
    this.reporteService.resumen().subscribe({
      next: (res) => (this.resumen = res),
      error: () => (this.resumen = null),
    });
  }

  formatFechaConsulta(fecha?: string): string {
    return this.formatFecha(fecha);
  }

  private formatFecha(fecha?: string): string {
    if (!fecha) return '—';
    const f = new Date(fecha);
    if (Number.isNaN(f.getTime())) return '—';
    return f.toLocaleDateString();
  }

  private getServicioCita(citaId?: number): string {
    if (!citaId) return 'Sin cita';
    const cita = this.citasPaciente.find((c) => c.id === citaId);
    return cita?.servicio || 'Sin servicio';
  }
}

