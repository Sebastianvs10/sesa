/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sesa.salud.dto.SyncBatchRequest.SyncOperationItem;
import com.sesa.salud.dto.SyncBatchResponse.SyncItemResult;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.repository.PacienteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handler de sincronización offline para la entidad Pacientes.
 * Soporta POST (crear), PUT (actualizar) y DELETE.
 */
@Component("pacientes")
@RequiredArgsConstructor
@Slf4j
public class PacienteSyncHandler implements SyncEntityHandler {

    private final PacienteRepository pacienteRepository;
    private final ObjectMapper objectMapper;

    private static final Pattern ID_PATTERN = Pattern.compile("/pacientes/(\\d+)");

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
            log.error("Error en PacienteSyncHandler: {}", e.getMessage(), e);
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(500)
                    .error("Error procesando paciente: " + e.getMessage()).build();
        }
    }

    private SyncItemResult handleCreate(SyncOperationItem op) throws Exception {
        Map<String, Object> body = toMap(op.getBody());
        String documento = (String) body.get("documento");

        // Conflicto: si ya existe paciente con ese documento, devolver el existente
        if (documento != null) {
            Optional<Paciente> existing = pacienteRepository.findByDocumento(documento);
            if (existing.isPresent()) {
                log.info("Sync: Paciente con documento {} ya existe (id={}), resolviendo conflicto como éxito",
                        documento, existing.get().getId());
                return SyncItemResult.builder()
                        .clientId(op.getClientId()).success(true).status(200)
                        .serverId(existing.get().getId()).build();
            }
        }

        Paciente p = Paciente.builder()
                .tipoDocumento((String) body.get("tipoDocumento"))
                .documento(documento)
                .nombres((String) body.get("nombres"))
                .apellidos((String) body.get("apellidos"))
                .telefono((String) body.get("telefono"))
                .email((String) body.get("email"))
                .direccion((String) body.get("direccion"))
                .sexo((String) body.get("sexo"))
                .grupoSanguineo((String) body.get("grupoSanguineo"))
                .activo(true)
                .build();
        p = pacienteRepository.save(p);

        return SyncItemResult.builder()
                .clientId(op.getClientId()).success(true).status(201)
                .serverId(p.getId()).build();
    }

    private SyncItemResult handleUpdate(SyncOperationItem op) throws Exception {
        Long id = extractId(op.getUrl());
        if (id == null) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("No se pudo extraer ID de la URL: " + op.getUrl()).build();
        }

        Optional<Paciente> opt = pacienteRepository.findById(id);
        if (opt.isEmpty()) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(404)
                    .error("Paciente no encontrado: " + id).build();
        }

        Paciente p = opt.get();
        Map<String, Object> body = toMap(op.getBody());

        if (body.containsKey("nombres")) p.setNombres((String) body.get("nombres"));
        if (body.containsKey("apellidos")) p.setApellidos((String) body.get("apellidos"));
        if (body.containsKey("telefono")) p.setTelefono((String) body.get("telefono"));
        if (body.containsKey("email")) p.setEmail((String) body.get("email"));
        if (body.containsKey("direccion")) p.setDireccion((String) body.get("direccion"));
        if (body.containsKey("sexo")) p.setSexo((String) body.get("sexo"));
        if (body.containsKey("activo")) p.setActivo(Boolean.TRUE.equals(body.get("activo")));

        p = pacienteRepository.save(p);

        return SyncItemResult.builder()
                .clientId(op.getClientId()).success(true).status(200)
                .serverId(p.getId()).build();
    }

    private SyncItemResult handleDelete(SyncOperationItem op) {
        Long id = extractId(op.getUrl());
        if (id == null) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("No se pudo extraer ID de la URL").build();
        }

        if (!pacienteRepository.existsById(id)) {
            // Ya no existe → considerarlo éxito (idempotente)
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(true).status(200).build();
        }

        pacienteRepository.deleteById(id);
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
}
