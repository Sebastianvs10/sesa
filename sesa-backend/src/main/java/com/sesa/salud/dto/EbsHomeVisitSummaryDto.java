/**
 * DTO resumen de visita domiciliaria EBS para listados.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EbsHomeVisitSummaryDto {

    private Long id;
    private Long householdId;
    private String householdAddress;
    private Long territoryId;
    private String territoryName;
    private Long professionalId;
    private String professionalName;
    private Instant visitDate;
    private String visitType;
    private String motivo;
    private String notes;
    private String status;
    private Boolean riskCardiovascular;
    private Boolean riskMaterno;
    private Boolean riskCronico;
}
