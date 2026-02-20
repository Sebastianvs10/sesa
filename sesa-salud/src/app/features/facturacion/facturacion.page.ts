/**
 * Facturación — skeleton, confirm anular, toast.
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';
import { FacturaDto, FacturaService } from '../../core/services/factura.service';
import { PacienteDto, PacienteService } from '../../core/services/paciente.service';

@Component({
  standalone: true,
  selector: 'sesa-facturacion-page',
  imports: [CommonModule, FormsModule, SesaCardComponent],
  templateUrl: './facturacion.page.html',
  styleUrl: './facturacion.page.scss',
})
export class FacturacionPageComponent implements OnInit {
  private readonly facturaService = inject(FacturaService);
  private readonly pacienteService = inject(PacienteService);
  private readonly toast = inject(SesaToastService);

  pacientes: PacienteDto[] = [];
  facturas: FacturaDto[] = [];
  error: string | null = null;
  ripsCsv = '';

  selectedPacienteId: number | null = null;
  desde = '';
  hasta = '';

  ngOnInit(): void {
    this.pacienteService.list(0, 300).subscribe({ next: (res) => (this.pacientes = res.content ?? []) });
  }

  cargarFacturas(): void {
    if (!this.selectedPacienteId) {
      this.error = 'Selecciona paciente';
      return;
    }
    this.error = null;
    this.facturaService.listByPaciente(this.selectedPacienteId).subscribe({
      next: (res) => (this.facturas = res ?? []),
      error: (err: unknown) => {
        this.error = (err as { error?: { error?: string } })?.error?.error ?? 'No se pudieron cargar facturas';
        this.toast.error(this.error!, 'Error de carga');
      },
    });
  }

  exportarRips(): void {
    if (!this.desde || !this.hasta) {
      this.error = 'Selecciona rango de fechas';
      return;
    }
    this.error = null;
    this.facturaService.exportRips(this.desde, this.hasta).subscribe({
      next: (csv: string) => {
        this.ripsCsv = csv;
        this.toast.success('RIPS exportado correctamente.', 'Exportación completada');
      },
      error: (err: unknown) => {
        this.error = (err as { error?: { error?: string } })?.error?.error ?? 'No se pudo exportar RIPS';
        this.toast.error(this.error!, 'Error');
      },
    });
  }
}
