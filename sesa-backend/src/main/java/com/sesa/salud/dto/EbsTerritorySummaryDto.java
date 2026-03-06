/**
 * DTO resumen de microterritorio EBS para listados.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EbsTerritorySummaryDto {
    private Long id;
    private String code;
    private String name;
    private String type;
    private long householdsCount;
    private long visitedHouseholdsCount;
    private long highRiskHouseholdsCount;
    private String igacDepartamentoCodigo;
    private String igacMunicipioCodigo;
    private String igacVeredaCodigo;
    /** Nombres para mostrar la ruta (ej. Antioquia > Medellín > Santa Elena). */
    private String igacDepartamentoNombre;
    private String igacMunicipioNombre;
    private String igacVeredaNombre;
}
