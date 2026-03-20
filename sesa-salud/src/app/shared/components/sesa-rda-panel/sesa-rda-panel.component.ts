/**
 * Panel RDA: permite generar y enviar el Resumen Digital de Atención en Salud
 * al Ministerio de Salud conforme a la Resolución 1888 de 2025
 * Autor: Ing. J Sebastian Vargas S
 */
import {
  Component, Input, OnInit, OnChanges,
  SimpleChanges, ChangeDetectionStrategy, ChangeDetectorRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RdaService, RdaStatus, EstadoRda } from '../../../core/services/rda.service';
import { SesaToastService } from '../sesa-toast/sesa-toast.component';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'app-sesa-rda-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './sesa-rda-panel.component.html',
  styleUrl:    './sesa-rda-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SesaRdaPanelComponent implements OnInit, OnChanges {

  @Input({ required: true }) atencionId!: number;

  rdaActual: RdaStatus | null = null;
  cargando  = false;
  enviando  = false;

  constructor(
    private rdaService: RdaService,
    private toast: SesaToastService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarEstado();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['atencionId'] && !changes['atencionId'].firstChange) {
      this.cargarEstado();
    }
  }

  cargarEstado(): void {
    if (!this.atencionId) return;
    this.cargando = true;
    this.rdaService.obtenerUltimo(this.atencionId, 'CONSULTA_EXTERNA')
      .pipe(finalize(() => { this.cargando = false; this.cdr.markForCheck(); }))
      .subscribe({
        next:  rda => { this.rdaActual = rda; },
        error: _   => { this.rdaActual = null; }
      });
  }

  generarYEnviar(): void {
    this.enviando = true;
    this.rdaService.generarYEnviar(this.atencionId, 'CONSULTA_EXTERNA')
      .pipe(finalize(() => { this.enviando = false; this.cdr.markForCheck(); }))
      .subscribe({
        next: rda => {
          this.rdaActual = rda;
          if (rda.estadoEnvio === 'ENVIADO' || rda.estadoEnvio === 'CONFIRMADO') {
            this.toast.success('RDA enviado exitosamente al Ministerio de Salud');
          } else if (rda.estadoEnvio === 'PENDIENTE') {
            this.toast.info('RDA generado. Configurar API Key para enviar al Ministerio.');
          } else {
            this.toast.error('Error al enviar RDA: ' + (rda.errorMensaje ?? 'Error desconocido'));
          }
        },
        error: err => {
          this.toast.error('Error al procesar el RDA: ' + (err.error?.message ?? err.message));
        }
      });
  }

  reenviar(): void {
    this.enviando = true;
    this.rdaService.enviarAlMinisterio(this.atencionId, 'CONSULTA_EXTERNA')
      .pipe(finalize(() => { this.enviando = false; this.cdr.markForCheck(); }))
      .subscribe({
        next:  rda => { this.rdaActual = rda; this.toast.success('RDA reenviado'); },
        error: err => { this.toast.error('Error: ' + (err.error?.message ?? err.message)); }
      });
  }

  descargar(): void {
    if (!this.rdaActual) return;
    this.rdaService.descargarBundle(this.rdaActual.rdaId).subscribe(blob => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `rda-bundle-${this.rdaActual!.rdaId}.json`;
      a.click();
      URL.revokeObjectURL(url);
    });
  }

  get estadoColor(): string {
    if (!this.rdaActual) return 'secondary';
    return this.rdaService.estadoColor(this.rdaActual.estadoEnvio);
  }

  get estadoLabel(): string {
    if (!this.rdaActual) return '';
    return this.rdaService.estadoLabel(this.rdaActual.estadoEnvio);
  }

  get puedeEnviar(): boolean {
    if (!this.rdaActual) return false;
    return this.rdaActual.estadoEnvio === 'PENDIENTE' || this.rdaActual.estadoEnvio === 'ERROR';
  }

  get estaConfirmado(): boolean {
    return this.rdaActual?.estadoEnvio === 'CONFIRMADO';
  }
}
