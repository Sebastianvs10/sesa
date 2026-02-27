/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service.impl;

import com.sesa.salud.dto.FacturaDto;
import com.sesa.salud.dto.FacturaRequestDto;
import com.sesa.salud.dto.ResumenFacturacionDto;
import com.sesa.salud.entity.Factura;
import com.sesa.salud.entity.OrdenClinica;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.repository.FacturaRepository;
import com.sesa.salud.repository.OrdenClinicaRepository;
import com.sesa.salud.repository.PacienteRepository;
import com.sesa.salud.service.FacturaService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FacturaServiceImpl implements FacturaService {

    private final FacturaRepository facturaRepository;
    private final PacienteRepository pacienteRepository;
    private final OrdenClinicaRepository ordenClinicaRepository;

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

        // Generar número de factura automático si no se proporcionó
        String numeroFactura = dto.getNumeroFactura();
        if (numeroFactura == null || numeroFactura.isBlank()) {
            long consecutivo = facturaRepository.count() + 1;
            int anio = Year.now().getValue();
            numeroFactura = String.format("FV-%d-%06d", anio, consecutivo);
        }

        Factura factura = Factura.builder()
                .numeroFactura(numeroFactura)
                .paciente(paciente)
                .orden(orden)
                .valorTotal(dto.getValorTotal())
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
        return toDto(facturaRepository.save(factura));
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
            us.append("").append("|") // codPrestador — configurar con NIT IPS
              .append(f.getFechaFactura() != null ? fmt.format(f.getFechaFactura()) : "").append("|")
              .append(nvl(f.getNumeroAutorizacionEps())).append("|")
              .append(nvl(p.getTipoDocumento())).append("|")
              .append(nvl(p.getDocumento())).append("|")
              .append("1|") // diasEstancia
              .append(nvl(f.getCodigoCups())).append("|")
              .append("").append("|") // codigoDiagnostico — extendible desde consulta
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
                ac.append("").append("|")
                  .append(f.getFechaFactura() != null ? fmt.format(f.getFechaFactura()) : "").append("|")
                  .append(nvl(f.getNumeroAutorizacionEps())).append("|")
                  .append(nvl(p.getTipoDocumento())).append("|")
                  .append(nvl(p.getDocumento())).append("|")
                  .append("01|") // tipo consulta: 01=primera vez, 02=control
                  .append(nvl(f.getCodigoCups())).append("|")
                  .append("").append("|") // codigoDiagPpal
                  .append("01|") // tipo diagnostico: 01=impresión
                  .append("13|") // causa externa: 13=enfermedad general
                  .append(f.getValorTotal() != null ? f.getValorTotal().toPlainString() : "0").append("|")
                  .append(f.getCuotaModeradora() != null ? f.getCuotaModeradora().toPlainString() : "0")
                  .append("\n");
            }
        }

        Map<String, String> resultado = new HashMap<>();
        resultado.put("CT", ct.toString());
        resultado.put("US", us.toString());
        resultado.put("AP", ap.toString());
        resultado.put("AC", ac.toString());
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

        return ResumenFacturacionDto.builder()
                .totalFacturadoMes(totalMes)
                .cantidadMes(cantMes)
                .montoPendiente(montoPendiente)
                .cantidadPendiente(cantPendiente)
                .montoPagado(montoPagado)
                .cantidadPagada(cantPagada)
                .cantidadAnulada(cantAnulada)
                .cantidadRechazada(cantRechazada)
                .build();
    }

    private FacturaDto toDto(Factura f) {
        Paciente p = f.getPaciente();
        String pacienteNombre = p.getNombres() + " " +
                (p.getApellidos() != null ? p.getApellidos() : "");
        String epsNombre = (p.getEps() != null) ? p.getEps().getNombre() : null;
        String epsCodigo = (p.getEps() != null) ? p.getEps().getCodigo() : null;

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
                .build();
    }
}
