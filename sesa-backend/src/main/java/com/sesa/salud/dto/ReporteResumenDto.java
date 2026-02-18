/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteResumenDto {
    private long totalPacientes;
    private long totalCitas;
    private long totalConsultas;
    private long totalOrdenes;
    private long totalFacturas;
    private BigDecimal totalFacturado;
}
