/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.dto.DashboardCitasPorDiaDto;
import com.sesa.salud.dto.DashboardCitasPorEstadoDto;
import com.sesa.salud.dto.DashboardFacturacionMesDto;
import com.sesa.salud.dto.DashboardMesCountDto;
import com.sesa.salud.dto.DashboardStatsDto;
import com.sesa.salud.dto.CumplimientoNormativoDto;
import com.sesa.salud.dto.IndicadorCalidadDto;
import com.sesa.salud.dto.ReporteResumenDto;
import com.sesa.salud.dto.PacienteRiesgoDto;
import com.sesa.salud.dto.RecuperacionCarteraDto;
import com.sesa.salud.dto.EvaluacionHcDto;
import com.sesa.salud.dto.AuditoriaHcProfesionalDto;
import com.sesa.salud.dto.AuditoriaHcServicioDto;

import java.time.Instant;
import java.time.LocalDate;
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

    /** Score de riesgo del paciente para cabecera HC (S1). */
    PacienteRiesgoDto getRiesgoPaciente(Long pacienteId);

    /** S4: Panel de cumplimiento normativo por período y opcionalmente por profesional. */
    CumplimientoNormativoDto getCumplimientoNormativo(LocalDate desde, LocalDate hasta, Long profesionalId);

    /** S9: Recuperación de cartera — glosas por período (y opcional contrato). */
    RecuperacionCarteraDto recuperacionCartera(Instant desde, Instant hasta, Long contratoId);

    /** S16: Evaluación de calidad de una consulta (motivo, CIE-10, plan, etc.). */
    EvaluacionHcDto evaluarAtencion(Long consultaId);

    /** S16: Reporte de auditoría HC por profesional (porcentaje completas, puntuación media). */
    List<AuditoriaHcProfesionalDto> reporteAuditoriaHcPorProfesional(LocalDate desde, LocalDate hasta);

    /** S16: Reporte de auditoría HC por servicio (tipo de consulta). */
    List<AuditoriaHcServicioDto> reporteAuditoriaHcPorServicio(LocalDate desde, LocalDate hasta);
}
