/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cumplimiento de tiempos por nivel de triage en el reporte.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CumplimientoTriageDto {

    private String nivelTriage;
    private long total;
    private long dentroTiempo;
    private long fueraTiempo;
    private Double porcentajeCumplimiento;
}
