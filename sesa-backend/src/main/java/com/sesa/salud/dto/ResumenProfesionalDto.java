/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Resumen de horas y carga laboral de un profesional en un mes. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumenProfesionalDto {

    private Long   personalId;
    private String nombres;
    private String apellidos;

    /** Horas totales asignadas en el mes. */
    private int horasTotales;

    /** Horas correspondientes a turnos nocturnos (URG_NOCHE, TURNO_24H). */
    private int horasNocturnas;

    /** Horas trabajadas en días festivos colombianos. */
    private int horasFestivos;

    /** Número de turnos asignados en el mes. */
    private int cantidadTurnos;

    /**
     * Porcentaje de ocupación respecto al máximo legal (192 h/mes).
     * Devuelto como valor entre 0 y 100.
     */
    private int porcentajeOcupacion;

    /**
     * {@code true} si hay algún conflicto de validación en los turnos
     * del profesional para el mes (solapamiento, exceso de horas, etc.).
     */
    private boolean tieneConflictos;
}
