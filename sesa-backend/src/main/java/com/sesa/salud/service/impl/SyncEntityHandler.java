/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.sesa.salud.dto.SyncBatchRequest.SyncOperationItem;
import com.sesa.salud.dto.SyncBatchResponse.SyncItemResult;

/**
 * Interfaz para handlers que procesan operaciones de sync por entidad.
 * Cada entidad (pacientes, consultas, citas, etc.) implementa su propio handler.
 */
public interface SyncEntityHandler {

    /**
     * Procesa una operación de sync offline para esta entidad.
     *
     * @param op        la operación (método, URL, body, etc.)
     * @param userEmail email del usuario que ejecuta la operación
     * @return resultado del procesamiento
     */
    SyncItemResult handle(SyncOperationItem op, String userEmail);
}
