/**
 * Portal del paciente — Consentimientos pendientes y firma desde móvil.
 * Autor: Ing. J Sebastian Vargas S
 */

import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  OnInit,
  ViewChild,
  ElementRef,
  AfterViewInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { PortalPacienteService } from '../../core/services/portal-paciente.service';
import { ConsentimientoService, ConsentimientoInformadoDto } from '../../core/services/consentimiento.service';
import { SesaToastService } from '../../shared/components/sesa-toast/sesa-toast.component';

const TIPOS: Record<string, string> = {
  GENERAL: 'General',
  QUIRURGICO: 'Quirúrgico',
  DIAGNOSTICO: 'Diagnóstico',
  ODONTOLOGICO: 'Odontológico',
  ANESTESIA: 'Anestesia',
};

@Component({
  selector: 'sesa-portal-consentimientos',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  templateUrl: './portal-consentimientos.page.html',
  styleUrl: './portal-consentimientos.page.scss',
})
export class PortalConsentimientosPageComponent implements OnInit, AfterViewInit {
  @ViewChild('canvasFirma') canvasRef?: ElementRef<HTMLCanvasElement>;

  private readonly portalSvc = inject(PortalPacienteService);
  private readonly consentimientoSvc = inject(ConsentimientoService);
  private readonly toast = inject(SesaToastService);

  readonly pendientes = signal<ConsentimientoInformadoDto[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly consentimientoFirma = signal<ConsentimientoInformadoDto | null>(null);

  private ctx: CanvasRenderingContext2D | null = null;
  private dibujando = false;
  private lastX = 0;
  private lastY = 0;
  private hasStroke = false;

  ngOnInit(): void {
    this.cargar();
  }

  ngAfterViewInit(): void {
    this.initCanvas();
  }

  private initCanvas(): void {
    const canvas = this.canvasRef?.nativeElement;
    if (!canvas) return;
    this.ctx = canvas.getContext('2d');
    if (this.ctx) {
      this.ctx.strokeStyle = 'var(--sesa-text-primary, #1a1a1a)';
      this.ctx.lineWidth = 2;
      this.ctx.lineCap = 'round';
    }
  }

  cargar(): void {
    this.loading.set(true);
    this.portalSvc.getConsentimientosPendientes().subscribe({
      next: (list) => {
        this.pendientes.set(list);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.toast.error('No se pudieron cargar los consentimientos.', 'Error');
      },
    });
  }

  tipoLabel(tipo?: string): string {
    return (tipo && TIPOS[tipo]) || tipo || '—';
  }

  formatFecha(iso?: string): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  abrirFirma(c: ConsentimientoInformadoDto): void {
    this.consentimientoFirma.set(c);
    this.hasStroke = false;
    setTimeout(() => {
      this.limpiarCanvas();
      this.initCanvas();
    }, 50);
  }

  cerrarFirma(): void {
    this.consentimientoFirma.set(null);
  }

  private getCanvasCoords(e: MouseEvent): { x: number; y: number } {
    const canvas = this.canvasRef?.nativeElement;
    if (!canvas) return { x: 0, y: 0 };
    const rect = canvas.getBoundingClientRect();
    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;
    return {
      x: (e.clientX - rect.left) * scaleX,
      y: (e.clientY - rect.top) * scaleY,
    };
  }

  iniciarDibujo(e: MouseEvent): void {
    this.dibujando = true;
    const { x, y } = this.getCanvasCoords(e);
    this.lastX = x;
    this.lastY = y;
    this.hasStroke = true;
  }

  dibujar(e: MouseEvent): void {
    if (!this.dibujando || !this.ctx) return;
    const { x, y } = this.getCanvasCoords(e);
    this.ctx.beginPath();
    this.ctx.moveTo(this.lastX, this.lastY);
    this.ctx.lineTo(x, y);
    this.ctx.stroke();
    this.lastX = x;
    this.lastY = y;
  }

  iniciarDibujoTouch(e: TouchEvent): void {
    e.preventDefault();
    const t = e.touches[0];
    const canvas = this.canvasRef?.nativeElement;
    if (!canvas) return;
    const rect = canvas.getBoundingClientRect();
    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;
    this.dibujando = true;
    this.lastX = (t.clientX - rect.left) * scaleX;
    this.lastY = (t.clientY - rect.top) * scaleY;
    this.hasStroke = true;
  }

  dibujarTouch(e: TouchEvent): void {
    e.preventDefault();
    if (!this.dibujando || !this.ctx) return;
    const t = e.touches[0];
    const canvas = this.canvasRef?.nativeElement;
    if (!canvas) return;
    const rect = canvas.getBoundingClientRect();
    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;
    const x = (t.clientX - rect.left) * scaleX;
    const y = (t.clientY - rect.top) * scaleY;
    this.ctx.beginPath();
    this.ctx.moveTo(this.lastX, this.lastY);
    this.ctx.lineTo(x, y);
    this.ctx.stroke();
    this.lastX = x;
    this.lastY = y;
  }

  terminarDibujo(): void {
    this.dibujando = false;
  }

  limpiarCanvas(): void {
    const canvas = this.canvasRef?.nativeElement;
    if (!canvas || !this.ctx) return;
    this.ctx.clearRect(0, 0, canvas.width, canvas.height);
    this.hasStroke = false;
  }

  hayFirma(): boolean {
    return this.hasStroke;
  }

  enviarFirma(): void {
    const c = this.consentimientoFirma();
    if (!c?.id || this.saving()) return;
    const canvas = this.canvasRef?.nativeElement;
    const dataUrl = canvas?.toDataURL?.('image/png') ?? undefined;
    this.saving.set(true);
    this.consentimientoSvc.firmar(c.id, dataUrl).subscribe({
      next: () => {
        this.pendientes.update(list => list.filter(x => x.id !== c.id));
        this.consentimientoFirma.set(null);
        this.saving.set(false);
        this.toast.success('Consentimiento firmado correctamente.', 'Firmado');
      },
      error: () => {
        this.saving.set(false);
        this.toast.error('No se pudo enviar la firma.', 'Error');
      },
    });
  }
}
