/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service.impl;

import com.sesa.salud.dto.DashboardCitasPorDiaDto;
import com.sesa.salud.dto.DashboardCitasPorEstadoDto;
import com.sesa.salud.dto.DashboardFacturacionMesDto;
import com.sesa.salud.dto.DashboardMesCountDto;
import com.sesa.salud.dto.DashboardStatsDto;
import com.sesa.salud.dto.CumplimientoNormativoDto;
import com.sesa.salud.dto.IndicadorCalidadDto;
import com.sesa.salud.dto.PacienteRiesgoDto;
import com.sesa.salud.dto.ReporteResumenDto;
import com.sesa.salud.dto.RecuperacionCarteraDto;
import com.sesa.salud.dto.EvaluacionHcDto;
import com.sesa.salud.dto.AuditoriaHcProfesionalDto;
import com.sesa.salud.dto.AuditoriaHcServicioDto;
import com.sesa.salud.repository.*;
import com.sesa.salud.service.GlosaService;
import com.sesa.salud.service.ReporteService;
import com.sesa.salud.service.UrgenciaRegistroService;
import com.sesa.salud.entity.Atencion;
import com.sesa.salud.entity.Consulta;
import com.sesa.salud.entity.Evolucion;
import com.sesa.salud.entity.RdaEnvio;
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
    private final HistoriaClinicaRepository historiaClinicaRepository;
    private final AtencionRepository atencionRepository;
    private final RdaEnvioRepository rdaEnvioRepository;
    private final EvolucionRepository evolucionRepository;
    private final UrgenciaRegistroService urgenciaRegistroService;
    private final GlosaService glosaService;

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

    @Override
    @Transactional(readOnly = true)
    public List<IndicadorCalidadDto> getIndicadoresCalidadRes256() {
        long totalCitas = citaRepository.count();
        List<Object[]> porEstado = citaRepository.countGroupByEstado();
        long citasRealizadas = 0;
        long citasAgendadas = 0;
        for (Object[] row : porEstado) {
            String estado = row[0] != null ? ((String) row[0]).toUpperCase() : "";
            Long total = row[1] != null ? (Long) row[1] : 0L;
            if ("REALIZADA".equals(estado) || "ATENDIDA".equals(estado)) {
                citasRealizadas += total;
            } else if ("AGENDADA".equals(estado)) {
                citasAgendadas += total;
            }
        }
        long totalConsultas = consultaRepository.count();
        long totalPacientes = pacienteRepository.count();

        List<IndicadorCalidadDto> lista = new ArrayList<>();
        lista.add(IndicadorCalidadDto.builder()
                .codigo("RES256-OPC-01")
                .nombre("Oportunidad en asignación de citas")
                .categoria("Efectividad")
                .valor(totalCitas > 0 ? String.format("%.1f", 100.0 * citasRealizadas / totalCitas) : "0")
                .meta("≥ 95%")
                .unidad("%")
                .interpretacion("Porcentaje de citas realizadas sobre total programadas")
                .build());
        lista.add(IndicadorCalidadDto.builder()
                .codigo("RES256-OPC-02")
                .nombre("Citas agendadas (acumulado)")
                .categoria("Efectividad")
                .valor(String.valueOf(citasAgendadas))
                .meta("-")
                .unidad("citas")
                .interpretacion("Cantidad de citas en estado agendada")
                .build());
        lista.add(IndicadorCalidadDto.builder()
                .codigo("RES256-CON-01")
                .nombre("Consultas médicas realizadas")
                .categoria("Efectividad")
                .valor(String.valueOf(totalConsultas))
                .meta("-")
                .unidad("consultas")
                .interpretacion("Total de consultas registradas en el periodo")
                .build());
        lista.add(IndicadorCalidadDto.builder()
                .codigo("RES256-PAC-01")
                .nombre("Pacientes únicos atendidos")
                .categoria("Cobertura")
                .valor(String.valueOf(totalPacientes))
                .meta("-")
                .unidad("pacientes")
                .interpretacion("Pacientes con historia clínica activa")
                .build());
        lista.add(IndicadorCalidadDto.builder()
                .codigo("RES256-SAT-01")
                .nombre("Satisfacción del usuario (estructura)")
                .categoria("Satisfacción")
                .valor("En implementación")
                .meta("≥ 90%")
                .unidad("%")
                .interpretacion("Indicador según Res. 0256/2016 - requiere encuestas")
                .build());
        return lista;
    }

    @Override
    @Transactional(readOnly = true)
    public PacienteRiesgoDto getRiesgoPaciente(Long pacienteId) {
        if (pacienteId == null || !pacienteRepository.existsById(pacienteId)) {
            return PacienteRiesgoDto.builder()
                    .nivelRiesgo("BAJO")
                    .puntos(0)
                    .factores(List.of())
                    .recomendaciones(List.of())
                    .build();
        }
        List<String> factores = new ArrayList<>();
        List<String> recomendaciones = new ArrayList<>();
        int puntos = 0;

        var hcOpt = historiaClinicaRepository.findByPacienteId(pacienteId);
        if (hcOpt.isPresent()) {
            String alergias = hcOpt.get().getAlergiasGenerales();
            if (alergias != null && !alergias.isBlank()) {
                factores.add("Alergias registradas");
                recomendaciones.add("Verificar reconciliación de medicamentos y alergias");
                puntos += 25;
            }
        }

        ZonedDateTime hace30 = ZonedDateTime.now().minusDays(30);
        long consultasRecientes = consultaRepository.countByPaciente_IdAndFechaConsultaBetween(
                pacienteId, hace30.toInstant(), Instant.now());
        if (consultasRecientes >= 3) {
            factores.add("Múltiples atenciones recientes (" + consultasRecientes + " en 30 días)");
            recomendaciones.add("Revisar continuidad y adherencia al tratamiento");
            puntos += 30;
        }

        puntos = Math.min(100, puntos);
        String nivel = puntos <= 30 ? "BAJO" : (puntos <= 60 ? "MEDIO" : "ALTO");
        if (factores.isEmpty()) {
            factores.add("Sin factores de riesgo identificados");
        }

        return PacienteRiesgoDto.builder()
                .nivelRiesgo(nivel)
                .puntos(puntos)
                .factores(factores)
                .recomendaciones(recomendaciones)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CumplimientoNormativoDto getCumplimientoNormativo(LocalDate desde, LocalDate hasta, Long profesionalId) {
        if (desde == null) desde = LocalDate.now().minusMonths(1);
        if (hasta == null) hasta = LocalDate.now();
        if (hasta.isBefore(desde)) hasta = desde;
        Instant desdeInst = desde.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant hastaInst = hasta.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<Atencion> atenciones = profesionalId != null
                ? atencionRepository.findByFechaAtencionBetweenAndProfesional_Id(desdeInst, hastaInst, profesionalId)
                : atencionRepository.findByFechaAtencionBetween(desdeInst, hastaInst);
        long totalAtenciones = atenciones.size();
        List<Long> atencionIds = atenciones.stream().map(Atencion::getId).toList();

        long atencionesConRdaEnviado = 0;
        Double porcentajeRdaEnviado = 0.0;
        if (!atencionIds.isEmpty()) {
            List<Long> conRda = rdaEnvioRepository.findDistinctAtencionIdsByAtencionIdInAndEstadoEnvioIn(
                    atencionIds, List.of(RdaEnvio.EstadoRda.ENVIADO, RdaEnvio.EstadoRda.CONFIRMADO));
            atencionesConRdaEnviado = conRda.size();
            porcentajeRdaEnviado = totalAtenciones > 0 ? 100.0 * atencionesConRdaEnviado / totalAtenciones : 0.0;
        }

        var urgenciasDto = urgenciaRegistroService.getReporteCumplimiento(desde, hasta);
        long totalUrgenciasAtendidas = urgenciasDto.getTotalAtendidos();
        long urgenciasDentroTiempo = urgenciasDto.getTotalDentroTiempo();
        Double porcentajeUrgenciasEnTiempo = urgenciasDto.getPorcentajeCumplimiento() != null
                ? urgenciasDto.getPorcentajeCumplimiento() : 0.0;

        long atencionesConCie10YEvolucion24h = 0;
        if (!atencionIds.isEmpty()) {
            List<Long> idsConCie10 = atenciones.stream()
                    .filter(a -> a.getCodigoCie10() != null && !a.getCodigoCie10().isBlank())
                    .map(Atencion::getId)
                    .toList();
            if (!idsConCie10.isEmpty()) {
                List<Evolucion> evols = evolucionRepository.findByAtencion_IdInOrderByFechaAsc(idsConCie10);
                Map<Long, Instant> primeraEvolPorAtencion = evols.stream()
                        .collect(Collectors.toMap(e -> e.getAtencion().getId(), Evolucion::getFecha, (a, b) -> a));
                for (Atencion a : atenciones) {
                    if (a.getCodigoCie10() == null || a.getCodigoCie10().isBlank()) continue;
                    Instant primeraEvol = primeraEvolPorAtencion.get(a.getId());
                    if (primeraEvol != null && a.getFechaAtencion() != null) {
                        Instant limite24h = a.getFechaAtencion().plus(24, java.time.temporal.ChronoUnit.HOURS);
                        if (!primeraEvol.isAfter(limite24h) && !primeraEvol.isBefore(a.getFechaAtencion())) {
                            atencionesConCie10YEvolucion24h++;
                        }
                    }
                }
            }
        }
        Double porcentajeHcCie10Evol24 = totalAtenciones > 0
                ? 100.0 * atencionesConCie10YEvolucion24h / totalAtenciones : 0.0;

        long totalResultadosCriticosNoLeidos = ordenClinicaRepository.countByResultadoCriticoTrueAndSinLectura();
        List<IndicadorCalidadDto> indicadores0256 = getIndicadoresCalidadRes256();

        return CumplimientoNormativoDto.builder()
                .periodoInicio(desde)
                .periodoFin(hasta)
                .profesionalId(profesionalId)
                .porcentajeRdaEnviado(porcentajeRdaEnviado)
                .totalAtenciones(totalAtenciones)
                .atencionesConRdaEnviado(atencionesConRdaEnviado)
                .porcentajeUrgenciasEnTiempo(porcentajeUrgenciasEnTiempo)
                .totalUrgenciasAtendidas(totalUrgenciasAtendidas)
                .urgenciasDentroTiempo(urgenciasDentroTiempo)
                .porcentajeHcConCie10YEvolucion24h(porcentajeHcCie10Evol24)
                .atencionesConCie10YEvolucion24h(atencionesConCie10YEvolucion24h)
                .totalResultadosCriticosNoLeidos(totalResultadosCriticosNoLeidos)
                .indicadores0256(indicadores0256)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RecuperacionCarteraDto recuperacionCartera(Instant desde, Instant hasta, Long contratoId) {
        return glosaService.recuperacionCartera(desde, hasta, contratoId);
    }

    private static final int UMBRAL_AUDITORIA_HC = 70;

    @Override
    @Transactional(readOnly = true)
    public EvaluacionHcDto evaluarAtencion(Long consultaId) {
        Consulta c = consultaRepository.findById(consultaId).orElse(null);
        if (c == null) {
            return EvaluacionHcDto.builder()
                    .consultaId(consultaId)
                    .camposCompletos(false)
                    .camposFaltantes(List.of("Consulta no encontrada"))
                    .puntuacion(0)
                    .build();
        }
        List<String> faltantes = new ArrayList<>();
        int puntuacion = 100;
        if (c.getMotivoConsulta() == null || c.getMotivoConsulta().isBlank()) { faltantes.add("Motivo de consulta"); puntuacion -= 20; }
        if (c.getEnfermedadActual() == null || c.getEnfermedadActual().isBlank()) { faltantes.add("Enfermedad actual / Subjetivo"); puntuacion -= 15; }
        if (c.getCodigoCie10() == null || c.getCodigoCie10().isBlank()) { faltantes.add("Código CIE-10"); puntuacion -= 25; }
        boolean tienePlan = (c.getPlanTratamiento() != null && !c.getPlanTratamiento().isBlank())
                || (c.getTratamientoFarmacologico() != null && !c.getTratamientoFarmacologico().isBlank());
        if (!tienePlan) { faltantes.add("Plan de tratamiento"); puntuacion -= 25; }
        if (c.getProfesional() == null) { faltantes.add("Profesional"); puntuacion -= 15; }
        if (c.getFechaConsulta() == null) { faltantes.add("Fecha"); puntuacion -= 10; }
        puntuacion = Math.max(0, Math.min(100, puntuacion));
        return EvaluacionHcDto.builder()
                .consultaId(consultaId)
                .camposCompletos(faltantes.isEmpty())
                .camposFaltantes(faltantes.isEmpty() ? List.of() : faltantes)
                .puntuacion(puntuacion)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditoriaHcProfesionalDto> reporteAuditoriaHcPorProfesional(LocalDate desde, LocalDate hasta) {
        Instant from = desde != null ? desde.atStartOfDay(ZoneId.systemDefault()).toInstant() : Instant.now().minus(365, java.time.temporal.ChronoUnit.DAYS);
        Instant to = hasta != null ? hasta.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant() : Instant.now();
        List<Consulta> consultas = consultaRepository.findByFechaConsultaBetween(from, to);
        Map<Long, List<Consulta>> porProfesional = consultas.stream()
                .filter(c -> c.getProfesional() != null)
                .collect(Collectors.groupingBy(c -> c.getProfesional().getId()));
        List<AuditoriaHcProfesionalDto> result = new ArrayList<>();
        for (Map.Entry<Long, List<Consulta>> e : porProfesional.entrySet()) {
            List<Consulta> list = e.getValue();
            long total = list.size();
            long completas = 0;
            double sumaPuntos = 0;
            for (Consulta c : list) {
                EvaluacionHcDto ev = evaluarAtencion(c.getId());
                if (ev.getPuntuacion() >= UMBRAL_AUDITORIA_HC) completas++;
                sumaPuntos += ev.getPuntuacion();
            }
            double pct = total > 0 ? 100.0 * completas / total : 0;
            double media = total > 0 ? sumaPuntos / total : 0;
            String nombre = list.get(0).getProfesional().getNombres() + " " + (list.get(0).getProfesional().getApellidos() != null ? list.get(0).getProfesional().getApellidos() : "");
            result.add(AuditoriaHcProfesionalDto.builder()
                    .profesionalId(e.getKey())
                    .profesionalNombre(nombre.trim())
                    .totalAtenciones(total)
                    .atencionesCompletas(completas)
                    .porcentajeCompletas(Math.round(pct * 10) / 10.0)
                    .puntuacionMedia(Math.round(media * 10) / 10.0)
                    .bajoUmbral(media < UMBRAL_AUDITORIA_HC)
                    .build());
        }
        result.sort((a, b) -> Double.compare(b.getPuntuacionMedia(), a.getPuntuacionMedia()));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditoriaHcServicioDto> reporteAuditoriaHcPorServicio(LocalDate desde, LocalDate hasta) {
        Instant from = desde != null ? desde.atStartOfDay(ZoneId.systemDefault()).toInstant() : Instant.now().minus(365, java.time.temporal.ChronoUnit.DAYS);
        Instant to = hasta != null ? hasta.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant() : Instant.now();
        List<Consulta> consultas = consultaRepository.findByFechaConsultaBetween(from, to);
        Map<String, List<Consulta>> porServicio = consultas.stream()
                .collect(Collectors.groupingBy(c -> c.getTipoConsulta() != null && !c.getTipoConsulta().isBlank() ? c.getTipoConsulta() : "SIN_TIPO"));
        List<AuditoriaHcServicioDto> result = new ArrayList<>();
        for (Map.Entry<String, List<Consulta>> e : porServicio.entrySet()) {
            List<Consulta> list = e.getValue();
            long total = list.size();
            long completas = 0;
            double sumaPuntos = 0;
            for (Consulta c : list) {
                EvaluacionHcDto ev = evaluarAtencion(c.getId());
                if (ev.getPuntuacion() >= UMBRAL_AUDITORIA_HC) completas++;
                sumaPuntos += ev.getPuntuacion();
            }
            double pct = total > 0 ? 100.0 * completas / total : 0;
            double media = total > 0 ? sumaPuntos / total : 0;
            result.add(AuditoriaHcServicioDto.builder()
                    .servicio(e.getKey())
                    .totalAtenciones(total)
                    .atencionesCompletas(completas)
                    .porcentajeCompletas(Math.round(pct * 10) / 10.0)
                    .puntuacionMedia(Math.round(media * 10) / 10.0)
                    .build());
        }
        result.sort((a, b) -> Long.compare(b.getTotalAtenciones(), a.getTotalAtenciones()));
        return result;
    }
}
