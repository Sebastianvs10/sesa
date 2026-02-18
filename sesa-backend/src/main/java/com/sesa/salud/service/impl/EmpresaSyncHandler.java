/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sesa.salud.dto.EmpresaCreateRequest;
import com.sesa.salud.dto.SyncBatchRequest.SyncOperationItem;
import com.sesa.salud.dto.SyncBatchResponse.SyncItemResult;
import com.sesa.salud.dto.EmpresaDto;
import com.sesa.salud.service.EmpresaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handler de sincronización offline para la entidad Empresas (creación/edición por superadmin).
 */
@Component("empresas")
@RequiredArgsConstructor
@Slf4j
public class EmpresaSyncHandler implements SyncEntityHandler {

    private final EmpresaService empresaService;
    private final ObjectMapper objectMapper;

    private static final Pattern ID_PATTERN = Pattern.compile("/empresas/(\\d+)");

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
            log.error("Error en EmpresaSyncHandler: {}", e.getMessage(), e);
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(500)
                    .error("Error procesando empresa: " + e.getMessage()).build();
        }
    }

    private SyncItemResult handleCreate(SyncOperationItem op) throws Exception {
        EmpresaCreateRequest request = toRequest(op.getBody());
        if (request == null) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("Cuerpo de la solicitud no válido para crear empresa").build();
        }
        if (request.getSchemaName() == null || request.getSchemaName().isBlank()) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("schemaName es obligatorio").build();
        }
        if (request.getAdminUser() == null) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("adminUser es obligatorio").build();
        }

        try {
            EmpresaDto dto = empresaService.create(request);
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(true).status(201)
                    .serverId(dto.getId()).build();
        } catch (IllegalArgumentException e) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error(e.getMessage()).build();
        }
    }

    private SyncItemResult handleUpdate(SyncOperationItem op) throws Exception {
        Long id = extractId(op.getUrl());
        if (id == null) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("No se pudo extraer ID de la URL: " + op.getUrl()).build();
        }

        EmpresaCreateRequest request = toRequest(op.getBody());
        if (request == null) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("Cuerpo de la solicitud no válido para actualizar empresa").build();
        }

        try {
            empresaService.update(id, request);
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(true).status(200)
                    .serverId(id).build();
        } catch (IllegalArgumentException e) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error(e.getMessage()).build();
        }
    }

    private EmpresaCreateRequest toRequest(Object body) {
        if (body == null) return null;
        try {
            if (body instanceof EmpresaCreateRequest) return (EmpresaCreateRequest) body;
            return objectMapper.convertValue(body, EmpresaCreateRequest.class);
        } catch (Exception e) {
            log.warn("No se pudo mapear body a EmpresaCreateRequest: {}", e.getMessage());
            return null;
        }
    }

    private Long extractId(String url) {
        if (url == null) return null;
        Matcher m = ID_PATTERN.matcher(url);
        return m.find() ? Long.parseLong(m.group(1)) : null;
    }
}
