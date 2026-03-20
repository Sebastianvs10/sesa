/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Item resumido para lista de urgencias "fuera de tiempo" en el dashboard.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UrgenciaFueraDeTiempoItemDto {

    private Long id;
    private Long pacienteId;
    private String pacienteNombre;
    private String nivelTriage;
    private LocalDateTime fechaHoraIngreso;
    /** Minutos transcurridos desde ingreso. */
    private long minutosEspera;
    /** Límite en minutos para este triage (Res. 5596/2015). */
    private int limiteMinutos;
}
