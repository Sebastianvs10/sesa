/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sesa.salud.dto.SyncBatchRequest;
import com.sesa.salud.dto.SyncBatchRequest.SyncOperationItem;
import com.sesa.salud.dto.SyncBatchResponse;
import com.sesa.salud.dto.SyncBatchResponse.SyncItemResult;
import com.sesa.salud.entity.SyncDeduplication;
import com.sesa.salud.repository.SyncDeduplicationRepository;
import com.sesa.salud.service.SyncBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implementación del procesamiento en lote de operaciones offline.
 * <p>
 * Estrategia:
 * 1. Deduplicar: descartar operaciones con clientId ya procesado.
 * 2. Ordenar por createdAt para respetar secuencia cronológica.
 * 3. Ejecutar cada operación mapeando al servicio correspondiente.
 * 4. Registrar resultado y clientId para idempotencia futura.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SyncBatchServiceImpl implements SyncBatchService {

    private final SyncDeduplicationRepository deduplicationRepository;
    private final ObjectMapper objectMapper;

    /** Mapa de entidad → handler de operaciones. Se usa para delegar a los servicios */
    private final Map<String, SyncEntityHandler> entityHandlers;

    @Override
    @Transactional
    public SyncBatchResponse processBatch(SyncBatchRequest request, String userEmail) {
        List<SyncOperationItem> ops = request.getOperations();
        if (ops == null || ops.isEmpty()) {
            return SyncBatchResponse.builder()
                    .processed(0).succeeded(0).failed(0)
                    .results(Collections.emptyList())
                    .build();
        }

        // Limitar tamaño del lote para proteger el servidor
        if (ops.size() > 200) {
            ops = ops.subList(0, 200);
            log.warn("Lote de sync recortado a 200 operaciones (usuario={})", userEmail);
        }

        // 1. Chequeo de deduplicación en lote
        Set<String> clientIds = ops.stream()
                .map(SyncOperationItem::getClientId)
                .collect(Collectors.toSet());
        Set<String> alreadyProcessed = deduplicationRepository.findExistingClientIds(clientIds);

        // 2. Ordenar por timestamp de creación
        ops.sort(Comparator.comparing(o -> o.getCreatedAt() != null ? o.getCreatedAt() : ""));

        List<SyncItemResult> results = new ArrayList<>();
        int succeeded = 0;
        int failed = 0;

        for (SyncOperationItem op : ops) {
            // 3. Si ya se procesó, devolver éxito (idempotente)
            if (alreadyProcessed.contains(op.getClientId())) {
                Optional<SyncDeduplication> existing = deduplicationRepository.findByClientId(op.getClientId());
                results.add(SyncItemResult.builder()
                        .clientId(op.getClientId())
                        .success(true)
                        .status(200)
                        .serverId(existing.map(SyncDeduplication::getServerId).orElse(null))
                        .build());
                succeeded++;
                continue;
            }

            // 4. Ejecutar la operación
            SyncItemResult result = executeOperation(op, userEmail);
            results.add(result);

            // 5. Registrar en deduplicación
            try {
                deduplicationRepository.save(SyncDeduplication.builder()
                        .clientId(op.getClientId())
                        .entityType(op.getEntity())
                        .serverId(result.getServerId())
                        .success(result.isSuccess())
                        .processedAt(Instant.now())
                        .build());
            } catch (Exception e) {
                log.warn("Error guardando deduplicación para clientId={}: {}", op.getClientId(), e.getMessage());
            }

            if (result.isSuccess()) succeeded++;
            else failed++;
        }

        return SyncBatchResponse.builder()
                .processed(ops.size())
                .succeeded(succeeded)
                .failed(failed)
                .results(results)
                .build();
    }

    /**
     * Ejecuta una operación delegando al handler de la entidad correspondiente.
     */
    private SyncItemResult executeOperation(SyncOperationItem op, String userEmail) {
        try {
            String entity = resolveEntity(op);
            if (entity == null) {
                return SyncItemResult.builder()
                        .clientId(op.getClientId())
                        .success(false)
                        .status(400)
                        .error("No se pudo determinar la entidad de la URL: " + op.getUrl())
                        .build();
            }

            SyncEntityHandler handler = entityHandlers.get(entity);
            if (handler == null) {
                return SyncItemResult.builder()
                        .clientId(op.getClientId())
                        .success(false)
                        .status(400)
                        .error("Entidad no soportada para sync offline: " + entity)
                        .build();
            }

            return handler.handle(op, userEmail);

        } catch (Exception e) {
            log.error("Error ejecutando operación sync clientId={}: {}", op.getClientId(), e.getMessage(), e);
            return SyncItemResult.builder()
                    .clientId(op.getClientId())
                    .success(false)
                    .status(500)
                    .error("Error interno: " + e.getMessage())
                    .build();
        }
    }

    /** Extrae la entidad de la URL o del campo entity */
    private String resolveEntity(SyncOperationItem op) {
        if (op.getEntity() != null && !op.getEntity().isBlank()) {
            return op.getEntity().toLowerCase().replace("-", "_");
        }
        if (op.getUrl() != null) {
            Matcher m = Pattern.compile("^/?([a-z\\-_]+)", Pattern.CASE_INSENSITIVE).matcher(op.getUrl());
            if (m.find()) return m.group(1).toLowerCase().replace("-", "_");
        }
        return null;
    }
}
