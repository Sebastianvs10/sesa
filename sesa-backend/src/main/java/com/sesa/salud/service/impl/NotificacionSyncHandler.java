/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sesa.salud.dto.NotificacionCreateRequest;
import com.sesa.salud.dto.NotificacionDto;
import com.sesa.salud.dto.SyncBatchRequest.SyncOperationItem;
import com.sesa.salud.dto.SyncBatchResponse.SyncItemResult;
import com.sesa.salud.entity.Usuario;
import com.sesa.salud.repository.UsuarioRepository;
import com.sesa.salud.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component("notificaciones")
@RequiredArgsConstructor
@Slf4j
public class NotificacionSyncHandler implements SyncEntityHandler {

    private final NotificacionService notificacionService;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;

    @Override
    public SyncItemResult handle(SyncOperationItem op, String userEmail) {
        String method = op.getMethod().toUpperCase();
        try {
            if (!"POST".equals(method)) {
                return SyncItemResult.builder()
                        .clientId(op.getClientId()).success(false).status(405)
                        .error("Método no soportado: " + method).build();
            }
            return handleCreate(op, userEmail);
        } catch (Exception e) {
            log.error("Error en NotificacionSyncHandler: {}", e.getMessage(), e);
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(500)
                    .error("Error procesando notificación: " + e.getMessage()).build();
        }
    }

    @SuppressWarnings("unchecked")
    private SyncItemResult handleCreate(SyncOperationItem op, String userEmail) throws Exception {
        Map<String, Object> body = toMap(op.getBody());

        String titulo = (String) body.get("titulo");
        String contenido = (String) body.get("contenido");
        if (titulo == null || contenido == null) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("titulo y contenido son obligatorios").build();
        }

        List<Number> ids = (List<Number>) body.get("destinatarioIds");
        if (ids == null || ids.isEmpty()) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("destinatarioIds es obligatorio").build();
        }

        List<Long> destinatarioIds = ids.stream().map(Number::longValue).toList();

        Usuario remitente = usuarioRepository.findByEmail(userEmail).orElse(null);
        Long remitenteId = remitente != null ? remitente.getId() : 0L;
        String remitenteNombre = remitente != null ? remitente.getNombreCompleto() : userEmail;

        NotificacionCreateRequest request = new NotificacionCreateRequest();
        request.setTitulo(titulo);
        request.setContenido(contenido);
        request.setTipo((String) body.getOrDefault("tipo", "GENERAL"));
        request.setDestinatarioIds(destinatarioIds);

        NotificacionDto dto = notificacionService.create(request, remitenteId, remitenteNombre);

        return SyncItemResult.builder()
                .clientId(op.getClientId()).success(true).status(201)
                .serverId(dto.getId()).build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object body) throws Exception {
        if (body instanceof Map) return (Map<String, Object>) body;
        return objectMapper.convertValue(body, Map.class);
    }
}
