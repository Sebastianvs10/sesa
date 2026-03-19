/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service.impl;

import com.sesa.salud.dto.AlertaFacturacionDto;
import com.sesa.salud.dto.AlertasFacturacionDto;
import com.sesa.salud.dto.BandejaFacturadorDto;
import com.sesa.salud.dto.ChecklistRadicacionDto;
import com.sesa.salud.dto.FacturaDto;
import com.sesa.salud.dto.FacturaRequestDto;
import com.sesa.salud.dto.FacturaItemDto;
import com.sesa.salud.dto.FacturaItemRequestDto;
import com.sesa.salud.dto.ResumenFacturacionDto;
import com.sesa.salud.dto.FacturaDetalleCompletoDto;
import com.sesa.salud.dto.FacturaLoteResultDto;
import com.sesa.salud.dto.FacturaTimelineEventDto;
import com.sesa.salud.dto.OrdenPendienteFacturaDto;
import com.sesa.salud.dto.TareaFacturadorDto;
import com.sesa.salud.entity.Factura;
import com.sesa.salud.entity.FacturaItem;
import com.sesa.salud.entity.Glosa;
import com.sesa.salud.entity.OrdenClinica;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.entity.Radicacion;
import com.sesa.salud.repository.FacturaRepository;
import com.sesa.salud.repository.GlosaRepository;
import com.sesa.salud.repository.OrdenClinicaRepository;
import com.sesa.salud.repository.PacienteRepository;
import com.sesa.salud.repository.RadicacionRepository;
import com.sesa.salud.service.FacturaService;
import com.sesa.salud.service.FacturacionElectronicaDianService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FacturaServiceImpl implements FacturaService {

    private final FacturaRepository facturaRepository;
    private final PacienteRepository pacienteRepository;
    private final OrdenClinicaRepository ordenClinicaRepository;
    private final GlosaRepository glosaRepository;
    private final RadicacionRepository radicacionRepository;
    private final FacturacionElectronicaDianService facturacionElectronicaDianService;

    /** Días hábiles para radicación ante EPS/ADRES (normativa vigente Res. 558/2024, FEV-RIPS). */
    private static final int DIAS_HABILES_RADICACION = 22;
    private static final ZoneId ZONE_COLOMBIA = ZoneId.of("America/Bogota");

    private static long businessDaysBetween(Instant from, Instant to) {
        LocalDate dFrom = from.atZone(ZONE_COLOMBIA).toLocalDate();
        LocalDate dTo = to.atZone(ZONE_COLOMBIA).toLocalDate();
        long count = 0;
        for (LocalDate d = dFrom; !d.isAfter(dTo); d = d.plusDays(1)) {
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) {
                count++;
            }
        }
        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FacturaDto> findByPacienteId(Long pacienteId, Pageable pageable) {
        return facturaRepository.findByPaciente_IdOrderByFechaFacturaDesc(pacienteId, pageable)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FacturaDto> findAllFiltered(String estado, Instant desde, Instant hasta, Long pacienteId, Pageable pageable) {
        Specification<Factura> spec = buildSpec(estado, desde, hasta, pacienteId);
        return facturaRepository.findAll(spec, pageable).map(this::toDto);
    }

    private Specification<Factura> buildSpec(String estado, Instant desde, Instant hasta, Long pacienteId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (estado != null && !estado.isBlank()) {
                predicates.add(cb.equal(root.get("estado"), estado));
            }
            if (desde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaFactura"), desde));
            }
            if (hasta != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaFactura"), hasta));
            }
            if (pacienteId != null) {
                predicates.add(cb.equal(root.get("paciente").get("id"), pacienteId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    @Transactional(readOnly = true)
    public FacturaDto findById(Long id) {
        return toDto(facturaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada: " + id)));
    }

    @Override
    @Transactional
    public FacturaDto create(FacturaRequestDto dto) {
        Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + dto.getPacienteId()));
        OrdenClinica orden = dto.getOrdenId() != null
                ? ordenClinicaRepository.findById(dto.getOrdenId()).orElse(null)
                : null;

        // Generar número de factura automático si no se proporcionó (consecutivo por secuencia tenant)
        String numeroFactura = dto.getNumeroFactura();
        if (numeroFactura == null || numeroFactura.isBlank()) {
            long consecutivo = facturaRepository.getNextConsecutive();
            int anio = Year.now().getValue();
            numeroFactura = String.format("FV-%d-%06d", anio, consecutivo);
        }

        BigDecimal valorTotal = dto.getValorTotal() != null ? dto.getValorTotal() : BigDecimal.ZERO;
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            valorTotal = dto.getItems().stream()
                    .map(it -> it.getValorTotal() != null ? it.getValorTotal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        Factura factura = Factura.builder()
                .numeroFactura(numeroFactura)
                .paciente(paciente)
                .orden(orden)
                .valorTotal(valorTotal)
                .estado(dto.getEstado() != null ? dto.getEstado() : "PENDIENTE")
                .descripcion(dto.getDescripcion())
                .fechaFactura(dto.getFechaFactura() != null ? dto.getFechaFactura() : Instant.now())
                .codigoCups(dto.getCodigoCups())
                .descripcionCups(dto.getDescripcionCups())
                .tipoServicio(dto.getTipoServicio())
                .responsablePago(dto.getResponsablePago())
                .cuotaModeradora(dto.getCuotaModeradora())
                .numeroAutorizacionEps(dto.getNumeroAutorizacionEps())
                .build();
        factura = facturaRepository.save(factura);

        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            int idx = 0;
            for (FacturaItemRequestDto req : dto.getItems()) {
                BigDecimal vU = req.getValorUnitario() != null ? req.getValorUnitario() : BigDecimal.ZERO;
                Integer cant = req.getCantidad() != null && req.getCantidad() > 0 ? req.getCantidad() : 1;
                BigDecimal vT = req.getValorTotal() != null ? req.getValorTotal() : vU.multiply(BigDecimal.valueOf(cant));
                FacturaItem item = FacturaItem.builder()
                        .factura(factura)
                        .itemIndex(req.getItemIndex() != null ? req.getItemIndex() : idx)
                        .codigoCups(req.getCodigoCups())
                        .descripcionCups(req.getDescripcionCups())
                        .tipoServicio(req.getTipoServicio())
                        .cantidad(cant)
                        .valorUnitario(vU)
                        .valorTotal(vT)
                        .build();
                factura.getItems().add(item);
                idx++;
            }
            factura = facturaRepository.save(factura);
        }
        return toDto(factura);
    }

    @Override
    @Transactional
    public FacturaDto update(Long id, FacturaRequestDto dto) {
        Factura factura = facturaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada: " + id));
        if (dto.getNumeroFactura() != null) factura.setNumeroFactura(dto.getNumeroFactura());
        if (dto.getValorTotal() != null) factura.setValorTotal(dto.getValorTotal());
        if (dto.getEstado() != null) factura.setEstado(dto.getEstado());
        if (dto.getDescripcion() != null) factura.setDescripcion(dto.getDescripcion());
        if (dto.getFechaFactura() != null) factura.setFechaFactura(dto.getFechaFactura());
        // Campos normativos RIPS / cuenta médica (indispensables para edición pre-radicación)
        if (dto.getCodigoCups() != null) factura.setCodigoCups(dto.getCodigoCups());
        if (dto.getDescripcionCups() != null) factura.setDescripcionCups(dto.getDescripcionCups());
        if (dto.getTipoServicio() != null) factura.setTipoServicio(dto.getTipoServicio());
        if (dto.getResponsablePago() != null) factura.setResponsablePago(dto.getResponsablePago());
        if (dto.getCuotaModeradora() != null) factura.setCuotaModeradora(dto.getCuotaModeradora());
        if (dto.getNumeroAutorizacionEps() != null) factura.setNumeroAutorizacionEps(dto.getNumeroAutorizacionEps());

        if (dto.getItems() != null) {
            factura.getItems().clear();
            BigDecimal sum = BigDecimal.ZERO;
            int idx = 0;
            for (FacturaItemRequestDto req : dto.getItems()) {
                BigDecimal vU = req.getValorUnitario() != null ? req.getValorUnitario() : BigDecimal.ZERO;
                Integer cant = req.getCantidad() != null && req.getCantidad() > 0 ? req.getCantidad() : 1;
                BigDecimal vT = req.getValorTotal() != null ? req.getValorTotal() : vU.multiply(BigDecimal.valueOf(cant));
                FacturaItem item = FacturaItem.builder()
                        .factura(factura)
                        .itemIndex(req.getItemIndex() != null ? req.getItemIndex() : idx)
                        .codigoCups(req.getCodigoCups())
                        .descripcionCups(req.getDescripcionCups())
                        .tipoServicio(req.getTipoServicio())
                        .cantidad(cant)
                        .valorUnitario(vU)
                        .valorTotal(vT)
                        .build();
                factura.getItems().add(item);
                sum = sum.add(vT);
                idx++;
            }
            if (!dto.getItems().isEmpty()) {
                factura.setValorTotal(sum);
            }
        }
        return toDto(facturaRepository.save(factura));
    }

    @Override
    @Transactional
    public FacturaDto cambiarEstado(Long id, String nuevoEstado) {
        Factura factura = facturaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada: " + id));
        String estadoActual = factura.getEstado();
        if ("ANULADA".equals(estadoActual)) {
            throw new RuntimeException("No se puede modificar una factura anulada.");
        }
        factura.setEstado(nuevoEstado.toUpperCase());
        return toDto(facturaRepository.save(factura));
    }

    @Override
    @Transactional
    public FacturaDto emitirElectronica(Long id) {
        Factura factura = facturaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada: " + id));
        // Evitar reenvíos innecesarios si ya fue aceptada por DIAN
        if ("ACEPTADA".equalsIgnoreCase(factura.getDianEstado())) {
            return toDto(factura);
        }
        facturacionElectronicaDianService.emitirFactura(factura);
        Factura guardada = facturaRepository.save(factura);
        return toDto(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public FacturaDetalleCompletoDto findDetalleCompleto(Long id) {
        Factura f = facturaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada: " + id));
        FacturaDto factura = toDto(f);
        FacturaDetalleCompletoDto.FacturaDetalleCompletoDtoBuilder b = FacturaDetalleCompletoDto.builder().factura(factura);
        if (f.getOrden() != null) {
            OrdenClinica ord = f.getOrden();
            b.ordenId(ord.getId()).tipoOrden(ord.getTipo()).valorEstimadoOrden(ord.getValorEstimado());
            if (ord.getConsulta() != null) {
                var c = ord.getConsulta();
                b.consultaId(c.getId())
                 .fechaConsultaIso(c.getFechaConsulta() != null ? c.getFechaConsulta().toString() : null)
                 .codigoCie10Consulta(c.getCodigoCie10());
            }
        }
        return b.build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FacturaTimelineEventDto> getTimeline(Long facturaId) {
        Factura f = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada: " + facturaId));
        List<FacturaTimelineEventDto> events = new ArrayList<>();

        events.add(FacturaTimelineEventDto.builder()
                .tipo(FacturaTimelineEventDto.TipoEvento.CREADA)
                .fecha(f.getCreatedAt())
                .titulo("Factura creada")
                .descripcion("Registro inicial de la cuenta médica.")
                .referencia(f.getNumeroFactura())
                .build());

        if (f.getDianEstado() != null && !f.getDianEstado().isBlank()) {
            Instant fevFecha = f.getDianFechaEnvio() != null ? f.getDianFechaEnvio() : f.getCreatedAt();
            events.add(FacturaTimelineEventDto.builder()
                    .tipo(FacturaTimelineEventDto.TipoEvento.EMITIDA_FEV)
                    .fecha(fevFecha)
                    .titulo("Emitida factura electrónica")
                    .descripcion("FEV DIAN: " + f.getDianEstado())
                    .referencia(f.getDianCufe())
                    .build());
        }

        List<Radicacion> radicaciones = radicacionRepository.findByFactura_IdOrderByFechaRadicacionDesc(facturaId);
        for (int i = radicaciones.size() - 1; i >= 0; i--) {
            Radicacion r = radicaciones.get(i);
            events.add(FacturaTimelineEventDto.builder()
                    .tipo(FacturaTimelineEventDto.TipoEvento.RADICADA)
                    .fecha(r.getFechaRadicacion())
                    .titulo("Radicada ante EPS")
                    .descripcion(r.getEpsNombre() != null ? r.getEpsNombre() : "EPS")
                    .referencia(r.getNumeroRadicado())
                    .build());
        }

        List<Glosa> glosas = glosaRepository.findByFactura_IdOrderByFechaRegistroDesc(facturaId);
        for (int i = glosas.size() - 1; i >= 0; i--) {
            Glosa g = glosas.get(i);
            String motivo = g.getMotivoRechazo() != null && g.getMotivoRechazo().length() > 80
                    ? g.getMotivoRechazo().substring(0, 80) + "…" : g.getMotivoRechazo();
            events.add(FacturaTimelineEventDto.builder()
                    .tipo(FacturaTimelineEventDto.TipoEvento.GLOSA)
                    .fecha(g.getFechaRegistro())
                    .titulo("Glosa #" + g.getId() + " — " + g.getEstado())
                    .descripcion(motivo)
                    .referencia("Glosa #" + g.getId())
                    .build());
        }

        String estado = f.getEstado();
        if ("PAGADA".equals(estado)) {
            events.add(FacturaTimelineEventDto.builder()
                    .tipo(FacturaTimelineEventDto.TipoEvento.PAGADA)
                    .fecha(Instant.now())
                    .titulo("Pagada")
                    .descripcion("Factura cobrada.")
                    .build());
        } else if ("RECHAZADA".equals(estado)) {
            events.add(FacturaTimelineEventDto.builder()
                    .tipo(FacturaTimelineEventDto.TipoEvento.RECHAZADA)
                    .fecha(Instant.now())
                    .titulo("Rechazada")
                    .descripcion("Estado: rechazada.")
                    .build());
        } else if ("ANULADA".equals(estado)) {
            events.add(FacturaTimelineEventDto.builder()
                    .tipo(FacturaTimelineEventDto.TipoEvento.ANULADA)
                    .fecha(Instant.now())
                    .titulo("Anulada")
                    .descripcion("Factura anulada.")
                    .build());
        }

        events.sort((a, b) -> {
            Instant fa = a.getFecha() != null ? a.getFecha() : Instant.EPOCH;
            Instant fb = b.getFecha() != null ? b.getFecha() : Instant.EPOCH;
            return fa.compareTo(fb);
        });
        return events;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (!facturaRepository.existsById(id)) {
            throw new RuntimeException("Factura no encontrada: " + id);
        }
        facturaRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public String exportRipsCsv(Instant desde, Instant hasta) {
        List<Factura> facturas = facturaRepository.findByFechaFacturaBetween(desde, hasta);
        StringBuilder sb = new StringBuilder();
        sb.append("numero_factura,factura_id,tipo_doc,documento,paciente,eps,orden_id,fecha,valor,estado\n");
        for (Factura f : facturas) {
            Paciente p = f.getPaciente();
            String pacienteNombre = (p.getNombres() + " " +
                    (p.getApellidos() != null ? p.getApellidos() : "")).trim().replace(",", " ");
            String eps = (p.getEps() != null ? p.getEps().getNombre() : "").replace(",", " ");
            sb.append(f.getNumeroFactura() != null ? f.getNumeroFactura() : "").append(",")
                    .append(f.getId()).append(",")
                    .append(p.getTipoDocumento() != null ? p.getTipoDocumento() : "").append(",")
                    .append(p.getDocumento()).append(",")
                    .append(pacienteNombre).append(",")
                    .append(eps).append(",")
                    .append(f.getOrden() != null ? f.getOrden().getId() : "").append(",")
                    .append(f.getFechaFactura()).append(",")
                    .append(f.getValorTotal()).append(",")
                    .append(f.getEstado())
                    .append("\n");
        }
        return sb.toString();
    }

    /**
     * Genera archivos RIPS estructurados según Res. 3374/2000:
     * CT - Datos del usuario (paciente)
     * US - Servicios ambulatorios (consultas/urgencias)
     * AP - Procedimientos
     * AC - Consultas externas
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, String> exportRipsEstructurado(Instant desde, Instant hasta) {
        List<Factura> facturas = facturaRepository.findByFechaFacturaBetween(desde, hasta);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("America/Bogota"));

        // Archivo CT — Datos del usuario (paciente)
        StringBuilder ct = new StringBuilder();
        ct.append("tipoDocumento|documento|primerApellido|segundoApellido|primerNombre|segundoNombre|fechaNacimiento|sexo|municipioResidencia|zonaResidencia|tipoUsuario|regimenAfiliacion|codEps\n");
        for (Factura f : facturas) {
            Paciente p = f.getPaciente();
            ct.append(nvl(p.getTipoDocumento())).append("|")
              .append(nvl(p.getDocumento())).append("|")
              .append(nvl(primerApellido(p.getApellidos()))).append("|")
              .append(nvl(segundoApellido(p.getApellidos()))).append("|")
              .append(nvl(primerNombre(p.getNombres()))).append("|")
              .append(nvl(segundoNombre(p.getNombres()))).append("|")
              .append(p.getFechaNacimiento() != null ? p.getFechaNacimiento().toString().replace("-","") : "").append("|")
              .append(nvl(p.getSexo())).append("|")
              .append(nvl(p.getMunicipioResidencia())).append("|")
              .append(nvl(p.getZonaResidencia())).append("|")
              .append(nvl(p.getTipoUsuario())).append("|")
              .append(nvl(p.getRegimenAfiliacion())).append("|")
              .append(p.getEps() != null ? nvl(p.getEps().getCodigo()) : "")
              .append("\n");
        }

        // Archivo US — Servicios ambulatorios
        StringBuilder us = new StringBuilder();
        us.append("codPrestador|fechaInicioAtencion|numAutorizacion|tipoDocumento|documento|diasEstancia|codigoCups|codigoDiagnostico|tipoServicio|valorBruto|valorDescuento|valorCopago|valorModerador|valorRecobro\n");
        for (Factura f : facturas) {
            Paciente p = f.getPaciente();
            String codigoDiag = "";
            if (f.getOrden() != null && f.getOrden().getConsulta() != null) {
                String cie = f.getOrden().getConsulta().getCodigoCie10();
                if (cie != null && !cie.isBlank()) codigoDiag = cie;
            }
            us.append("").append("|") // codPrestador — configurar con NIT IPS
              .append(f.getFechaFactura() != null ? fmt.format(f.getFechaFactura()) : "").append("|")
              .append(nvl(f.getNumeroAutorizacionEps())).append("|")
              .append(nvl(p.getTipoDocumento())).append("|")
              .append(nvl(p.getDocumento())).append("|")
              .append("1|") // diasEstancia
              .append(nvl(f.getCodigoCups())).append("|")
              .append(codigoDiag).append("|") // codigoDiagnostico desde consulta (RIPS)
              .append(nvl(f.getTipoServicio())).append("|")
              .append(f.getValorTotal() != null ? f.getValorTotal().toPlainString() : "0").append("|")
              .append("0|")
              .append(f.getCuotaModeradora() != null ? f.getCuotaModeradora().toPlainString() : "0").append("|")
              .append("0|0")
              .append("\n");
        }

        // Archivo AP — Procedimientos
        StringBuilder ap = new StringBuilder();
        ap.append("codPrestador|fechaProcedimiento|idMedicoTratante|tipoDocumento|documento|codigoCups|ambito|finalidad|personalAtiende|valorProcedimiento\n");
        for (Factura f : facturas) {
            Paciente p = f.getPaciente();
            if (f.getCodigoCups() != null && !f.getCodigoCups().isBlank()) {
                ap.append("").append("|") // codPrestador
                  .append(f.getFechaFactura() != null ? fmt.format(f.getFechaFactura()) : "").append("|")
                  .append("").append("|") // idMedicoTratante
                  .append(nvl(p.getTipoDocumento())).append("|")
                  .append(nvl(p.getDocumento())).append("|")
                  .append(nvl(f.getCodigoCups())).append("|")
                  .append("2|") // ambito: 2=ambulatorio
                  .append("11|") // finalidad: 11=diagnóstico
                  .append("1|") // personalAtiende: 1=médico
                  .append(f.getValorTotal() != null ? f.getValorTotal().toPlainString() : "0")
                  .append("\n");
            }
        }

        // Archivo AC — Consultas externas
        StringBuilder ac = new StringBuilder();
        ac.append("codPrestador|fechaConsulta|numAutorizacion|tipoDocumento|documento|tipoConsulta|codigoCups|codigoDiagPpal|tipoDiagPpal|causaExterna|valorConsulta|valorCopago\n");
        for (Factura f : facturas) {
            if ("CONSULTA_EXTERNA".equals(f.getTipoServicio()) || "URGENCIAS".equals(f.getTipoServicio())) {
                Paciente p = f.getPaciente();
                String codigoDiagPpal = "";
                if (f.getOrden() != null && f.getOrden().getConsulta() != null) {
                    String cie = f.getOrden().getConsulta().getCodigoCie10();
                    if (cie != null && !cie.isBlank()) codigoDiagPpal = cie;
                }
                ac.append("").append("|")
                  .append(f.getFechaFactura() != null ? fmt.format(f.getFechaFactura()) : "").append("|")
                  .append(nvl(f.getNumeroAutorizacionEps())).append("|")
                  .append(nvl(p.getTipoDocumento())).append("|")
                  .append(nvl(p.getDocumento())).append("|")
                  .append("01|") // tipo consulta: 01=primera vez, 02=control
                  .append(nvl(f.getCodigoCups())).append("|")
                  .append(codigoDiagPpal).append("|") // codigoDiagPpal desde consulta (RIPS)
                  .append("01|") // tipo diagnostico: 01=impresión
                  .append("13|") // causa externa: 13=enfermedad general
                  .append(f.getValorTotal() != null ? f.getValorTotal().toPlainString() : "0").append("|")
                  .append(f.getCuotaModeradora() != null ? f.getCuotaModeradora().toPlainString() : "0")
                  .append("\n");
            }
        }

        // Archivo AM — Medicamentos (RIPS Res. 3374/2000 y 2275/2023)
        StringBuilder am = new StringBuilder();
        am.append("codPrestador|fechaDispensacion|numAutorizacion|tipoDocumento|documento|codigoMedicamento|tipoMedicamento|nombreMedicamento|cantidad|valorUnitario|valorTotal\n");
        for (Factura f : facturas) {
            Paciente p = f.getPaciente();
            boolean tieneItems = f.getItems() != null && !f.getItems().isEmpty();
            boolean esMedicamento = "MEDICAMENTO".equalsIgnoreCase(f.getTipoServicio()) || "COMPUESTA".equalsIgnoreCase(f.getTipoServicio());
            if (tieneItems) {
                for (var item : f.getItems()) {
                    if ("MEDICAMENTO".equalsIgnoreCase(item.getTipoServicio()) || item.getCodigoCups() != null) {
                        am.append("").append("|")
                          .append(f.getFechaFactura() != null ? fmt.format(f.getFechaFactura()) : "").append("|")
                          .append(nvl(f.getNumeroAutorizacionEps())).append("|")
                          .append(nvl(p.getTipoDocumento())).append("|")
                          .append(nvl(p.getDocumento())).append("|")
                          .append(nvl(item.getCodigoCups())).append("|")
                          .append("1|")
                          .append(nvl(item.getDescripcionCups()).replace("|", " ")).append("|")
                          .append(item.getCantidad() != null ? item.getCantidad() : 1).append("|")
                          .append(item.getValorUnitario() != null ? item.getValorUnitario().toPlainString() : "0").append("|")
                          .append(item.getValorTotal() != null ? item.getValorTotal().toPlainString() : "0")
                          .append("\n");
                    }
                }
            } else if (esMedicamento) {
                am.append("").append("|")
                  .append(f.getFechaFactura() != null ? fmt.format(f.getFechaFactura()) : "").append("|")
                  .append(nvl(f.getNumeroAutorizacionEps())).append("|")
                  .append(nvl(p.getTipoDocumento())).append("|")
                  .append(nvl(p.getDocumento())).append("|")
                  .append(nvl(f.getCodigoCups())).append("|")
                  .append("1|")
                  .append(nvl(f.getDescripcionCups()).replace("|", " ")).append("|")
                  .append("1|")
                  .append(f.getValorTotal() != null ? f.getValorTotal().toPlainString() : "0").append("|")
                  .append(f.getValorTotal() != null ? f.getValorTotal().toPlainString() : "0")
                  .append("\n");
            }
        }

        Map<String, String> resultado = new HashMap<>();
        resultado.put("CT", ct.toString());
        resultado.put("US", us.toString());
        resultado.put("AP", ap.toString());
        resultado.put("AC", ac.toString());
        resultado.put("AM", am.toString());
        resultado.put("resumen", "Facturas procesadas: " + facturas.size() + " | Rango: " + fmt.format(desde) + " - " + fmt.format(hasta));
        return resultado;
    }

    private String nvl(String s) { return s != null ? s.replace("|", " ") : ""; }
    private String primerApellido(String apellidos) {
        if (apellidos == null) return "";
        String[] partes = apellidos.trim().split("\\s+");
        return partes.length > 0 ? partes[0] : "";
    }
    private String segundoApellido(String apellidos) {
        if (apellidos == null) return "";
        String[] partes = apellidos.trim().split("\\s+");
        return partes.length > 1 ? partes[1] : "";
    }
    private String primerNombre(String nombres) {
        if (nombres == null) return "";
        String[] partes = nombres.trim().split("\\s+");
        return partes.length > 0 ? partes[0] : "";
    }
    private String segundoNombre(String nombres) {
        if (nombres == null) return "";
        String[] partes = nombres.trim().split("\\s+");
        return partes.length > 1 ? partes[1] : "";
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenFacturacionDto resumen() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        Instant inicioMes = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant();
        Instant inicioProximoMes = now.plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant();

        BigDecimal totalMes = facturaRepository.sumByFechaBetween(inicioMes, inicioProximoMes);
        long cantMes = facturaRepository.countByFechaBetween(inicioMes, inicioProximoMes);

        BigDecimal montoPendiente = facturaRepository.sumByEstado("PENDIENTE");
        long cantPendiente = facturaRepository.countByEstado("PENDIENTE");

        BigDecimal montoPagado = facturaRepository.sumByEstado("PAGADA");
        long cantPagada = facturaRepository.countByEstado("PAGADA");

        long cantAnulada = facturaRepository.countByEstado("ANULADA");
        long cantRechazada = facturaRepository.countByEstado("RECHAZADA");

        List<Factura> pendientesOEnProceso = new ArrayList<>();
        facturaRepository.findByEstadoIn(List.of("PENDIENTE", "EN_PROCESO")).forEach(pendientesOEnProceso::add);
        Instant instanteAhora = Instant.now();
        long cantVencidaRadicacion = 0;
        BigDecimal montoVencidoRadicacion = BigDecimal.ZERO;
        for (Factura fac : pendientesOEnProceso) {
            if (fac.getFechaFactura() != null && businessDaysBetween(fac.getFechaFactura(), instanteAhora) > DIAS_HABILES_RADICACION) {
                cantVencidaRadicacion++;
                montoVencidoRadicacion = montoVencidoRadicacion.add(fac.getValorTotal() != null ? fac.getValorTotal() : BigDecimal.ZERO);
            }
        }

        return ResumenFacturacionDto.builder()
                .totalFacturadoMes(totalMes)
                .cantidadMes(cantMes)
                .montoPendiente(montoPendiente)
                .cantidadPendiente(cantPendiente)
                .montoPagado(montoPagado)
                .cantidadPagada(cantPagada)
                .cantidadAnulada(cantAnulada)
                .cantidadRechazada(cantRechazada)
                .cantidadVencidaRadicacion(cantVencidaRadicacion)
                .montoVencidoRadicacion(montoVencidoRadicacion)
                .build();
    }

    private FacturaDto toDto(Factura f) {
        Paciente p = f.getPaciente();
        String pacienteNombre = p.getNombres() + " " +
                (p.getApellidos() != null ? p.getApellidos() : "");
        String epsNombre = (p.getEps() != null) ? p.getEps().getNombre() : null;
        String epsCodigo = (p.getEps() != null) ? p.getEps().getCodigo() : null;

        Long consultaId = null;
        if (f.getOrden() != null && f.getOrden().getConsulta() != null) {
            consultaId = f.getOrden().getConsulta().getId();
        }

        Integer diasParaRadicacion = null;
        Boolean vencidaRadicacion = false;
        if (f.getFechaFactura() != null && ("PENDIENTE".equals(f.getEstado()) || "EN_PROCESO".equals(f.getEstado()))) {
            long habilesTranscurridos = businessDaysBetween(f.getFechaFactura(), Instant.now());
            if (habilesTranscurridos > DIAS_HABILES_RADICACION) {
                vencidaRadicacion = true;
                diasParaRadicacion = 0;
            } else {
                diasParaRadicacion = (int) (DIAS_HABILES_RADICACION - habilesTranscurridos);
            }
        }

        List<FacturaItemDto> itemsDto = f.getItems() != null && !f.getItems().isEmpty()
                ? f.getItems().stream().map(this::toItemDto).collect(Collectors.toList())
                : null;

        return FacturaDto.builder()
                .id(f.getId())
                .numeroFactura(f.getNumeroFactura())
                .pacienteId(p.getId())
                .pacienteNombre(pacienteNombre.trim())
                .pacienteDocumento(p.getDocumento())
                .pacienteTipoDocumento(p.getTipoDocumento())
                .epsNombre(epsNombre)
                .epsCodigo(epsCodigo)
                .ordenId(f.getOrden() != null ? f.getOrden().getId() : null)
                .consultaId(consultaId)
                .valorTotal(f.getValorTotal())
                .estado(f.getEstado())
                .descripcion(f.getDescripcion())
                .fechaFactura(f.getFechaFactura())
                .createdAt(f.getCreatedAt())
                .codigoCups(f.getCodigoCups())
                .descripcionCups(f.getDescripcionCups())
                .tipoServicio(f.getTipoServicio())
                .responsablePago(f.getResponsablePago())
                .cuotaModeradora(f.getCuotaModeradora())
                .numeroAutorizacionEps(f.getNumeroAutorizacionEps())
                .dianEstado(f.getDianEstado())
                .dianCufe(f.getDianCufe())
                .dianQrUrl(f.getDianQrUrl())
                .diasParaRadicacion(diasParaRadicacion)
                .vencidaRadicacion(vencidaRadicacion)
                .items(itemsDto)
                .build();
    }

    private FacturaItemDto toItemDto(FacturaItem it) {
        return FacturaItemDto.builder()
                .id(it.getId())
                .itemIndex(it.getItemIndex())
                .codigoCups(it.getCodigoCups())
                .descripcionCups(it.getDescripcionCups())
                .tipoServicio(it.getTipoServicio())
                .cantidad(it.getCantidad())
                .valorUnitario(it.getValorUnitario())
                .valorTotal(it.getValorTotal())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrdenPendienteFacturaDto> getOrdenesPendientesDeFacturar(Pageable pageable) {
        return ordenClinicaRepository.findPendientesDeFacturar(pageable).map(this::toOrdenPendienteFacturaDto);
    }

    private OrdenPendienteFacturaDto toOrdenPendienteFacturaDto(OrdenClinica o) {
        Paciente p = o.getPaciente();
        String pacienteNombre = (p.getNombres() != null ? p.getNombres() : "") + " " +
                (p.getApellidos() != null ? p.getApellidos() : "");
        Long consultaId = o.getConsulta() != null ? o.getConsulta().getId() : null;
        return OrdenPendienteFacturaDto.builder()
                .id(o.getId())
                .pacienteId(p.getId())
                .pacienteNombre(pacienteNombre.trim())
                .pacienteDocumento(p.getDocumento())
                .tipo(o.getTipo())
                .detalle(o.getDetalle())
                .valorEstimado(o.getValorEstimado())
                .fechaOrden(o.getCreatedAt())
                .consultaId(consultaId)
                .estado(o.getEstado())
                .estadoDispensacionFarmacia(o.getEstadoDispensacionFarmacia())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AlertasFacturacionDto getAlertas() {
        List<AlertaFacturacionDto> alertas = new ArrayList<>();
        Instant now = Instant.now();
        int porVencer = 0, vencidas = 0;

        List<Factura> pendientes = facturaRepository.findByEstadoIn(List.of("PENDIENTE", "EN_PROCESO"));
        for (Factura f : pendientes) {
            if (f.getFechaFactura() == null || "ANULADA".equals(f.getEstado())) continue;
            long habiles = businessDaysBetween(f.getFechaFactura(), now);
            int diasRestantes = (int) (DIAS_HABILES_RADICACION - habiles);
            String epsNombre = f.getPaciente() != null && f.getPaciente().getEps() != null
                    ? f.getPaciente().getEps().getNombre() : null;
            BigDecimal monto = f.getValorTotal() != null ? f.getValorTotal() : BigDecimal.ZERO;

            if (habiles > DIAS_HABILES_RADICACION) {
                vencidas++;
                alertas.add(AlertaFacturacionDto.builder()
                        .tipo(AlertaFacturacionDto.Tipo.VENCIDA_RADICACION)
                        .titulo("Factura vencida para radicación")
                        .mensaje("Superó el plazo de " + DIAS_HABILES_RADICACION + " días hábiles (Res. 558/2024).")
                        .facturaId(f.getId())
                        .numeroFactura(f.getNumeroFactura())
                        .diasRestantes(0)
                        .monto(monto)
                        .epsNombre(epsNombre)
                        .build());
            } else if (diasRestantes <= 7 && diasRestantes > 0) {
                porVencer++;
                alertas.add(AlertaFacturacionDto.builder()
                        .tipo(AlertaFacturacionDto.Tipo.POR_VENCER_RADICACION)
                        .titulo("Próxima a vencer radicación")
                        .mensaje("Quedan " + diasRestantes + " días hábiles para radicar.")
                        .facturaId(f.getId())
                        .numeroFactura(f.getNumeroFactura())
                        .diasRestantes(diasRestantes)
                        .monto(monto)
                        .epsNombre(epsNombre)
                        .build());
            }
        }

        long glosasPendientesCount = glosaRepository.countByEstado("PENDIENTE");
        List<Glosa> glosasPendientes = glosaRepository.findByEstadoOrderByFechaRegistroDesc("PENDIENTE");
        int maxGlosasEnAlertas = 10;
        for (int i = 0; i < Math.min(glosasPendientes.size(), maxGlosasEnAlertas); i++) {
            Glosa g = glosasPendientes.get(i);
            Factura fac = g.getFactura();
            alertas.add(AlertaFacturacionDto.builder()
                    .tipo(AlertaFacturacionDto.Tipo.GLOSA_PENDIENTE)
                    .titulo("Glosa pendiente de respuesta")
                    .mensaje(g.getMotivoRechazo() != null ? g.getMotivoRechazo() : "Sin motivo registrado")
                    .facturaId(fac != null ? fac.getId() : null)
                    .numeroFactura(fac != null ? fac.getNumeroFactura() : null)
                    .glosaId(g.getId())
                    .build());
        }

        return AlertasFacturacionDto.builder()
                .alertas(alertas)
                .totalPorVencer(porVencer)
                .totalVencidas(vencidas)
                .totalGlosasPendientes((int) glosasPendientesCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ChecklistRadicacionDto getChecklistRadicacion(Long facturaId) {
        Factura f = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada: " + facturaId));
        List<String> errores = new ArrayList<>();
        List<String> advertencias = new ArrayList<>();

        if (f.getEstado() == null || "ANULADA".equals(f.getEstado())) {
            errores.add("La factura está anulada y no puede radicarse.");
        }
        if (f.getNumeroAutorizacionEps() == null || f.getNumeroAutorizacionEps().isBlank()) {
            errores.add("Falta número de autorización EPS (obligatorio para radicación).");
        }
        boolean tieneCups = (f.getCodigoCups() != null && !f.getCodigoCups().isBlank())
                || (f.getItems() != null && !f.getItems().isEmpty());
        if (!tieneCups) {
            errores.add("Debe registrar al menos un código CUPS (cabecera o ítems).");
        }
        if (f.getValorTotal() == null || f.getValorTotal().compareTo(BigDecimal.ZERO) <= 0) {
            errores.add("El valor total debe ser mayor a cero.");
        }
        Paciente p = f.getPaciente();
        if (p != null) {
            if (p.getDocumento() == null || p.getDocumento().isBlank()) {
                errores.add("Paciente sin documento de identidad (requerido RIPS).");
            }
            if (p.getTipoDocumento() == null || p.getTipoDocumento().isBlank()) {
                advertencias.add("Paciente sin tipo de documento; verificar para RIPS CT.");
            }
            if (p.getMunicipioResidencia() == null || p.getMunicipioResidencia().isBlank()) {
                advertencias.add("Paciente sin municipio de residencia (código DANE para RIPS).");
            }
        }

        boolean listo = errores.isEmpty();
        String resumen = listo
                ? (advertencias.isEmpty() ? "Lista para radicar." : "Cumple requisitos. Revise las advertencias.")
                : "Corrija los errores antes de radicar.";

        return ChecklistRadicacionDto.builder()
                .listo(listo)
                .errores(errores)
                .advertencias(advertencias)
                .resumen(resumen)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BandejaFacturadorDto getBandejaFacturador() {
        List<TareaFacturadorDto> tareas = new ArrayList<>();
        List<Factura> pendientes = facturaRepository.findByEstadoIn(List.of("PENDIENTE", "EN_PROCESO"));
        int facturasPorRadicar = 0;
        for (Factura f : pendientes) {
            if (f.getFechaFactura() == null) continue;
            facturasPorRadicar++;
            long habiles = businessDaysBetween(f.getFechaFactura(), Instant.now());
            int diasRestantes = (int) (DIAS_HABILES_RADICACION - habiles);
            boolean vencida = habiles > DIAS_HABILES_RADICACION;
            if (tareas.size() < 20) {
                tareas.add(TareaFacturadorDto.builder()
                        .tipo(TareaFacturadorDto.TipoTarea.RADICAR)
                        .id(f.getId())
                        .referencia(f.getNumeroFactura() != null ? f.getNumeroFactura() : "Factura #" + f.getId())
                        .descripcion(vencida ? "Vencida: radicar con prontitud" : "Radicar en los próximos " + diasRestantes + " días hábiles")
                        .diasRestantes(Math.max(0, diasRestantes))
                        .vencida(vencida)
                        .monto(f.getValorTotal())
                        .build());
            }
        }
        List<Glosa> glosasPend = glosaRepository.findByEstadoOrderByFechaRegistroDesc("PENDIENTE");
        int totalGlosasPendientes = glosasPend.size();
        for (int i = 0; i < Math.min(glosasPend.size(), 15); i++) {
            Glosa g = glosasPend.get(i);
            Factura fac = g.getFactura();
            tareas.add(TareaFacturadorDto.builder()
                    .tipo(TareaFacturadorDto.TipoTarea.RESPONDER_GLOSA)
                    .id(g.getId())
                    .referencia("Glosa #" + g.getId())
                    .descripcion(fac != null ? fac.getNumeroFactura() + " — " + (g.getMotivoRechazo() != null ? g.getMotivoRechazo() : "") : g.getMotivoRechazo())
                    .vencida(false)
                    .build());
        }
        return BandejaFacturadorDto.builder()
                .tareas(tareas)
                .totalFacturasPorRadicar(facturasPorRadicar)
                .totalGlosasPendientes(totalGlosasPendientes)
                .build();
    }

    @Override
    @Transactional
    public FacturaLoteResultDto createFromLote(List<Long> ordenIds) {
        if (ordenIds == null || ordenIds.isEmpty()) {
            return FacturaLoteResultDto.builder()
                    .facturasCreadas(List.of())
                    .errores(List.of("No se indicaron órdenes."))
                    .totalProcesadas(0)
                    .totalCreadas(0)
                    .build();
        }
        List<OrdenClinica> todas = ordenClinicaRepository.findAllById(ordenIds);
        List<OrdenClinica> pendientes = todas.stream()
                .filter(o -> !facturaRepository.existsByOrden_Id(o.getId()))
                .toList();
        if (pendientes.isEmpty()) {
            return FacturaLoteResultDto.builder()
                    .facturasCreadas(List.of())
                    .errores(List.of("Ninguna de las órdenes está pendiente de facturar (ya tienen factura asociada)."))
                    .totalProcesadas(ordenIds.size())
                    .totalCreadas(0)
                    .build();
        }
        Map<Long, List<OrdenClinica>> porPaciente = pendientes.stream()
                .collect(Collectors.groupingBy(o -> o.getPaciente().getId(), LinkedHashMap::new, Collectors.toList()));
        List<FacturaDto> creadas = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        for (List<OrdenClinica> grupo : porPaciente.values()) {
            if (grupo.isEmpty()) continue;
            OrdenClinica primera = grupo.get(0);
            Paciente paciente = primera.getPaciente();
            List<FacturaItemRequestDto> items = new ArrayList<>();
            BigDecimal valorTotal = BigDecimal.ZERO;
            int idx = 0;
            for (OrdenClinica o : grupo) {
                BigDecimal valor = o.getValorEstimado() != null ? o.getValorEstimado() : BigDecimal.ZERO;
                items.add(FacturaItemRequestDto.builder()
                        .itemIndex(idx++)
                        .codigoCups(null)
                        .descripcionCups(o.getDetalle() != null ? o.getDetalle() : o.getTipo())
                        .tipoServicio(o.getTipo())
                        .cantidad(1)
                        .valorUnitario(valor)
                        .valorTotal(valor)
                        .build());
                valorTotal = valorTotal.add(valor);
            }
            try {
                FacturaRequestDto dto = FacturaRequestDto.builder()
                        .pacienteId(paciente.getId())
                        .ordenId(primera.getId())
                        .fechaFactura(Instant.now())
                        .estado("PENDIENTE")
                        .descripcion("Factura por lote: " + grupo.size() + " orden(es)")
                        .valorTotal(valorTotal)
                        .items(items)
                        .build();
                creadas.add(create(dto));
            } catch (Exception e) {
                errores.add("Paciente " + (paciente.getNombres() != null ? paciente.getNombres() : paciente.getId()) + ": " + e.getMessage());
            }
        }
        return FacturaLoteResultDto.builder()
                .facturasCreadas(creadas)
                .errores(errores)
                .totalProcesadas(ordenIds.size())
                .totalCreadas(creadas.size())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public String exportLibroCsv(Instant desde, Instant hasta, String estado) {
        Specification<Factura> spec = buildSpec(estado, desde, hasta, null);
        List<Factura> list = facturaRepository.findAll(spec, org.springframework.data.domain.Sort.by("fechaFactura"));
        DateTimeFormatter dateFmt = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZONE_COLOMBIA);
        StringBuilder sb = new StringBuilder();
        sb.append("numero_factura,fecha_factura,paciente_documento,paciente_nombre,eps,codigo_cups,tipo_servicio,responsable_pago,valor_total,estado,numero_autorizacion_eps\n");
        for (Factura f : list) {
            FacturaDto dto = toDto(f);
            sb.append(escapeCsv(dto.getNumeroFactura())).append(",")
                    .append(dto.getFechaFactura() != null ? dateFmt.format(dto.getFechaFactura()) : "").append(",")
                    .append(escapeCsv(dto.getPacienteDocumento())).append(",")
                    .append(escapeCsv(dto.getPacienteNombre())).append(",")
                    .append(escapeCsv(dto.getEpsNombre())).append(",")
                    .append(escapeCsv(dto.getCodigoCups())).append(",")
                    .append(escapeCsv(dto.getTipoServicio())).append(",")
                    .append(escapeCsv(dto.getResponsablePago())).append(",")
                    .append(dto.getValorTotal() != null ? dto.getValorTotal() : "").append(",")
                    .append(escapeCsv(dto.getEstado())).append(",")
                    .append(escapeCsv(dto.getNumeroAutorizacionEps())).append("\n");
        }
        return sb.toString();
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
