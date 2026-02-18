/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request para sincronización en lote de operaciones offline.
 * Cada item representa una operación HTTP que el frontend encoló mientras estaba sin conexión.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncBatchRequest {

    @NotEmpty(message = "El lote debe contener al menos una operación")
    @Valid
    private List<SyncOperationItem> operations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncOperationItem {
        /** UUID generado en el cliente para deduplicación */
        @NotNull(message = "clientId es obligatorio")
        private String clientId;

        /** Método HTTP: POST, PUT, DELETE, PATCH */
        @NotNull(message = "method es obligatorio")
        private String method;

        /** URL relativa, ej: /pacientes, /consultas */
        @NotNull(message = "url es obligatorio")
        private String url;

        /** Body de la operación (JSON) */
        private Object body;

        /** Timestamp ISO de cuándo se creó en el cliente */
        private String createdAt;

        /** Entidad afectada: pacientes, consultas, etc. */
        private String entity;
    }
}
