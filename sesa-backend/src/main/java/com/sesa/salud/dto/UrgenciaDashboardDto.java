/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Respuesta del dashboard de urgencias (sugerencia 2).
 * Contadores por estado y triage, lista de pacientes fuera de tiempo y tiempo promedio de espera.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UrgenciaDashboardDto {

    /** Conteo por estado (EN_ESPERA, EN_ATENCION, ALTA, etc.). */
    private Map<String, Long> conteoPorEstado;

    /** Conteo por nivel de triage (I, II, III, IV, V). */
    private Map<String, Long> conteoPorTriage;

    /** Pacientes en espera que superan el tiempo límite según Res. 5596/2015. */
    private List<UrgenciaFueraDeTiempoItemDto> fueraDeTiempo;

    /** Tiempo promedio de espera en minutos (solo pacientes actualmente en espera). */
    private Double tiempoPromedioEsperaMinutos;

    /** Total de registros en espera. */
    private long totalEnEspera;
}
