import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { SesaDataTableComponent } from '../../shared/components/sesa-data-table/sesa-data-table.component';
import { LaboratorioSolicitudService } from '../../core/services/laboratorio-solicitud.service';

@Component({
  standalone: true,
  selector: 'sesa-laboratorios-page',
  imports: [CommonModule, SesaCardComponent, SesaDataTableComponent],
  templateUrl: './laboratorios.page.html',
  styleUrl: './laboratorios.page.scss',
})
export class LaboratoriosPageComponent implements OnInit {
  private readonly laboratorioService = inject(LaboratorioSolicitudService);

  cargando = false;
  error: string | null = null;

  solicitudesColumns = [
    { key: 'fecha', label: 'Fecha', width: '90px' },
    { key: 'paciente', label: 'Paciente' },
    { key: 'prueba', label: 'Prueba' },
    { key: 'estado', label: 'Estado', width: '110px' },
  ];

  solicitudesData: Record<string, unknown>[] = [];

  ngOnInit(): void {
    this.cargarSolicitudes();
  }

  private cargarSolicitudes(): void {
    this.cargando = true;
    this.error = null;

    this.laboratorioService.list().subscribe({
      next: (res) => {
        const rows = res?.content ?? [];
        this.solicitudesData = rows.map((s) => ({
          fecha: this.formatFecha(s.fechaSolicitud),
          paciente: s.pacienteNombre || 'Paciente',
          prueba: s.tipoPrueba || 'Sin tipo',
          estado: s.estado || 'Pendiente',
        }));
        this.cargando = false;
      },
      error: (err) => {
        this.error = err?.error?.error || 'No se pudieron cargar solicitudes';
        this.cargando = false;
      },
    });
  }

  private formatFecha(fecha?: string): string {
    if (!fecha) return '--/--/----';
    const f = new Date(fecha);
    if (Number.isNaN(f.getTime())) return '--/--/----';
    return f.toLocaleDateString();
  }
}

