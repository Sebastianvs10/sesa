import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

/**
 * Campo de formulario con soporte para tooltips, hints y estados de validación.
 * Autor: Ing. J Sebastian Vargas S
 */
@Component({
  standalone: true,
  selector: 'sesa-form-field',
  imports: [CommonModule],
  templateUrl: './sesa-form-field.component.html',
  styleUrl: './sesa-form-field.component.scss',
})
export class SesaFormFieldComponent {
  /** Etiqueta del campo */
  @Input() label = '';
  /** Texto de ayuda bajo el campo */
  @Input() hint = '';
  /** Mensaje de error de validación */
  @Input() error = '';
  /** Texto del tooltip contextual (icono ℹ️ al lado del label) */
  @Input() tooltip = '';
  /** Indica si el campo es obligatorio */
  @Input() required = false;
  /** Mostrar estado de éxito (campo válido) */
  @Input() success = false;
}

