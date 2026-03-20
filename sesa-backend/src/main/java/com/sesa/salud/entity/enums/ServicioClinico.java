/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.entity.enums;

/**
 * Servicios clínicos habilitados en la IPS Nivel II
 * según la Resolución 2003 de 2014 del MSPS Colombia.
 */
public enum ServicioClinico {

    URGENCIAS("Urgencias"),
    HOSPITALIZACION("Hospitalización"),
    OBSERVACION("Observación"),
    UCI("UCI Básica"),
    CONSULTA_EXTERNA("Consulta Externa");

    public final String etiqueta;

    ServicioClinico(String etiqueta) {
        this.etiqueta = etiqueta;
    }
}
