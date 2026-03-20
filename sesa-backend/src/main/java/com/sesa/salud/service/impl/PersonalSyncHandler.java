/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sesa.salud.dto.SyncBatchRequest.SyncOperationItem;
import com.sesa.salud.dto.SyncBatchResponse.SyncItemResult;
import com.sesa.salud.entity.Personal;
import com.sesa.salud.repository.PersonalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("personal")
@RequiredArgsConstructor
@Slf4j
public class PersonalSyncHandler implements SyncEntityHandler {

    private final PersonalRepository personalRepository;
    private final ObjectMapper objectMapper;

    private static final Pattern ID_PATTERN = Pattern.compile("/personal/(\\d+)");

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
            log.error("Error en PersonalSyncHandler: {}", e.getMessage(), e);
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(500)
                    .error("Error procesando personal: " + e.getMessage()).build();
        }
    }

    private SyncItemResult handleCreate(SyncOperationItem op) throws Exception {
        Map<String, Object> body = toMap(op.getBody());

        String nombres = (String) body.get("nombres");
        if (nombres == null) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("nombres es obligatorio").build();
        }

        String rolSync = (String) body.get("rol");

        Personal p = Personal.builder()
                .nombres(nombres)
                .apellidos((String) body.get("apellidos"))
                .identificacion((String) body.get("identificacion"))
                .primerNombre((String) body.get("primerNombre"))
                .segundoNombre((String) body.get("segundoNombre"))
                .primerApellido((String) body.get("primerApellido"))
                .segundoApellido((String) body.get("segundoApellido"))
                .celular((String) body.get("celular"))
                .email((String) body.get("email"))
                .rol(rolSync)
                .activo(true)
                .build();

        p = personalRepository.save(p);
        return SyncItemResult.builder()
                .clientId(op.getClientId()).success(true).status(201)
                .serverId(p.getId()).build();
    }

    private SyncItemResult handleUpdate(SyncOperationItem op) throws Exception {
        Long id = extractId(op.getUrl());
        if (id == null) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(400)
                    .error("No se pudo extraer ID de la URL").build();
        }

        Optional<Personal> opt = personalRepository.findById(id);
        if (opt.isEmpty()) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(false).status(404)
                    .error("Personal no encontrado: " + id).build();
        }

        Personal p = opt.get();
        Map<String, Object> body = toMap(op.getBody());

        if (body.containsKey("nombres")) p.setNombres((String) body.get("nombres"));
        if (body.containsKey("apellidos")) p.setApellidos((String) body.get("apellidos"));
        if (body.containsKey("identificacion")) p.setIdentificacion((String) body.get("identificacion"));
        if (body.containsKey("primerNombre")) p.setPrimerNombre((String) body.get("primerNombre"));
        if (body.containsKey("segundoNombre")) p.setSegundoNombre((String) body.get("segundoNombre"));
        if (body.containsKey("primerApellido")) p.setPrimerApellido((String) body.get("primerApellido"));
        if (body.containsKey("segundoApellido")) p.setSegundoApellido((String) body.get("segundoApellido"));
        if (body.containsKey("celular")) p.setCelular((String) body.get("celular"));
        if (body.containsKey("email")) p.setEmail((String) body.get("email"));
        if (body.containsKey("rol")) p.setRol((String) body.get("rol"));

        p = personalRepository.save(p);
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

        Optional<Personal> opt = personalRepository.findById(id);
        if (opt.isEmpty()) {
            return SyncItemResult.builder()
                    .clientId(op.getClientId()).success(true).status(200).build();
        }

        Personal p = opt.get();
        p.setActivo(false);
        personalRepository.save(p);
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
