/**
 * DTO indicadores por territorio para dashboard EBS.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EbsTerritoryIndicatorDto {

    private Long territoryId;
    private String territoryName;
    private String territoryCode;
    private long totalHogares;
    private long hogaresVisitados;
    private double porcentajeCobertura;
    private long hogaresAltoRiesgo;
    private long visitasEnPeriodo;
}
