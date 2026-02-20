/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity.enums;

/** Ciclo de vida de la programación mensual completa. */
public enum EstadoProgramacion {

    /** En construcción por el Jefe / Coordinador. */
    BORRADOR,

    /** Enviada por el Jefe de Enfermería; pendiente de aprobación del Coordinador. */
    EN_REVISION,

    /** Aprobada por el Coordinador Médico. */
    APROBADO,

    /** Mes cerrado — sin modificaciones permitidas. */
    CERRADO
}
