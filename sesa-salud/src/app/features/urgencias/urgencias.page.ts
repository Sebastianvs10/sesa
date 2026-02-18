import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { SesaDataTableComponent } from '../../shared/components/sesa-data-table/sesa-data-table.component';
import { UrgenciaRegistroService } from '../../core/services/urgencia-registro.service';

@Component({
  standalone: true,
  selector: 'sesa-urgencias-page',
  imports: [CommonModule, SesaCardComponent, SesaDataTableComponent],
  templateUrl: './urgencias.page.html',
  styleUrl: './urgencias.page.scss',
})
export class UrgenciasPageComponent implements OnInit {
  private readonly urgenciaService = inject(UrgenciaRegistroService);

  cargando = false;
  error: string | null = null;

  urgenciasColumns = [
    { key: 'hora', label: 'Hora', width: '80px' },
    { key: 'paciente', label: 'Paciente' },
    { key: 'triage', label: 'Triage', width: '90px' },
    { key: 'estado', label: 'Estado', width: '120px' },
  ];

  urgenciasData: Record<string, unknown>[] = [];

  ngOnInit(): void {
    this.cargarUrgencias();
  }

  private cargarUrgencias(): void {
    this.cargando = true;
    this.error = null;

    this.urgenciaService.list().subscribe({
      next: (res) => {
        const rows = res?.content ?? [];
        this.urgenciasData = rows.map((u) => ({
          hora: this.formatHora(u.fechaHoraIngreso),
          paciente: u.pacienteNombre || 'Paciente',
          triage: u.nivelTriage || 'Sin clasificar',
          estado: u.estado || 'Pendiente',
        }));
        this.cargando = false;
      },
      error: (err) => {
        this.error = err?.error?.error || 'No se pudieron cargar urgencias';
        this.cargando = false;
      },
    });
  }

  private formatHora(fechaHora?: string): string {
    if (!fechaHora) return '--:--';
    const fecha = new Date(fechaHora);
    if (Number.isNaN(fecha.getTime())) return '--:--';
    return fecha.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
  }
}

