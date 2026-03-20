/**
 * Resumen de alertas para el facturador (dashboard predictivo).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertasFacturacionDto {
    private List<AlertaFacturacionDto> alertas;
    private int totalPorVencer;
    private int totalVencidas;
    private int totalGlosasPendientes;
}
