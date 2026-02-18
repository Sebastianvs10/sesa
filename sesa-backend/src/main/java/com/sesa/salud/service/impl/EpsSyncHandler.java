/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sesa.salud.dto.SyncBatchRequest.SyncOperationItem;
import com.sesa.salud.dto.SyncBatchResponse.SyncItemResult;
import com.sesa.salud.entity.Eps;
import com.sesa.salud.repository.EpsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("eps")
@RequiredArgsConstructor
@Slf4j
public class EpsSyncHandler implements SyncEntityHandler {

    private final EpsRepository epsRepository;
    private final ObjectMapper objectMapper;

    private static final Pattern ID_PATTERN = Pattern.compile("/eps/(\\d+)");

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
            log.error("Error en EpsSyncHandler: {}", e.getMessage(), e);
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(500)
                    .error("Error procesando EPS: " + e.getMessage()).build();
        }
    }

    private SyncItemResult handleCreate(SyncOperationItem op) throws Exception {
        Map<String, Object> body = toMap(op.getBody());

        String codigo = (String) body.get("codigo");
        String nombre = (String) body.get("nombre");
        if (codigo == null || nombre == null) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("codigo y nombre son obligatorios").build();
        }

        Eps eps = Eps.builder()
                .codigo(codigo)
                .nombre(nombre)
                .activo(true)
                .build();

        eps = epsRepository.save(eps);
        return SyncItemResult.builder()
                .clientId(op.getClientId()).success(true).status(201)
                .serverId(eps.getId()).build();
    }

    private SyncItemResult handleUpdate(SyncOperationItem op) throws Exception {
        Long id = extractId(op.getUrl());
        if (id == null) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("No se pudo extraer ID de la URL").build();
        }

        Optional<Eps> opt = epsRepository.findById(id);
        if (opt.isEmpty()) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(404)
                    .error("EPS no encontrada: " + id).build();
        }

        Eps eps = opt.get();
        Map<String, Object> body = toMap(op.getBody());

        if (body.containsKey("codigo")) eps.setCodigo((String) body.get("codigo"));
        if (body.containsKey("nombre")) eps.setNombre((String) body.get("nombre"));
        if (body.containsKey("activo")) {
            Object act = body.get("activo");
            if (act instanceof Boolean) eps.setActivo((Boolean) act);
        }

        eps = epsRepository.save(eps);
        return SyncItemResult.builder()
                .clientId(op.getClientId()).success(true).status(200)
                .serverId(eps.getId()).build();
    }

    private SyncItemResult handleDelete(SyncOperationItem op) {
        Long id = extractId(op.getUrl());
        if (id == null) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("No se pudo extraer ID de la URL").build();
        }

        Optional<Eps> opt = epsRepository.findById(id);
        if (opt.isEmpty()) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(true).status(200).build();
        }

        Eps eps = opt.get();
        eps.setActivo(false);
        epsRepository.save(eps);
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
