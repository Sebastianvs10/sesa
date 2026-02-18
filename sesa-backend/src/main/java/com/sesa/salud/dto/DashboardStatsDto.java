/**
 * DTO único con todas las estadísticas para las gráficas del dashboard.
 * Un solo endpoint GET /api/reportes/dashboard devuelve este objeto.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDto {
    private List<DashboardCitasPorDiaDto> citasPorDia;
    private List<DashboardMesCountDto> consultasPorMes;
    private List<DashboardFacturacionMesDto> facturacionPorMes;
    private List<DashboardCitasPorEstadoDto> citasPorEstado;
}
