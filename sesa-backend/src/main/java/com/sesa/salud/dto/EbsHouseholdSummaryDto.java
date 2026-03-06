/**
 * DTO resumen de hogar EBS para listados (con última visita).
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EbsHouseholdSummaryDto {
    private Long id;
    private Long territoryId;
    private String addressText;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Instant lastVisitDate;
    private String riskLevel;
    private String state;
}
