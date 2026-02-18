/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sesa.salud.dto.SyncBatchRequest.SyncOperationItem;
import com.sesa.salud.dto.SyncBatchResponse.SyncItemResult;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.entity.UrgenciaRegistro;
import com.sesa.salud.repository.PacienteRepository;
import com.sesa.salud.repository.UrgenciaRegistroRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("urgencias")
@RequiredArgsConstructor
@Slf4j
public class UrgenciaSyncHandler implements SyncEntityHandler {

    private final UrgenciaRegistroRepository urgenciaRepository;
    private final PacienteRepository pacienteRepository;
    private final ObjectMapper objectMapper;

    private static final Pattern ID_PATTERN = Pattern.compile("/urgencias/(\\d+)");

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
            log.error("Error en UrgenciaSyncHandler: {}", e.getMessage(), e);
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(500)
                    .error("Error procesando urgencia: " + e.getMessage()).build();
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

        UrgenciaRegistro u = UrgenciaRegistro.builder()
                .paciente(paciente)
                .nivelTriage((String) body.get("nivelTriage"))
                .estado((String) body.getOrDefault("estado", "EN_ESPERA"))
                .fechaHoraIngreso(LocalDateTime.now())
                .observaciones((String) body.get("observaciones"))
                .build();

        u = urgenciaRepository.save(u);
        return SyncItemResult.builder()
                .clientId(op.getClientId()).success(true).status(201)
                .serverId(u.getId()).build();
    }

    private SyncItemResult handleUpdate(SyncOperationItem op) throws Exception {
        Long id = extractId(op.getUrl());
        if (id == null) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("No se pudo extraer ID de la URL").build();
        }

        Optional<UrgenciaRegistro> opt = urgenciaRepository.findById(id);
        if (opt.isEmpty()) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(404)
                    .error("Urgencia no encontrada: " + id).build();
        }

        UrgenciaRegistro u = opt.get();
        Map<String, Object> body = toMap(op.getBody());

        if (body.containsKey("nivelTriage")) u.setNivelTriage((String) body.get("nivelTriage"));
        if (body.containsKey("estado")) u.setEstado((String) body.get("estado"));
        if (body.containsKey("observaciones")) u.setObservaciones((String) body.get("observaciones"));

        u = urgenciaRepository.save(u);
        return SyncItemResult.builder()
                .clientId(op.getClientId()).success(true).status(200)
                .serverId(u.getId()).build();
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
        if (value instanceof String) { try { return Long.parseLong((String) value); } catch (NumberFormatException e) { return null; } }
        return null;
    }
}
