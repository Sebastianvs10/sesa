/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sesa.salud.dto.SyncBatchRequest.SyncOperationItem;
import com.sesa.salud.dto.SyncBatchResponse.SyncItemResult;
import com.sesa.salud.entity.FarmaciaDispensacion;
import com.sesa.salud.entity.FarmaciaMedicamento;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.repository.FarmaciaDispensacionRepository;
import com.sesa.salud.repository.FarmaciaMedicamentoRepository;
import com.sesa.salud.repository.PacienteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handler de sincronización offline para la entidad Farmacia.
 * Soporta POST a /farmacia/medicamentos (crear medicamento) y
 * POST a /farmacia/dispensar (crear dispensación).
 */
@Component("farmacia")
@RequiredArgsConstructor
@Slf4j
public class FarmaciaSyncHandler implements SyncEntityHandler {

    private final FarmaciaMedicamentoRepository farmaciaMedicamentoRepository;
    private final FarmaciaDispensacionRepository farmaciaDispensacionRepository;
    private final PacienteRepository pacienteRepository;
    private final ObjectMapper objectMapper;

    private static final Pattern MEDICAMENTO_ID_PATTERN = Pattern.compile("/farmacia/medicamentos/(\\d+)");
    private static final Pattern DISPENSACION_ID_PATTERN = Pattern.compile("/farmacia/dispensaciones/(\\d+)");

    @Override
    public SyncItemResult handle(SyncOperationItem op, String userEmail) {
        String method = op.getMethod().toUpperCase();
        String url = op.getUrl();
        
        try {
            if ("POST".equals(method)) {
                if (url != null && url.contains("/farmacia/medicamentos")) {
                    return handleCreateMedicamento(op);
                } else if (url != null && url.contains("/farmacia/dispensar")) {
                    return handleCreateDispensacion(op);
                }
            }
            
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(405)
                    .error("Método o URL no soportado: " + method + " " + url).build();
        } catch (Exception e) {
            log.error("Error en FarmaciaSyncHandler: {}", e.getMessage(), e);
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(500)
                    .error("Error procesando farmacia: " + e.getMessage()).build();
        }
    }

    private SyncItemResult handleCreateMedicamento(SyncOperationItem op) throws Exception {
        Map<String, Object> body = toMap(op.getBody());
        
        String nombre = (String) body.get("nombre");
        if (nombre == null || nombre.trim().isEmpty()) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("nombre es obligatorio").build();
        }
        
        Object cantidadObj = body.get("cantidad");
        Integer cantidad = cantidadObj != null ? toInteger(cantidadObj) : 0;
        
        Object precioObj = body.get("precio");
        BigDecimal precio = null;
        if (precioObj != null) {
            if (precioObj instanceof Number) {
                precio = BigDecimal.valueOf(((Number) precioObj).doubleValue());
            } else if (precioObj instanceof String) {
                precio = new BigDecimal((String) precioObj);
            }
        }
        
        String fechaVencimientoStr = (String) body.get("fechaVencimiento");
        LocalDate fechaVencimiento = fechaVencimientoStr != null ? LocalDate.parse(fechaVencimientoStr) : null;
        
        FarmaciaMedicamento m = FarmaciaMedicamento.builder()
                .nombre(nombre)
                .lote((String) body.get("lote"))
                .fechaVencimiento(fechaVencimiento)
                .cantidad(cantidad)
                .precio(precio)
                .stockMinimo(toInteger(body.get("stockMinimo")))
                .activo(true)
                .build();
        
        m = farmaciaMedicamentoRepository.save(m);
        
        return SyncItemResult.builder()
                .clientId(op.getClientId()).success(true).status(201)
                .serverId(m.getId()).build();
    }

    private SyncItemResult handleCreateDispensacion(SyncOperationItem op) throws Exception {
        Map<String, Object> body = toMap(op.getBody());
        
        Long medicamentoId = toLong(body.get("medicamentoId"));
        Long pacienteId = toLong(body.get("pacienteId"));
        
        if (medicamentoId == null) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("medicamentoId es obligatorio").build();
        }
        
        if (pacienteId == null) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("pacienteId es obligatorio").build();
        }
        
        FarmaciaMedicamento medicamento = farmaciaMedicamentoRepository.findById(medicamentoId)
                .orElseThrow(() -> new RuntimeException("Medicamento no encontrado: " + medicamentoId));
        
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + pacienteId));
        
        Object cantidadObj = body.get("cantidad");
        Integer cantidad = cantidadObj != null ? toInteger(cantidadObj) : null;
        
        if (cantidad == null) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("cantidad es obligatorio").build();
        }
        
        String fechaDispensacionStr = (String) body.get("fechaDispensacion");
        LocalDateTime fechaDispensacion = fechaDispensacionStr != null ? LocalDateTime.parse(fechaDispensacionStr) : LocalDateTime.now();
        
        FarmaciaDispensacion d = FarmaciaDispensacion.builder()
                .medicamento(medicamento)
                .paciente(paciente)
                .cantidad(cantidad)
                .fechaDispensacion(fechaDispensacion)
                .entregadoPor((String) body.get("entregadoPor"))
                .build();
        
        d = farmaciaDispensacionRepository.save(d);
        
        return SyncItemResult.builder()
                .clientId(op.getClientId()).success(true).status(201)
                .serverId(d.getId()).build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object body) throws Exception {
        if (body instanceof Map) return (Map<String, Object>) body;
        return objectMapper.convertValue(body, Map.class);
    }

    private Long extractId(String url) {
        if (url == null) return null;
        Matcher m = MEDICAMENTO_ID_PATTERN.matcher(url);
        if (m.find()) return Long.parseLong(m.group(1));
        m = DISPENSACION_ID_PATTERN.matcher(url);
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

    private Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
