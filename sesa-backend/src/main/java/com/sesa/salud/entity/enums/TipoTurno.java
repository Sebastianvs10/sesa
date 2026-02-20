/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity.enums;

/**
 * Tipos de turno definidos en la normativa colombiana para IPS Nivel II.
 * Las duraciones y horarios siguen el Código Sustantivo del Trabajo.
 */
public enum TipoTurno {

    /** Urgencias Día  — 07:00 a 19:00 (12 h) */
    URG_DIA(12, 7, "Urgencias Día", true),

    /** Urgencias Noche — 19:00 a 07:00 (12 h) */
    URG_NOCHE(12, 19, "Urgencias Noche", true),

    /** Turno de  6 horas */
    TURNO_6H(6, 6, "Turno 6 h", false),

    /** Turno de  8 horas */
    TURNO_8H(8, 7, "Turno 8 h", false),

    /** Turno de 12 horas (diurno genérico) */
    TURNO_12H(12, 7, "Turno 12 h", false),

    /** Turno de 24 horas (fin de semana — Res. 2003/2014) */
    TURNO_24H(24, 7, "Turno 24 h", true),

    /** Disponibilidad / on-call — computable como 8 h efectivas */
    DISPONIBILIDAD(8, 0, "Disponibilidad", false);

    /** Duración en horas. */
    public final int duracionHoras;

    /** Hora de inicio predeterminada (24 h). */
    public final int horaInicio;

    /** Etiqueta en español para reportes y notificaciones. */
    public final String etiqueta;

    /**
     * Indica si el turno activa el descanso compensatorio obligatorio
     * (nocturno o extenso ≥ 24 h) según el CST.
     */
    public final boolean requiereDescansoExtendido;

    TipoTurno(int duracionHoras, int horaInicio, String etiqueta, boolean requiereDescansoExtendido) {
        this.duracionHoras             = duracionHoras;
        this.horaInicio                = horaInicio;
        this.etiqueta                  = etiqueta;
        this.requiereDescansoExtendido = requiereDescansoExtendido;
    }
}
