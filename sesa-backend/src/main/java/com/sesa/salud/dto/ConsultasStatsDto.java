/**
 * DTO de estadísticas del día para el módulo Consulta Médica.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultasStatsDto {
    private long total;
    private long agendadas;
    private long atendidas;
    private long canceladas;
    private int porcentajeAsistencia;
}
