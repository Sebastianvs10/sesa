/**
 * DTO datos para reportes EBS (cobertura, captación, crónicos).
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EbsReportDataDto {

    private String reportType; // COBERTURA, CAPTACION, CRONICOS, PDM
    private String periodFrom;
    private String periodTo;
    private BigDecimal totalHouseholds;
    private BigDecimal visitedHouseholds;
    private BigDecimal coveragePercent;
    private List<Row> rows;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Row {
        private String territoryName;
        private String veredaName;
        private Long households;
        private Long visited;
        private BigDecimal percent;
        private Long highRisk;
    }
}
