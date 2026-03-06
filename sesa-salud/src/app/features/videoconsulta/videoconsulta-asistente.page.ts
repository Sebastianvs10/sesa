/**
 * Vista del asistente: solo toma de notas de la videoconsulta (con consentimiento previo).
 * Ruta: /videoconsulta/asistente?room=XXX&token=YYY (token de asistente).
 * Autor: Ing. J Sebastian Vargas S
 */

import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  OnInit,
  OnDestroy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { interval, Subscription, firstValueFrom } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { VideoconsultaService } from '../../core/services/videoconsulta.service';

@Component({
  selector: 'sesa-videoconsulta-asistente',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="vc-asistente">
      <header class="vc-asistente__header">
        <span class="vc-asistente__logo">SESA</span>
        <span class="vc-asistente__title">Notas de la videoconsulta</span>
      </header>

      @if (!autorizado()) {
        <div class="vc-asistente__wait">
          <p>Esperando autorización del paciente para que un asistente tome notas de esta reunión.</p>
          <p class="vc-asistente__wait-hint">El profesional debe solicitar incluir asistente y el paciente debe aceptar.</p>
        </div>
      } @else {
        <main class="vc-asistente__main">
          <label class="vc-asistente__label" for="notas">Resumen de la conversación (el profesional recibirá este texto al finalizar):</label>
          <textarea
            id="notas"
            class="vc-asistente__textarea"
            [(ngModel)]="notasTexto"
            (blur)="guardar()"
            placeholder="Escriba aquí los puntos principales, acuerdos, indicaciones..."
            rows="16"
          ></textarea>
          <div class="vc-asistente__actions">
            <button type="button" class="vc-asistente__btn" (click)="guardar()">Guardar notas</button>
            @if (guardadoOk()) {
              <span class="vc-asistente__saved">Guardado</span>
            }
          </div>
        </main>
      }
    </div>
  `,
  styleUrl: './videoconsulta-asistente.page.scss',
})
export class VideoconsultaAsistentePageComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly videoconsulta = inject(VideoconsultaService);

  readonly autorizado = signal(false);
  readonly guardadoOk = signal(false);
  notasTexto = '';
  private salaId: string | null = null;
  private token: string | null = null;
  private pollSubscription: Subscription | null = null;

  ngOnInit(): void {
    const room = this.route.snapshot.queryParamMap.get('room');
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!room || !token) {
      this.router.navigate(['/login']);
      return;
    }
    this.salaId = room;
    this.token = token;
    this.pollSubscription = interval(3000).pipe(
      switchMap(() => this.videoconsulta.validarAsistente(room, token))
    ).subscribe({
      next: (res) => {
        if (res.valido) this.autorizado.set(true);
      },
    });
  }

  ngOnDestroy(): void {
    this.pollSubscription?.unsubscribe();
  }

  guardar(): void {
    if (!this.salaId || !this.token || !this.autorizado()) return;
    this.videoconsulta.guardarNotas(this.salaId, this.token, this.notasTexto).subscribe({
      next: () => {
        this.guardadoOk.set(true);
        setTimeout(() => this.guardadoOk.set(false), 2000);
      },
      error: () => {},
    });
  }
}
