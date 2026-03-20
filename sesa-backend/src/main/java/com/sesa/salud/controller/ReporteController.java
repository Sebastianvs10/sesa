/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.controller;

import com.sesa.salud.dto.*;
import com.sesa.salud.service.ReporteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','ODONTOLOGO','BACTERIOLOGO','ENFERMERO','JEFE_ENFERMERIA','AUXILIAR_ENFERMERIA','PSICOLOGO','REGENTE_FARMACIA','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ReporteResumenDto resumen() {
        return reporteService.resumen();
    }

    /**
     * Estadísticas para gráficas del dashboard en una sola llamada.
     * Cada sección se carga por separado con try-catch: si una falla,
     * las demás siguen funcionando y el error se logea.
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','ODONTOLOGO','BACTERIOLOGO','ENFERMERO','JEFE_ENFERMERIA','AUXILIAR_ENFERMERIA','PSICOLOGO','REGENTE_FARMACIA','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public DashboardStatsDto dashboard() {
        List<DashboardCitasPorDiaDto> cpd = List.of();
        List<DashboardMesCountDto> cpm = List.of();
        List<DashboardFacturacionMesDto> fpm = List.of();
        List<DashboardCitasPorEstadoDto> cpe = List.of();

        try {
            cpd = reporteService.citasPorUltimosDias(7);
        } catch (Exception e) {
            log.error("Dashboard: error en citasPorUltimosDias: {}", e.getMessage(), e);
        }
        try {
            cpm = reporteService.consultasPorUltimosMeses(6);
        } catch (Exception e) {
            log.error("Dashboard: error en consultasPorUltimosMeses: {}", e.getMessage(), e);
        }
        try {
            fpm = reporteService.facturacionPorUltimosMeses(6);
        } catch (Exception e) {
            log.error("Dashboard: error en facturacionPorUltimosMeses: {}", e.getMessage(), e);
        }
        try {
            cpe = reporteService.citasPorEstado();
        } catch (Exception e) {
            log.error("Dashboard: error en citasPorEstado: {}", e.getMessage(), e);
        }

        return DashboardStatsDto.builder()
                .citasPorDia(cpd)
                .consultasPorMes(cpm)
                .facturacionPorMes(fpm)
                .citasPorEstado(cpe)
                .build();
    }

    @GetMapping("/dashboard/citas-por-dia")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','ODONTOLOGO','BACTERIOLOGO','ENFERMERO','JEFE_ENFERMERIA','AUXILIAR_ENFERMERIA','PSICOLOGO','REGENTE_FARMACIA','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public List<DashboardCitasPorDiaDto> citasPorDia(@RequestParam(defaultValue = "7") int dias) {
        return reporteService.citasPorUltimosDias(Math.min(Math.max(1, dias), 31));
    }

    @GetMapping("/dashboard/consultas-por-mes")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','ODONTOLOGO','BACTERIOLOGO','ENFERMERO','JEFE_ENFERMERIA','AUXILIAR_ENFERMERIA','PSICOLOGO','REGENTE_FARMACIA','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public List<DashboardMesCountDto> consultasPorMes(@RequestParam(defaultValue = "6") int meses) {
        return reporteService.consultasPorUltimosMeses(Math.min(Math.max(1, meses), 12));
    }

    @GetMapping("/dashboard/facturacion-por-mes")
    @PreAuthorize("hasAnyRole('ADMIN','USER','REGENTE_FARMACIA','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public List<DashboardFacturacionMesDto> facturacionPorMes(@RequestParam(defaultValue = "6") int meses) {
        return reporteService.facturacionPorUltimosMeses(Math.min(Math.max(1, meses), 12));
    }

    @GetMapping("/dashboard/citas-por-estado")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','ODONTOLOGO','BACTERIOLOGO','ENFERMERO','JEFE_ENFERMERIA','AUXILIAR_ENFERMERIA','PSICOLOGO','REGENTE_FARMACIA','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public List<DashboardCitasPorEstadoDto> citasPorEstado() {
        return reporteService.citasPorEstado();
    }

    /** Tablero de calidad en salud — Res. 0256/2016 (indicadores de efectividad, cobertura, satisfacción). */
    @GetMapping("/calidad")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','SUPERADMINISTRADOR','COORDINADOR_MEDICO','SUPERVISOR_APS','JEFE_ENFERMERIA','ENFERMERO','ODONTOLOGO','RECEPCIONISTA')")
    public List<IndicadorCalidadDto> indicadoresCalidad() {
        return reporteService.getIndicadoresCalidadRes256();
    }

    /** Score de riesgo del paciente para cabecera de historia clínica (S1). */
    @GetMapping("/paciente/{pacienteId}/riesgo")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','ODONTOLOGO','BACTERIOLOGO','ENFERMERO','JEFE_ENFERMERIA','AUXILIAR_ENFERMERIA','PSICOLOGO','REGENTE_FARMACIA','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public PacienteRiesgoDto riesgoPaciente(@PathVariable Long pacienteId) {
        return reporteService.getRiesgoPaciente(pacienteId);
    }

    /** S4: Panel de cumplimiento normativo. */
    @GetMapping("/cumplimiento-normativo")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','COORDINADOR_MEDICO','REPORTES')")
    public CumplimientoNormativoDto cumplimientoNormativo(
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta,
            @RequestParam(required = false) Long profesionalId) {
        return reporteService.getCumplimientoNormativo(desde, hasta, profesionalId);
    }

    /** S9: Recuperación de cartera — glosas por período. */
    @GetMapping("/recuperacion-cartera")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','FACTURACION','RECEPCIONISTA')")
    public RecuperacionCarteraDto recuperacionCartera(
            @RequestParam(required = false) Instant desde,
            @RequestParam(required = false) Instant hasta,
            @RequestParam(required = false) Long contratoId) {
        if (desde == null) desde = Instant.now().minusSeconds(30L * 24 * 60 * 60);
        if (hasta == null) hasta = Instant.now();
        return reporteService.recuperacionCartera(desde, hasta, contratoId);
    }

    /** S16: Evaluación de calidad de una consulta (auditoría HC). */
    @GetMapping("/auditoria-hc/atencion/{consultaId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','COORDINADOR_MEDICO','REPORTES','MEDICO')")
    public EvaluacionHcDto auditoriaHcAtencion(@PathVariable Long consultaId) {
        return reporteService.evaluarAtencion(consultaId);
    }

    /** S16: Reporte de auditoría HC por profesional. */
    @GetMapping("/auditoria-hc/por-profesional")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','COORDINADOR_MEDICO','REPORTES')")
    public List<AuditoriaHcProfesionalDto> auditoriaHcPorProfesional(
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta) {
        return reporteService.reporteAuditoriaHcPorProfesional(desde, hasta);
    }

    /** S16: Reporte de auditoría HC por servicio (tipo de consulta). */
    @GetMapping("/auditoria-hc/por-servicio")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','COORDINADOR_MEDICO','REPORTES')")
    public List<AuditoriaHcServicioDto> auditoriaHcPorServicio(
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta) {
        return reporteService.reporteAuditoriaHcPorServicio(desde, hasta);
    }
}
