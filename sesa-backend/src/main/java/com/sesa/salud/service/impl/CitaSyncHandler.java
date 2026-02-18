/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sesa.salud.dto.SyncBatchRequest.SyncOperationItem;
import com.sesa.salud.dto.SyncBatchResponse.SyncItemResult;
import com.sesa.salud.entity.Cita;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.entity.Personal;
import com.sesa.salud.repository.CitaRepository;
import com.sesa.salud.repository.PacienteRepository;
import com.sesa.salud.repository.PersonalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handler de sincronización offline para la entidad Citas.
 * Soporta POST (crear), PUT (actualizar) y DELETE.
 */
@Component("citas")
@RequiredArgsConstructor
@Slf4j
public class CitaSyncHandler implements SyncEntityHandler {

    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final PersonalRepository personalRepository;
    private final ObjectMapper objectMapper;

    private static final Pattern ID_PATTERN = Pattern.compile("/citas/(\\d+)");

    @Override
    public SyncItemResult handle(SyncOperationItem op, String userEmail) {
        String method = op.getMethod().toUpperCase();
        try {
            return switch (method) {
                case "POST" -> handleCreate(op);
                case "PUT", "PATCH" -> handleUpdate(op);
                case "DELETE" -> handleDelete(op);
                default -> SyncItemResult.builder()
                        .clientId(op.getClientId()).success(false).status(405)
                        .error("Método no soportado: " + method).build();
            };
        } catch (Exception e) {
            log.error("Error en CitaSyncHandler: {}", e.getMessage(), e);
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(500)
                    .error("Error procesando cita: " + e.getMessage()).build();
        }
    }

    private SyncItemResult handleCreate(SyncOperationItem op) throws Exception {
        Map<String, Object> body = toMap(op.getBody());
        
        Long pacienteId = toLong(body.get("pacienteId"));
        Long profesionalId = toLong(body.get("profesionalId"));
        
        if (pacienteId == null) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("pacienteId es obligatorio").build();
        }
        
        if (profesionalId == null) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("profesionalId es obligatorio").build();
        }
        
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + pacienteId));
        
        Personal profesional = personalRepository.findById(profesionalId)
                .orElseThrow(() -> new RuntimeException("Personal no encontrado: " + profesionalId));
        
        String fechaHoraStr = (String) body.get("fechaHora");
        LocalDateTime fechaHora = fechaHoraStr != null ? LocalDateTime.parse(fechaHoraStr) : LocalDateTime.now();
        
        Cita c = Cita.builder()
                .paciente(paciente)
                .profesional(profesional)
                .servicio((String) body.get("servicio"))
                .fechaHora(fechaHora)
                .estado((String) body.getOrDefault("estado", "AGENDADA"))
                .notas((String) body.get("notas"))
                .build();
        
        c = citaRepository.save(c);
        
        return SyncItemResult.builder()
                .clientId(op.getClientId()).success(true).status(201)
                .serverId(c.getId()).build();
    }

    private SyncItemResult handleUpdate(SyncOperationItem op) throws Exception {
        Long id = extractId(op.getUrl());
        if (id == null) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("No se pudo extraer ID de la URL: " + op.getUrl()).build();
        }

        Optional<Cita> opt = citaRepository.findById(id);
        if (opt.isEmpty()) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(404)
                    .error("Cita no encontrada: " + id).build();
        }

        Cita c = opt.get();
        Map<String, Object> body = toMap(op.getBody());

        if (body.containsKey("pacienteId")) {
            Long pacienteId = toLong(body.get("pacienteId"));
            Paciente paciente = pacienteRepository.findById(pacienteId)
                    .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + pacienteId));
            c.setPaciente(paciente);
        }
        
        if (body.containsKey("profesionalId")) {
            Long profesionalId = toLong(body.get("profesionalId"));
            Personal profesional = personalRepository.findById(profesionalId)
                    .orElseThrow(() -> new RuntimeException("Personal no encontrado: " + profesionalId));
            c.setProfesional(profesional);
        }
        
        if (body.containsKey("servicio")) c.setServicio((String) body.get("servicio"));
        if (body.containsKey("fechaHora")) {
            String fechaHoraStr = (String) body.get("fechaHora");
            if (fechaHoraStr != null) {
                c.setFechaHora(LocalDateTime.parse(fechaHoraStr));
            }
        }
        if (body.containsKey("estado")) c.setEstado((String) body.get("estado"));
        if (body.containsKey("notas")) c.setNotas((String) body.get("notas"));

        c = citaRepository.save(c);

        return SyncItemResult.builder()
                .clientId(op.getClientId()).success(true).status(200)
                .serverId(c.getId()).build();
    }

    private SyncItemResult handleDelete(SyncOperationItem op) {
        Long id = extractId(op.getUrl());
        if (id == null) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("No se pudo extraer ID de la URL").build();
        }

        if (!citaRepository.existsById(id)) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(true).status(200).build();
        }

        citaRepository.deleteById(id);
        return SyncItemResult.builder()
                .clientId(op.getClientId()).success(true).status(200).build();
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
