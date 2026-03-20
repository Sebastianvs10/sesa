/**
 * S13: DTO para sincronización de visitas EBS (offline-first).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VisitaEbsSyncDto {
    /** UUID generado en el cliente (offline). */
    private String offlineUuid;
    /** ID en servidor si ya fue sincronizada. */
    private Long serverId;
    /** Timestamp de última modificación en el cliente (para last-write-wins). */
    private String clientUpdatedAt;
    private Long householdId;
    private Long familyGroupId;
    private String visitDate;
    private String visitType;
    private String tipoIntervencion;
    private String veredaCodigo;
    private String diagnosticoCie10;
    private String planCuidado;
    private Long brigadeId;
    private Long professionalId;
    private String motivo;
    private String notes;
    private String status;
    private Map<String, Boolean> riskFlags;
}
