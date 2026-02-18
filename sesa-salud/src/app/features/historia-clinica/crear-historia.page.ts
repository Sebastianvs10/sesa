import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { PacienteService, PacienteDto } from '../../core/services/paciente.service';
import { HistoriaClinicaService, CrearHistoriaCompletaRequestDto } from '../../core/services/historia-clinica.service';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { SesaFormFieldComponent } from '../../shared/components/sesa-form-field/sesa-form-field.component';

export type CrearHCTab = 'paciente' | 'motivo' | 'antecedentes' | 'revision' | 'examen' | 'plan';

@Component({
  standalone: true,
  selector: 'sesa-crear-historia-page',
  imports: [CommonModule, FormsModule, SesaCardComponent, SesaFormFieldComponent],
  templateUrl: './crear-historia.page.html',
  styleUrl: './crear-historia.page.scss',
})
export class CrearHistoriaPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly pacienteService = inject(PacienteService);
  private readonly historiaService = inject(HistoriaClinicaService);

  activeTab: CrearHCTab = 'paciente';

  paciente: PacienteDto | null = null;
  cargandoPaciente = false;
  errorPaciente: string | null = null;
  guardando = false;
  errorGuardado: string | null = null;
  exito = false;

  // 1️⃣ Datos de identificación (ya vienen del paciente)
  // 2️⃣ Motivo de consulta
  motivoConsulta = '';

  // 3️⃣ Enfermedad actual
  enfermedad = '';
  versionEnfermedad = '';
  sintomasAsociados = '';
  factoresMejoran = '';
  factoresEmpeoran = '';

  // 4️⃣ Antecedentes
  antecedentesPatologicos = '';
  antecedentesQuirurgicos = '';
  antecedentesFarmacologicos = '';
  antecedentesAlergicos = '';
  antecedentesTraumaticos = '';
  antecedentesGinecoobstetricos = '';
  habitosTabaco = false;
  habitosAlcohol = false;
  habitosSustancias = false;
  habitosDetalles = '';
  antecedentesFamiliares = '';

  // 5️⃣ Revisión por sistemas
  sistemaRespiratoria = '';
  sistemaCardiovascular = '';
  sistemaDigestivo = '';
  sistemaUrinario = '';
  sistemaNervioso = '';
  sistemaEndocrino = '';
  sistemaMusculos = '';
  sistemaPsiquiatrico = '';

  // 6️⃣ Examen físico
  presionArterial = '';
  frecuenciaCardiaca = '';
  frecuenciaRespiratoria = '';
  temperatura = '';
  peso = '';
  talla = '';
  imc = '';
  evaluacionGeneral = '';
  hallazosMuerte = '';
  hallazgosPulmones = '';
  hallazgosCaza = '';
  hallazgosAbdomen = '';
  hallazgosNeurologico = '';

  // 7️⃣ Impresión diagnóstica
  diagnostico = '';
  codigoCIE10 = '';

  // 8️⃣ Plan de manejo
  tratamientoFarmacologico = '';
  ordenesMedicas = '';
  examenesSolicitados = '';
  incapacidad = '';
  recomendaciones = '';

  ngOnInit(): void {
    const pacienteId = this.route.snapshot.paramMap.get('pacienteId');
    if (!pacienteId) {
      this.router.navigate(['/historia-clinica/nueva']);
      return;
    }

    this.cargandoPaciente = true;
    this.pacienteService.get(parseInt(pacienteId, 10)).subscribe({
      next: (paciente) => {
        this.paciente = paciente;
        this.cargandoPaciente = false;
      },
      error: (err) => {
        this.errorPaciente = err.error?.error || 'Error al cargar paciente';
        this.cargandoPaciente = false;
      },
    });
  }

  guardarHistoria(): void {
    if (!this.paciente) {
      this.errorGuardado = 'Paciente no encontrado';
      return;
    }

    if (!this.motivoConsulta.trim() || !this.enfermedad.trim()) {
      this.errorGuardado = 'Motivo y enfermedad son requeridos';
      return;
    }

    this.guardando = true;
    this.errorGuardado = null;

    const revisionSistemas = [
      this.sistemaRespiratoria && `Respiratorio: ${this.sistemaRespiratoria}`,
      this.sistemaCardiovascular && `Cardiovascular: ${this.sistemaCardiovascular}`,
      this.sistemaDigestivo && `Digestivo: ${this.sistemaDigestivo}`,
      this.sistemaUrinario && `Urinario: ${this.sistemaUrinario}`,
      this.sistemaNervioso && `Nervioso: ${this.sistemaNervioso}`,
      this.sistemaEndocrino && `Endocrino: ${this.sistemaEndocrino}`,
      this.sistemaMusculos && `Musculoesquelético: ${this.sistemaMusculos}`,
      this.sistemaPsiquiatrico && `Psiquiátrico: ${this.sistemaPsiquiatrico}`,
    ]
      .filter(Boolean)
      .join('\n');

    const hallazgos = [this.hallazosMuerte, this.hallazgosPulmones, this.hallazgosCaza, this.hallazgosAbdomen, this.hallazgosNeurologico]
      .filter(Boolean)
      .join('\n');

    const dto: CrearHistoriaCompletaRequestDto = {
      grupoSanguineo: this.paciente.grupoSanguineo || undefined,
      alergiasGenerales: this.antecedentesAlergicos || undefined,
      antecedentesPersonales: this.antecedentesPatologicos || undefined,
      antecedentesQuirurgicos: this.antecedentesQuirurgicos || undefined,
      antecedentesFarmacologicos: this.antecedentesFarmacologicos || undefined,
      antecedentesTraumaticos: this.antecedentesTraumaticos || undefined,
      antecedentesGinecoobstetricos: this.antecedentesGinecoobstetricos || undefined,
      antecedentesFamiliares: this.antecedentesFamiliares || undefined,
      habitosTabaco: this.habitosTabaco || undefined,
      habitosAlcohol: this.habitosAlcohol || undefined,
      habitosSustancias: this.habitosSustancias || undefined,
      habitosDetalles: this.habitosDetalles || undefined,
      motivoConsulta: this.motivoConsulta,
      enfermedadActual: this.enfermedad,
      versionEnfermedad: this.versionEnfermedad || undefined,
      sintomasAsociados: this.sintomasAsociados || undefined,
      factoresMejoran: this.factoresMejoran || undefined,
      factoresEmpeoran: this.factoresEmpeoran || undefined,
      revisionSistemas: revisionSistemas || undefined,
      presionArterial: this.presionArterial || undefined,
      frecuenciaCardiaca: this.frecuenciaCardiaca || undefined,
      frecuenciaRespiratoria: this.frecuenciaRespiratoria || undefined,
      temperatura: this.temperatura || undefined,
      peso: this.peso || undefined,
      talla: this.talla || undefined,
      imc: this.imc || undefined,
      evaluacionGeneral: this.evaluacionGeneral || undefined,
      hallazgos: hallazgos || undefined,
      diagnostico: this.diagnostico || undefined,
      codigoCie10: this.codigoCIE10 || undefined,
      planTratamiento: this.ordenesMedicas || undefined,
      tratamientoFarmacologico: this.tratamientoFarmacologico || undefined,
      ordenesMedicas: this.ordenesMedicas || undefined,
      examenesSolicitados: this.examenesSolicitados || undefined,
      incapacidad: this.incapacidad || undefined,
      recomendaciones: this.recomendaciones || undefined,
    };

    this.historiaService
      .createCompleta(this.paciente.id, dto)
      .subscribe({
        next: () => {
          this.exito = true;
          this.guardando = false;
          setTimeout(() => {
            this.router.navigate(['/historia-clinica'], {
              queryParams: { pacienteId: this.paciente?.id },
            });
          }, 2000);
        },
        error: (err) => {
          this.errorGuardado = err.error?.error || 'Error al guardar';
          this.guardando = false;
        },
      });
  }

  calcularIMC(): void {
    if (this.peso && this.talla) {
      const peso = parseFloat(this.peso);
      const talla = parseFloat(this.talla) / 100;
      const imc = peso / (talla * talla);
      this.imc = imc.toFixed(2);
    }
  }

  cancelar(): void {
    if (confirm('¿Descartar cambios?')) {
      this.router.navigate(['/historia-clinica/nueva']);
    }
  }

  setTab(tab: CrearHCTab): void {
    this.activeTab = tab;
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
}
