/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service;

import com.sesa.salud.dto.SyncBatchRequest;
import com.sesa.salud.dto.SyncBatchResponse;

/**
 * Servicio para procesar operaciones offline sincronizadas en lote.
 */
public interface SyncBatchService {

    /**
     * Procesa un lote de operaciones offline.
     * Cada operación se ejecuta individualmente; si una falla, las demás continúan.
     * Se garantiza idempotencia mediante clientId de deduplicación.
     */
    SyncBatchResponse processBatch(SyncBatchRequest request, String userEmail);
}
