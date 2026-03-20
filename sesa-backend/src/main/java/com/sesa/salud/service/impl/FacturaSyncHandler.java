/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sesa.salud.dto.SyncBatchRequest.SyncOperationItem;
import com.sesa.salud.dto.SyncBatchResponse.SyncItemResult;
import com.sesa.salud.entity.Factura;
import com.sesa.salud.entity.OrdenClinica;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.repository.FacturaRepository;
import com.sesa.salud.repository.OrdenClinicaRepository;
import com.sesa.salud.repository.PacienteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handler de sincronización offline para la entidad Facturas.
 * Soporta POST (crear).
 */
@Component("facturas")
@RequiredArgsConstructor
@Slf4j
public class FacturaSyncHandler implements SyncEntityHandler {

    private final FacturaRepository facturaRepository;
    private final PacienteRepository pacienteRepository;
    private final OrdenClinicaRepository ordenClinicaRepository;
    private final ObjectMapper objectMapper;

    private static final Pattern ID_PATTERN = Pattern.compile("/facturas/(\\d+)");

    @Override
    public SyncItemResult handle(SyncOperationItem op, String userEmail) {
        String method = op.getMethod().toUpperCase();
        try {
            return switch (method) {
                case "POST" -> handleCreate(op);
                default -> SyncItemResult.builder()
                        .clientId(op.getClientId()).success(false).status(405)
                        .error("Método no soportado: " + method).build();
            };
        } catch (Exception e) {
            log.error("Error en FacturaSyncHandler: {}", e.getMessage(), e);
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(500)
                    .error("Error procesando factura: " + e.getMessage()).build();
        }
    }

    private SyncItemResult handleCreate(SyncOperationItem op) throws Exception {
        Map<String, Object> body = toMap(op.getBody());
        
        Long pacienteId = toLong(body.get("pacienteId"));
        
        if (pacienteId == null) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("pacienteId es obligatorio").build();
        }
        
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + pacienteId));
        
        OrdenClinica orden = null;
        Long ordenId = toLong(body.get("ordenId"));
        if (ordenId != null) {
            orden = ordenClinicaRepository.findById(ordenId)
                    .orElse(null);
        }
        
        Object valorTotalObj = body.get("valorTotal");
        BigDecimal valorTotal = null;
        if (valorTotalObj != null) {
            if (valorTotalObj instanceof Number) {
                valorTotal = BigDecimal.valueOf(((Number) valorTotalObj).doubleValue());
            } else if (valorTotalObj instanceof String) {
                valorTotal = new BigDecimal((String) valorTotalObj);
            }
        }
        
        if (valorTotal == null) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("valorTotal es obligatorio").build();
        }
        
        Factura f = Factura.builder()
                .numeroFactura((String) body.get("numeroFactura"))
                .paciente(paciente)
                .orden(orden)
                .valorTotal(valorTotal)
                .estado((String) body.getOrDefault("estado", "PENDIENTE"))
                .descripcion((String) body.get("descripcion"))
                .fechaFactura(Instant.now())
                .build();
        
        f = facturaRepository.save(f);
        
        return SyncItemResult.builder()
                .clientId(op.getClientId()).success(true).status(201)
                .serverId(f.getId()).build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object body) throws Exception {
        if (body instanceof Map) return (Map<String, Object>) body;
        return objectMapper.convertValue(body, Map.class);
    }

    private Long extractId(String url) {
        if (url == null) return null;
        Matcher m = ID_PATTERN.matcher(url);
        return m.find() ? Long.parseLong(m.group(1)) : null;
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
