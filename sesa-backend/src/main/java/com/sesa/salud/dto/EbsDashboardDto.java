/**
 * DTO dashboard EBS para Supervisor APS (indicadores gerenciales).
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EbsDashboardDto {

    /** Total de microterritorios activos. */
    private long totalTerritorios;
    /** Total de hogares registrados. */
    private long totalHogares;
    /** Hogares con al menos una visita. */
    private long hogaresVisitados;
    /** Porcentaje cobertura (hogares visitados / total hogares). */
    private double porcentajeCobertura;
    /** Hogares con riesgo alto o muy alto. */
    private long hogaresAltoRiesgo;
    /** Visitas domiciliarias en el período (ej. último mes). */
    private long visitasEnPeriodo;
    /** Crónicos con control reciente (placeholder para indicador). */
    private long cronicosControlados;
    /** Alertas epidemiológicas o seguimiento pendiente (placeholder). */
    private long alertasSeguimiento;
    /** Desglose por territorio (opcional). */
    private List<EbsTerritoryIndicatorDto> porTerritorio;
}
