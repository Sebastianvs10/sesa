/**
 * DTO solicitud de creación de visita domiciliaria EBS.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EbsHomeVisitRequestDto {
    @NotNull
    private Long householdId;
    private Long familyGroupId;
    @NotNull
    private String visitDate;
    private String visitType;
    private String tipoIntervencion;
    private String veredaCodigo;
    private String diagnosticoCie10;
    private String planCuidado;
    private Long brigadeId;
    private String motivo;
    private String notes;
    private Map<String, Boolean> riskFlags;
}
