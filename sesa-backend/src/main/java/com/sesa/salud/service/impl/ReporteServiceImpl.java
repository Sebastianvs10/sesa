/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service.impl;

import com.sesa.salud.dto.*;
import com.sesa.salud.repository.*;
import com.sesa.salud.service.ReporteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * NOTA: No usar Instant.minus(n, ChronoUnit.MONTHS) porque Instant no soporta
 * unidades basadas en meses (lanza UnsupportedTemporalTypeException).
 * Se usa ZonedDateTime para restar meses y luego convertir a Instant.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteServiceImpl implements ReporteService {

    private final PacienteRepository pacienteRepository;
    private final CitaRepository citaRepository;
    private final ConsultaRepository consultaRepository;
    private final OrdenClinicaRepository ordenClinicaRepository;
    private final FacturaRepository facturaRepository;

    @Override
    @Transactional(readOnly = true)
    public ReporteResumenDto resumen() {
        BigDecimal totalFacturado = facturaRepository.findAll().stream()
                .map(f -> f.getValorTotal() != null ? f.getValorTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ReporteResumenDto.builder()
                .totalPacientes(pacienteRepository.count())
                .totalCitas(citaRepository.count())
                .totalConsultas(consultaRepository.count())
                .totalOrdenes(ordenClinicaRepository.count())
                .totalFacturas(facturaRepository.count())
                .totalFacturado(totalFacturado)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DashboardCitasPorDiaDto> citasPorUltimosDias(int dias) {
        LocalDate hoy = LocalDate.now();
        List<DashboardCitasPorDiaDto> resultado = new ArrayList<>();
        for (int i = dias - 1; i >= 0; i--) {
            LocalDate fecha = hoy.minusDays(i);
            LocalDateTime inicio = fecha.atStartOfDay();
            LocalDateTime fin = fecha.plusDays(1).atStartOfDay();
            long total = citaRepository.countByFechaHoraBetween(inicio, fin);
            resultado.add(DashboardCitasPorDiaDto.builder()
                    .fecha(fecha.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .total(total)
                    .build());
        }
        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DashboardMesCountDto> consultasPorUltimosMeses(int meses) {
        ZonedDateTime ahora = ZonedDateTime.now();
        Instant hasta = ahora.toInstant();
        Instant desde = ahora.minusMonths(meses).toInstant();
        List<com.sesa.salud.entity.Consulta> consultas = consultaRepository.findByFechaConsultaBetween(desde, hasta);
        Map<YearMonth, Long> porMes = consultas.stream()
                .filter(c -> c.getFechaConsulta() != null)
                .collect(Collectors.groupingBy(
                        c -> YearMonth.from(c.getFechaConsulta().atZone(ZoneId.systemDefault())),
                        Collectors.counting()));
        List<DashboardMesCountDto> resultado = new ArrayList<>();
        YearMonth mesActual = YearMonth.now().minusMonths(meses - 1);
        for (int i = 0; i < meses; i++) {
            long total = porMes.getOrDefault(mesActual, 0L);
            resultado.add(DashboardMesCountDto.builder()
                    .anio(mesActual.getYear())
                    .mes(mesActual.getMonthValue())
                    .total(total)
                    .build());
            mesActual = mesActual.plusMonths(1);
        }
        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DashboardFacturacionMesDto> facturacionPorUltimosMeses(int meses) {
        ZonedDateTime ahora = ZonedDateTime.now();
        Instant hasta = ahora.toInstant();
        Instant desde = ahora.minusMonths(meses).toInstant();
        List<com.sesa.salud.entity.Factura> facturas = facturaRepository.findByFechaFacturaBetween(desde, hasta);
        Map<YearMonth, List<com.sesa.salud.entity.Factura>> porMes = facturas.stream()
                .filter(f -> f.getFechaFactura() != null)
                .collect(Collectors.groupingBy(
                        f -> YearMonth.from(f.getFechaFactura().atZone(ZoneId.systemDefault()))));
        List<DashboardFacturacionMesDto> resultado = new ArrayList<>();
        YearMonth mesActual = YearMonth.now().minusMonths(meses - 1);
        for (int i = 0; i < meses; i++) {
            List<com.sesa.salud.entity.Factura> delMes = porMes.getOrDefault(mesActual, List.of());
            long cantidad = delMes.size();
            BigDecimal valorTotal = delMes.stream()
                    .map(f -> f.getValorTotal() != null ? f.getValorTotal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            resultado.add(DashboardFacturacionMesDto.builder()
                    .anio(mesActual.getYear())
                    .mes(mesActual.getMonthValue())
                    .cantidad(cantidad)
                    .valorTotal(valorTotal)
                    .build());
            mesActual = mesActual.plusMonths(1);
        }
        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DashboardCitasPorEstadoDto> citasPorEstado() {
        List<Object[]> rows = citaRepository.countGroupByEstado();
        List<DashboardCitasPorEstadoDto> resultado = new ArrayList<>();
        for (Object[] row : rows) {
            String estado = (String) row[0];
            Long total = (Long) row[1];
            resultado.add(DashboardCitasPorEstadoDto.builder()
                    .estado(estado != null ? estado : "SIN_ESTADO")
                    .total(total != null ? total : 0L)
                    .build());
        }
        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsDto dashboardStats() {
        return DashboardStatsDto.builder()
                .citasPorDia(citasPorUltimosDias(7))
                .consultasPorMes(consultasPorUltimosMeses(6))
                .facturacionPorMes(facturacionPorUltimosMeses(6))
                .citasPorEstado(citasPorEstado())
                .build();
    }
}
