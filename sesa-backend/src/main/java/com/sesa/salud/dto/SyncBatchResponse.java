/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Respuesta de sincronización en lote.
 * Informa el resultado individual de cada operación procesada.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncBatchResponse {

    private int processed;
    private int succeeded;
    private int failed;

    @Builder.Default
    private List<SyncItemResult> results = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SyncItemResult {
        /** clientId de la operación original */
        private String clientId;
        /** true si se procesó exitosamente */
        private boolean success;
        /** Código HTTP equivalente */
        private int status;
        /** Mensaje de error si falló */
        private String error;
        /** ID del recurso en el servidor, si aplica */
        private Long serverId;
    }
}
