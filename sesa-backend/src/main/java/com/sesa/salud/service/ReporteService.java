/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.dto.*;

import java.util.List;

public interface ReporteService {
    ReporteResumenDto resumen();
    List<DashboardCitasPorDiaDto> citasPorUltimosDias(int dias);
    List<DashboardMesCountDto> consultasPorUltimosMeses(int meses);
    List<DashboardFacturacionMesDto> facturacionPorUltimosMeses(int meses);
    List<DashboardCitasPorEstadoDto> citasPorEstado();
    /** Estadísticas para gráficas del dashboard en una sola llamada (GET /api/reportes/dashboard). */
    DashboardStatsDto dashboardStats();
}
