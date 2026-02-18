/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sesa.salud.dto.SyncBatchRequest.SyncOperationItem;
import com.sesa.salud.dto.SyncBatchResponse.SyncItemResult;
import com.sesa.salud.entity.Hospitalizacion;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.repository.HospitalizacionRepository;
import com.sesa.salud.repository.PacienteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handler de sincronización offline para la entidad Hospitalizaciones.
 * Soporta POST (crear) y PUT (actualizar).
 */
@Component("hospitalizaciones")
@RequiredArgsConstructor
@Slf4j
public class HospitalizacionSyncHandler implements SyncEntityHandler {

    private final HospitalizacionRepository hospitalizacionRepository;
    private final PacienteRepository pacienteRepository;
    private final ObjectMapper objectMapper;

    private static final Pattern ID_PATTERN = Pattern.compile("/hospitalizaciones/(\\d+)");

    @Override
    public SyncItemResult handle(SyncOperationItem op, String userEmail) {
        String method = op.getMethod().toUpperCase();
        try {
            return switch (method) {
                case "POST" -> handleCreate(op);
                case "PUT", "PATCH" -> handleUpdate(op);
                default -> SyncItemResult.builder()
                        .clientId(op.getClientId()).success(false).status(405)
                        .error("Método no soportado: " + method).build();
            };
        } catch (Exception e) {
            log.error("Error en HospitalizacionSyncHandler: {}", e.getMessage(), e);
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(500)
                    .error("Error procesando hospitalizacion: " + e.getMessage()).build();
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
        
        String fechaIngresoStr = (String) body.get("fechaIngreso");
        LocalDateTime fechaIngreso = fechaIngresoStr != null ? LocalDateTime.parse(fechaIngresoStr) : LocalDateTime.now();
        
        Hospitalizacion h = Hospitalizacion.builder()
                .paciente(paciente)
                .servicio((String) body.get("servicio"))
                .cama((String) body.get("cama"))
                .estado((String) body.getOrDefault("estado", "INGRESADO"))
                .fechaIngreso(fechaIngreso)
                .evolucionDiaria((String) body.get("evolucionDiaria"))
                .ordenesMedicas((String) body.get("ordenesMedicas"))
                .epicrisis((String) body.get("epicrisis"))
                .build();
        
        h = hospitalizacionRepository.save(h);
        
        return SyncItemResult.builder()
                .clientId(op.getClientId()).success(true).status(201)
                .serverId(h.getId()).build();
    }

    private SyncItemResult handleUpdate(SyncOperationItem op) throws Exception {
        Long id = extractId(op.getUrl());
        if (id == null) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("No se pudo extraer ID de la URL: " + op.getUrl()).build();
        }

        Optional<Hospitalizacion> opt = hospitalizacionRepository.findById(id);
        if (opt.isEmpty()) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(404)
                    .error("Hospitalizacion no encontrada: " + id).build();
        }

        Hospitalizacion h = opt.get();
        Map<String, Object> body = toMap(op.getBody());

        if (body.containsKey("pacienteId")) {
            Long pacienteId = toLong(body.get("pacienteId"));
            Paciente paciente = pacienteRepository.findById(pacienteId)
                    .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + pacienteId));
            h.setPaciente(paciente);
        }
        
        if (body.containsKey("servicio")) h.setServicio((String) body.get("servicio"));
        if (body.containsKey("cama")) h.setCama((String) body.get("cama"));
        if (body.containsKey("estado")) h.setEstado((String) body.get("estado"));
        if (body.containsKey("fechaIngreso")) {
            String fechaIngresoStr = (String) body.get("fechaIngreso");
            if (fechaIngresoStr != null) {
                h.setFechaIngreso(LocalDateTime.parse(fechaIngresoStr));
            }
        }
        if (body.containsKey("fechaEgreso")) {
            String fechaEgresoStr = (String) body.get("fechaEgreso");
            if (fechaEgresoStr != null) {
                h.setFechaEgreso(LocalDateTime.parse(fechaEgresoStr));
            }
        }
        if (body.containsKey("evolucionDiaria")) h.setEvolucionDiaria((String) body.get("evolucionDiaria"));
        if (body.containsKey("ordenesMedicas")) h.setOrdenesMedicas((String) body.get("ordenesMedicas"));
        if (body.containsKey("epicrisis")) h.setEpicrisis((String) body.get("epicrisis"));

        h = hospitalizacionRepository.save(h);

        return SyncItemResult.builder()
                .clientId(op.getClientId()).success(true).status(200)
                .serverId(h.getId()).build();
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
