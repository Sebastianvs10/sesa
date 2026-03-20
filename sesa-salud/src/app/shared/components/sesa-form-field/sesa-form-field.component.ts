/**
 * Campo de formulario premium — prefix/suffix slots, shake on error, skeleton loading.
 * Autor: Ing. J Sebastian Vargas S
 */
import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { SesaSkeletonComponent } from '../sesa-skeleton/sesa-skeleton.component';

@Component({
  standalone: true,
  selector: 'sesa-form-field',
  imports: [CommonModule, SesaSkeletonComponent],
  templateUrl: './sesa-form-field.component.html',
  styleUrl: './sesa-form-field.component.scss',
})
export class SesaFormFieldComponent {
  /** Etiqueta del campo */
  @Input() label = '';
  /** Texto de ayuda bajo el campo */
  @Input() hint = '';
  /** Mensaje de error de validación — activa animación shake */
  @Input() error = '';
  /** Texto del tooltip contextual */
  @Input() tooltip = '';
  /** Indica si el campo es obligatorio */
  @Input() required = false;
  /** Mostrar estado de éxito */
  @Input() success = false;
  /** Muestra skeleton mientras carga el valor del campo (modo edición) */
  @Input() loading = false;
}
