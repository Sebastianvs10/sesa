/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Reporte de cumplimiento de tiempos de espera en urgencias (Res. 5596/2015).
 * Sugerencia 10: indicadores por rango de fechas para auditoría y SISPRO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UrgenciaReporteCumplimientoDto {

    private LocalDate desde;
    private LocalDate hasta;

    /** Por nivel de triage: total atendidos, dentro de tiempo, fuera de tiempo, % cumplimiento. */
    private List<CumplimientoTriageDto> porTriage;

    /** Resumen global: total registros, total atendidos en el período, % cumplimiento global. */
    private long totalRegistros;
    private long totalAtendidos;
    private long totalDentroTiempo;
    private long totalFueraTiempo;
    private Double porcentajeCumplimiento;
}
