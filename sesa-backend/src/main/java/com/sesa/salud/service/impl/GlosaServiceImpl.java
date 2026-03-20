/**
 * S9: Gestión de glosas y recuperación de cartera.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service.impl;

import com.sesa.salud.dto.GlosaAdjuntoDto;
import com.sesa.salud.dto.GlosaDto;
import com.sesa.salud.dto.GlosaRequestDto;
import com.sesa.salud.dto.GlosaResumenDto;
import com.sesa.salud.dto.RecuperacionCarteraDto;
import com.sesa.salud.entity.Factura;
import com.sesa.salud.entity.Glosa;
import com.sesa.salud.entity.GlosaAdjunto;
import com.sesa.salud.entity.Usuario;
import com.sesa.salud.repository.FacturaRepository;
import com.sesa.salud.repository.GlosaAdjuntoRepository;
import com.sesa.salud.repository.GlosaRepository;
import com.sesa.salud.service.ArchivoService;
import com.sesa.salud.service.GlosaService;
import com.sesa.salud.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GlosaServiceImpl implements GlosaService {

    private static final String ENTIDAD_ADJUNTO = "GLOSA_ADJUNTO";

    private final GlosaRepository glosaRepository;
    private final GlosaAdjuntoRepository glosaAdjuntoRepository;
    private final FacturaRepository facturaRepository;
    private final ArchivoService archivoService;

    @Override
    @Transactional
    public GlosaDto create(GlosaRequestDto dto, Long creadoPorId) {
        Factura factura = facturaRepository.findById(dto.getFacturaId())
                .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada: " + dto.getFacturaId()));
        Usuario creadoPor = null;
        if (creadoPorId != null) {
            creadoPor = new Usuario();
            creadoPor.setId(creadoPorId);
        }
        Glosa g = Glosa.builder()
                .factura(factura)
                .motivoRechazo(dto.getMotivoRechazo())
                .estado(dto.getEstado() != null && !dto.getEstado().isBlank() ? dto.getEstado() : "PENDIENTE")
                .observaciones(dto.getObservaciones())
                .creadoPor(creadoPor)
                .build();
        g = glosaRepository.save(g);
        return toDto(g);
    }

    @Override
    @Transactional
    public GlosaDto update(Long id, GlosaRequestDto dto) {
        Glosa g = glosaRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Glosa no encontrada: " + id));
        g.setMotivoRechazo(dto.getMotivoRechazo());
        if (dto.getEstado() != null && !dto.getEstado().isBlank()) g.setEstado(dto.getEstado());
        g.setObservaciones(dto.getObservaciones());
        g = glosaRepository.save(g);
        return toDto(g);
    }

    @Override
    @Transactional(readOnly = true)
    public GlosaDto findById(Long id) {
        Glosa g = glosaRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Glosa no encontrada: " + id));
        return toDto(g);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GlosaDto> findByFacturaId(Long facturaId) {
        return glosaRepository.findByFactura_IdOrderByFechaRegistroDesc(facturaId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<GlosaDto> list(String estado, Instant desde, Instant hasta, Long facturaId) {
        if (facturaId != null) {
            return findByFacturaId(facturaId);
        }
        if (estado != null && !estado.isBlank() && desde != null && hasta != null) {
            return glosaRepository.findByFechaRegistroBetweenOrderByFechaRegistroDesc(desde, hasta).stream()
                    .filter(gl -> estado.equals(gl.getEstado()))
                    .map(this::toDto).collect(Collectors.toList());
        }
        if (desde != null && hasta != null) {
            return glosaRepository.findByFechaRegistroBetweenOrderByFechaRegistroDesc(desde, hasta).stream()
                    .map(this::toDto).collect(Collectors.toList());
        }
        if (estado != null && !estado.isBlank()) {
            return glosaRepository.findByEstadoOrderByFechaRegistroDesc(estado).stream()
                    .map(this::toDto).collect(Collectors.toList());
        }
        return glosaRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public GlosaDto cambiarEstado(Long id, String estado) {
        Glosa g = glosaRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Glosa no encontrada: " + id));
        g.setEstado(estado);
        if ("ENVIADO".equals(estado) || "ACEPTADO".equals(estado) || "RECHAZADO".equals(estado)) {
            g.setFechaRespuesta(Instant.now());
        }
        g = glosaRepository.save(g);
        return toDto(g);
    }

    @Override
    @Transactional
    public GlosaDto addAdjunto(Long glosaId, String nombreArchivo, String tipo, String urlOBlob) {
        Glosa g = glosaRepository.findById(glosaId).orElseThrow(() -> new IllegalArgumentException("Glosa no encontrada: " + glosaId));
        GlosaAdjunto adj = GlosaAdjunto.builder()
                .glosa(g)
                .nombreArchivo(nombreArchivo)
                .tipo(tipo)
                .urlOBlob(urlOBlob)
                .build();
        glosaAdjuntoRepository.save(adj);
        g.getAdjuntos().add(adj);
        return toDto(glosaRepository.save(g));
    }

    @Override
    @Transactional
    public GlosaDto uploadAdjunto(Long glosaId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede estar vacío");
        }
        String schema = TenantContextHolder.getTenantSchema();
        if (schema == null) schema = TenantContextHolder.PUBLIC;
        String uuid = archivoService.guardar(file, ENTIDAD_ADJUNTO, String.valueOf(glosaId), schema, false);
        String url = "/api/archivos/" + uuid;
        String nombre = file.getOriginalFilename() != null ? file.getOriginalFilename() : "adjunto";
        String tipo = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        return addAdjunto(glosaId, nombre, tipo, url);
    }

    @Override
    @Transactional(readOnly = true)
    public RecuperacionCarteraDto recuperacionCartera(Instant desde, Instant hasta, Long contratoId) {
        List<Glosa> glosas = glosaRepository.findByFechaRegistroBetweenOrderByFechaRegistroDesc(desde, hasta);
        if (contratoId != null) {
            // Si en el futuro hay contrato en factura/glosa, filtrar por contratoId
        }
        long pendientes = glosas.stream().filter(g -> "PENDIENTE".equals(g.getEstado())).count();
        long enviadas = glosas.stream().filter(g -> "ENVIADO".equals(g.getEstado())).count();
        long aceptadas = glosas.stream().filter(g -> "ACEPTADO".equals(g.getEstado())).count();
        long rechazadas = glosas.stream().filter(g -> "RECHAZADO".equals(g.getEstado())).count();
        BigDecimal totalRecuperado = BigDecimal.ZERO;
        for (Glosa g : glosas) {
            if ("ACEPTADO".equals(g.getEstado()) && g.getFactura() != null) {
                totalRecuperado = totalRecuperado.add(g.getFactura().getValorTotal() != null ? g.getFactura().getValorTotal() : BigDecimal.ZERO);
            }
        }
        Map<String, Long> porEstadoMap = glosas.stream()
                .collect(Collectors.groupingBy(Glosa::getEstado, Collectors.counting()));
        List<GlosaResumenDto> porEstado = new ArrayList<>();
        for (Map.Entry<String, Long> e : porEstadoMap.entrySet()) {
            porEstado.add(GlosaResumenDto.builder().estado(e.getKey()).cantidad(e.getValue()).build());
        }
        return RecuperacionCarteraDto.builder()
                .totalGlosas(glosas.size())
                .pendientes(pendientes)
                .enviadas(enviadas)
                .aceptadas(aceptadas)
                .rechazadas(rechazadas)
                .totalRecuperado(totalRecuperado)
                .porEstado(porEstado)
                .build();
    }

    private GlosaDto toDto(Glosa g) {
        String numeroFactura = null;
        if (g.getFactura() != null) {
            numeroFactura = g.getFactura().getNumeroFactura();
        }
        Long creadoPorId = null;
        String creadoPorNombre = null;
        if (g.getCreadoPor() != null) {
            creadoPorId = g.getCreadoPor().getId();
            creadoPorNombre = g.getCreadoPor().getNombreCompleto();
        }
        List<GlosaAdjunto> adjuntosList = glosaAdjuntoRepository.findByGlosaIdOrderByCreatedAtAsc(g.getId());
        List<GlosaAdjuntoDto> adjuntos = adjuntosList.stream().map(a -> GlosaAdjuntoDto.builder()
                .id(a.getId())
                .glosaId(a.getGlosa().getId())
                .nombreArchivo(a.getNombreArchivo())
                .tipo(a.getTipo())
                .urlOBlob(a.getUrlOBlob())
                .createdAt(a.getCreatedAt())
                .build()).collect(Collectors.toList());
        return GlosaDto.builder()
                .id(g.getId())
                .facturaId(g.getFactura() != null ? g.getFactura().getId() : null)
                .numeroFactura(numeroFactura)
                .motivoRechazo(g.getMotivoRechazo())
                .estado(g.getEstado())
                .fechaRegistro(g.getFechaRegistro())
                .fechaRespuesta(g.getFechaRespuesta())
                .observaciones(g.getObservaciones())
                .creadoPorId(creadoPorId)
                .creadoPorNombre(creadoPorNombre)
                .createdAt(g.getCreatedAt())
                .updatedAt(g.getUpdatedAt())
                .adjuntos(adjuntos)
                .build();
    }
}
