/**
 * Encabezado de página compartido: identidad (icono + título + subtítulo) alineada a la izquierda,
 * Volver y acciones en la misma fila; barra de acento y tipografía tokens SESA.
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  standalone: true,
  selector: 'sesa-page-header',
  imports: [CommonModule, RouterLink],
  templateUrl: './sesa-page-header.component.html',
  styleUrl: './sesa-page-header.component.scss',
})
export class SesaPageHeaderComponent {
  /** Título principal del encabezado */
  @Input() title = '';
  /** Subtítulo (opcional) */
  @Input() subtitle = '';
  /** Ruta para el botón Volver (si no se indica, no se muestra el botón) */
  @Input() backLink: string | null = null;
  /** Texto del botón Volver */
  @Input() backLabel = 'Volver';
  /** Atributo aria-label para el enlace Volver */
  @Input() backAriaLabel = 'Volver';
}
