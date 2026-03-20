/**
 * S9: Reporte recuperación de cartera — glosas por período.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

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
public class RecuperacionCarteraDto {
    private long totalGlosas;
    private long pendientes;
    private long enviadas;
    private long aceptadas;
    private long rechazadas;
    /** Valor total recuperado (glosas en estado ACEPTADO asociadas a facturas). */
    private BigDecimal totalRecuperado;
    private List<GlosaResumenDto> porEstado;
}
