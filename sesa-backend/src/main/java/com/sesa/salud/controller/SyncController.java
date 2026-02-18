/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller;

import com.sesa.salud.dto.SyncBatchRequest;
import com.sesa.salud.dto.SyncBatchResponse;
import com.sesa.salud.service.SyncBatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para sincronización offline.
 * Recibe lotes de operaciones encoladas en el cliente cuando no había conexión
 * y las procesa de forma atómica con deduplicación.
 */
@RestController
@RequestMapping("/sync")
@RequiredArgsConstructor
@Slf4j
public class SyncController {

    private final SyncBatchService syncBatchService;

    /**
     * POST /api/sync/batch
     * Procesa un lote de operaciones offline.
     * Cada operación se valida, deduplicación por clientId, y se ejecuta individualmente.
     */
    @PostMapping("/batch")
    public ResponseEntity<SyncBatchResponse> processBatch(
            @Valid @RequestBody SyncBatchRequest request,
            Authentication authentication
    ) {
        String email = authentication != null ? authentication.getName() : "anonymous";
        log.info("Sync batch recibido: {} operaciones (usuario={})",
                request.getOperations() != null ? request.getOperations().size() : 0, email);

        SyncBatchResponse response = syncBatchService.processBatch(request, email);

        log.info("Sync batch completado: processed={}, succeeded={}, failed={} (usuario={})",
                response.getProcessed(), response.getSucceeded(), response.getFailed(), email);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/sync/status
     * Endpoint de health check para verificar disponibilidad del servicio de sync.
     */
    @GetMapping("/status")
    public ResponseEntity<java.util.Map<String, Object>> status() {
        return ResponseEntity.ok(java.util.Map.of(
                "service", "sync",
                "status", "available",
                "timestamp", java.time.Instant.now().toString()
        ));
    }
}
