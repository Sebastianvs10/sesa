/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.dto.DashboardCitasPorDiaDto;
import com.sesa.salud.dto.DashboardCitasPorEstadoDto;
import com.sesa.salud.dto.DashboardFacturacionMesDto;
import com.sesa.salud.dto.DashboardMesCountDto;
import com.sesa.salud.dto.DashboardStatsDto;
import com.sesa.salud.dto.IndicadorCalidadDto;
import com.sesa.salud.dto.ReporteResumenDto;

import java.util.List;

public interface ReporteService {
    ReporteResumenDto resumen();
    List<DashboardCitasPorDiaDto> citasPorUltimosDias(int dias);
    List<DashboardMesCountDto> consultasPorUltimosMeses(int meses);
    List<DashboardFacturacionMesDto> facturacionPorUltimosMeses(int meses);
    List<DashboardCitasPorEstadoDto> citasPorEstado();
    /** Estadísticas para gráficas del dashboard en una sola llamada (GET /api/reportes/dashboard). */
    DashboardStatsDto dashboardStats();

    /** Indicadores de calidad Res. 0256/2016 (Sistema de Información para la Calidad en Salud). */
    List<IndicadorCalidadDto> getIndicadoresCalidadRes256();
}
