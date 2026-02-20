/**
 * Hospitalización — skeleton, toast errores/éxito.
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { PacienteDto, PacienteService } from '../../core/services/paciente.service';
import { HospitalizacionDto, HospitalizacionService } from '../../core/services/hospitalizacion.service';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';

@Component({
  standalone: true,
  selector: 'sesa-hospitalizacion-page',
  imports: [CommonModule, FormsModule, SesaCardComponent],
  templateUrl: './hospitalizacion.page.html',
  styleUrl: './hospitalizacion.page.scss',
})
export class HospitalizacionPageComponent implements OnInit {
  private readonly pacienteService = inject(PacienteService);
  private readonly hospitalizacionService = inject(HospitalizacionService);
  private readonly toast = inject(SesaToastService);

  pacientes: PacienteDto[] = [];
  hospitalizaciones: HospitalizacionDto[] = [];
  error: string | null = null;

  form = {
    pacienteId: null as number | null,
    servicio: '',
    cama: '',
    estado: 'INGRESADO',
    evolucionDiaria: '',
    ordenesMedicas: '',
    epicrisis: '',
  };

  ngOnInit(): void {
    this.pacienteService.list(0, 300).subscribe({ next: (res) => (this.pacientes = res.content ?? []) });
    this.cargar();
  }

  cargar(): void {
    this.hospitalizacionService.listByEstado().subscribe({
      next: (res) => (this.hospitalizaciones = res ?? []),
      error: (err) => {
        this.error = err?.error?.error || 'No se pudo cargar hospitalizaciones';
        this.toast.error(this.error!, 'Error de carga');
      },
    });
  }

  crear(): void {
    if (!this.form.pacienteId) {
      this.error = 'Selecciona paciente';
      return;
    }
    this.error = null;
    this.hospitalizacionService.create({
      pacienteId: this.form.pacienteId,
      servicio: this.form.servicio || undefined,
      cama: this.form.cama || undefined,
      estado: this.form.estado || undefined,
      evolucionDiaria: this.form.evolucionDiaria || undefined,
      ordenesMedicas: this.form.ordenesMedicas || undefined,
      epicrisis: this.form.epicrisis || undefined,
    }).subscribe({
      next: () => {
        this.form.servicio = '';
        this.form.cama = '';
        this.form.evolucionDiaria = '';
        this.form.ordenesMedicas = '';
        this.form.epicrisis = '';
        this.toast.success('Hospitalización registrada correctamente.', 'Alta registrada');
        this.cargar();
      },
      error: (err) => {
        this.error = err?.error?.error || 'No se pudo crear hospitalización';
        this.toast.error(this.error!, 'Error');
      },
    });
  }
}
