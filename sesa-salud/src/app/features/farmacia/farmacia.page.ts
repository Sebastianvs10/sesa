import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SesaCardComponent } from '../../shared/components/sesa-card/sesa-card.component';
import { FarmaciaDispensacionDto, FarmaciaService, FarmaciaMedicamentoDto } from '../../core/services/farmacia.service';
import { PacienteDto, PacienteService } from '../../core/services/paciente.service';

@Component({
  standalone: true,
  selector: 'sesa-farmacia-page',
  imports: [CommonModule, FormsModule, SesaCardComponent],
  templateUrl: './farmacia.page.html',
  styleUrl: './farmacia.page.scss',
})
export class FarmaciaPageComponent implements OnInit {
  private readonly farmaciaService = inject(FarmaciaService);
  private readonly pacienteService = inject(PacienteService);

  medicamentos: FarmaciaMedicamentoDto[] = [];
  pacientes: PacienteDto[] = [];
  dispensaciones: FarmaciaDispensacionDto[] = [];
  error: string | null = null;

  medForm = {
    nombre: '',
    lote: '',
    fechaVencimiento: '',
    cantidad: '' as string,
    precio: '' as string,
    stockMinimo: '' as string,
  };

  dispForm = {
    medicamentoId: null as number | null,
    pacienteId: null as number | null,
    cantidad: '' as string,
    entregadoPor: '',
  };

  ngOnInit(): void {
    this.recargar();
    this.pacienteService.list(0, 300).subscribe({ next: (res) => (this.pacientes = res.content ?? []) });
  }

  recargar(): void {
    this.farmaciaService.listMedicamentos().subscribe({
      next: (res) => (this.medicamentos = res ?? []),
      error: (err) => (this.error = err?.error?.error || 'No se pudo cargar inventario'),
    });
  }

  crearMedicamento(): void {
    if (!this.medForm.nombre.trim()) {
      this.error = 'Nombre del medicamento es obligatorio';
      return;
    }
    this.error = null;
    this.farmaciaService.createMedicamento({
      nombre: this.medForm.nombre.trim(),
      lote: this.medForm.lote || undefined,
      fechaVencimiento: this.medForm.fechaVencimiento || undefined,
      cantidad: this.medForm.cantidad ? Number(this.medForm.cantidad) : 0,
      precio: this.medForm.precio ? Number(this.medForm.precio) : undefined,
      stockMinimo: this.medForm.stockMinimo ? Number(this.medForm.stockMinimo) : 0,
      activo: true,
    }).subscribe({
      next: () => {
        this.medForm = { nombre: '', lote: '', fechaVencimiento: '', cantidad: '', precio: '', stockMinimo: '' };
        this.recargar();
      },
      error: (err) => (this.error = err?.error?.error || 'No se pudo crear medicamento'),
    });
  }

  dispensar(): void {
    if (!this.dispForm.medicamentoId || !this.dispForm.pacienteId || !this.dispForm.cantidad) {
      this.error = 'Selecciona medicamento, paciente y cantidad';
      return;
    }
    this.error = null;
    this.farmaciaService.dispensar({
      medicamentoId: this.dispForm.medicamentoId,
      pacienteId: this.dispForm.pacienteId,
      cantidad: Number(this.dispForm.cantidad),
      entregadoPor: this.dispForm.entregadoPor || undefined,
    }).subscribe({
      next: (d) => {
        this.dispensaciones = [d, ...this.dispensaciones];
        this.dispForm = { medicamentoId: null, pacienteId: null, cantidad: '', entregadoPor: '' };
        this.recargar();
      },
      error: (err) => (this.error = err?.error?.error || 'No se pudo dispensar'),
    });
  }
}
