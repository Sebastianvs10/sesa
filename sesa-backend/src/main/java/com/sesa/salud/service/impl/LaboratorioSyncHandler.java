/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sesa.salud.dto.SyncBatchRequest.SyncOperationItem;
import com.sesa.salud.dto.SyncBatchResponse.SyncItemResult;
import com.sesa.salud.entity.LaboratorioSolicitud;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.entity.Personal;
import com.sesa.salud.repository.LaboratorioSolicitudRepository;
import com.sesa.salud.repository.PacienteRepository;
import com.sesa.salud.repository.PersonalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("laboratorios")
@RequiredArgsConstructor
@Slf4j
public class LaboratorioSyncHandler implements SyncEntityHandler {

    private final LaboratorioSolicitudRepository laboratorioRepository;
    private final PacienteRepository pacienteRepository;
    private final PersonalRepository personalRepository;
    private final ObjectMapper objectMapper;

    private static final Pattern ID_PATTERN = Pattern.compile("/laboratorios/(\\d+)");

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
            log.error("Error en LaboratorioSyncHandler: {}", e.getMessage(), e);
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(500)
                    .error("Error procesando solicitud laboratorio: " + e.getMessage()).build();
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

        String tipoPrueba = (String) body.get("tipoPrueba");
        if (tipoPrueba == null || tipoPrueba.isBlank()) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("tipoPrueba es obligatorio").build();
        }

        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + pacienteId));

        Personal solicitante = null;
        Long solicitanteId = toLong(body.get("solicitanteId"));
        if (solicitanteId != null) {
            solicitante = personalRepository.findById(solicitanteId).orElse(null);
        }

        LaboratorioSolicitud lab = LaboratorioSolicitud.builder()
                .paciente(paciente)
                .solicitante(solicitante)
                .tipoPrueba(tipoPrueba)
                .estado((String) body.getOrDefault("estado", "PENDIENTE"))
                .fechaSolicitud(LocalDate.now())
                .build();

        lab = laboratorioRepository.save(lab);
        return SyncItemResult.builder()
                .clientId(op.getClientId()).success(true).status(201)
                .serverId(lab.getId()).build();
    }

    private SyncItemResult handleUpdate(SyncOperationItem op) throws Exception {
        Long id = extractId(op.getUrl());
        if (id == null) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("No se pudo extraer ID de la URL").build();
        }

        Optional<LaboratorioSolicitud> opt = laboratorioRepository.findById(id);
        if (opt.isEmpty()) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(404)
                    .error("Solicitud de laboratorio no encontrada: " + id).build();
        }

        LaboratorioSolicitud lab = opt.get();
        Map<String, Object> body = toMap(op.getBody());

        if (body.containsKey("tipoPrueba")) lab.setTipoPrueba((String) body.get("tipoPrueba"));
        if (body.containsKey("estado")) lab.setEstado((String) body.get("estado"));

        lab = laboratorioRepository.save(lab);
        return SyncItemResult.builder()
                .clientId(op.getClientId()).success(true).status(200)
                .serverId(lab.getId()).build();
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
