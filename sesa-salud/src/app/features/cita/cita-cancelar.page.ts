/**
 * Página pública para cancelar cita por enlace (S3).
 * Autor: Ing. J Sebastian Vargas S
 */
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { environment } from '../../../environments/environment';

@Component({
  standalone: true,
  selector: 'app-cita-cancelar',
  imports: [CommonModule, FormsModule],
  templateUrl: './cita-cancelar.page.html',
  styleUrl: './cita-cancelar.page.scss',
})
export class CitaCancelarPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  token = '';
  motivo = '';
  loading = false;
  success = false;
  mensaje = '';
  error = '';

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('t') || '';
    if (!this.token.trim()) {
      this.error = 'Enlace inválido: falta el token de la cita.';
    }
  }

  enviar(): void {
    if (!this.token.trim()) return;
    this.loading = true;
    this.error = '';
    this.http
      .post<{ ok: boolean; mensaje?: string; error?: string }>(
        `${this.apiUrl}/cita/cancelar`,
        { t: this.token, motivo: this.motivo.trim() || undefined }
      )
      .subscribe({
        next: (res) => {
          this.loading = false;
          if (res.ok && res.mensaje) {
            this.success = true;
            this.mensaje = res.mensaje;
          } else {
            this.error = res.error || 'No se pudo cancelar la cita.';
          }
        },
        error: (err) => {
          this.loading = false;
          this.error = err?.error?.error || 'Error de conexión. Intente más tarde.';
        },
      });
  }
}
