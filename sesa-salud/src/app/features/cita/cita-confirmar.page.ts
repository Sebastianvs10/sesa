/**
 * Página pública para confirmar cita por enlace (S3).
 * Autor: Ing. J Sebastian Vargas S
 */
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { environment } from '../../../environments/environment';

@Component({
  standalone: true,
  selector: 'app-cita-confirmar',
  imports: [CommonModule],
  templateUrl: './cita-confirmar.page.html',
  styleUrl: './cita-confirmar.page.scss',
})
export class CitaConfirmarPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  loading = true;
  success = false;
  mensaje = '';
  error = '';

  ngOnInit(): void {
    const t = this.route.snapshot.queryParamMap.get('t');
    if (!t?.trim()) {
      this.loading = false;
      this.error = 'Enlace inválido: falta el token de la cita.';
      return;
    }
    this.http
      .get<{ ok: boolean; mensaje?: string; error?: string }>(
        `${this.apiUrl}/cita/confirmar`,
        { params: { t } }
      )
      .subscribe({
        next: (res) => {
          this.loading = false;
          if (res.ok && res.mensaje) {
            this.success = true;
            this.mensaje = res.mensaje;
          } else {
            this.error = res.error || 'No se pudo confirmar la cita.';
          }
        },
        error: (err) => {
          this.loading = false;
          this.error = err?.error?.error || 'Error de conexión. Intente más tarde.';
        },
      });
  }
}
