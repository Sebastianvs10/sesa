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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
