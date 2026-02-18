/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sesa.salud.dto.SyncBatchRequest.SyncOperationItem;
import com.sesa.salud.dto.SyncBatchResponse.SyncItemResult;
import com.sesa.salud.entity.Consulta;
import com.sesa.salud.entity.OrdenClinica;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.repository.ConsultaRepository;
import com.sesa.salud.repository.OrdenClinicaRepository;
import com.sesa.salud.repository.PacienteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handler de sincronización offline para la entidad Ordenes Clínicas.
 * Soporta POST (crear).
 */
@Component("ordenes-clinicas")
@RequiredArgsConstructor
@Slf4j
public class OrdenClinicaSyncHandler implements SyncEntityHandler {

    private final OrdenClinicaRepository ordenClinicaRepository;
    private final PacienteRepository pacienteRepository;
    private final ConsultaRepository consultaRepository;
    private final ObjectMapper objectMapper;

    private static final Pattern ID_PATTERN = Pattern.compile("/ordenes-clinicas/(\\d+)");

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
            log.error("Error en OrdenClinicaSyncHandler: {}", e.getMessage(), e);
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(500)
                    .error("Error procesando orden clinica: " + e.getMessage()).build();
        }
    }

    private SyncItemResult handleCreate(SyncOperationItem op) throws Exception {
        Map<String, Object> body = toMap(op.getBody());
        
        Long pacienteId = toLong(body.get("pacienteId"));
        Long consultaId = toLong(body.get("consultaId"));
        
        if (pacienteId == null) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("pacienteId es obligatorio").build();
        }
        
        if (consultaId == null) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("consultaId es obligatorio").build();
        }
        
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + pacienteId));
        
        Consulta consulta = consultaRepository.findById(consultaId)
                .orElseThrow(() -> new RuntimeException("Consulta no encontrada: " + consultaId));
        
        String tipo = (String) body.get("tipo");
        if (tipo == null || tipo.trim().isEmpty()) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("tipo es obligatorio").build();
        }
        
        Object valorEstimadoObj = body.get("valorEstimado");
        BigDecimal valorEstimado = null;
        if (valorEstimadoObj != null) {
            if (valorEstimadoObj instanceof Number) {
                valorEstimado = BigDecimal.valueOf(((Number) valorEstimadoObj).doubleValue());
            } else if (valorEstimadoObj instanceof String) {
                valorEstimado = new BigDecimal((String) valorEstimadoObj);
            }
        }
        
        OrdenClinica oc = OrdenClinica.builder()
                .paciente(paciente)
                .consulta(consulta)
                .tipo(tipo)
                .detalle((String) body.get("detalle"))
                .estado((String) body.getOrDefault("estado", "PENDIENTE"))
                .valorEstimado(valorEstimado)
                .build();
        
        oc = ordenClinicaRepository.save(oc);
        
        return SyncItemResult.builder()
                .clientId(op.getClientId()).success(true).status(201)
                .serverId(oc.getId()).build();
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
