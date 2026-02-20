/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity.enums;

/** Ciclo de vida de un turno individual. */
public enum EstadoTurno {

    /** Turno registrado, aún no publicado. */
    BORRADOR,

    /** Turno publicado y confirmado para el profesional. */
    APROBADO,

    /** Mes cerrado — turno no modificable. */
    CERRADO
}
