/**
 * Resumen KPI del módulo de facturación
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
public class ResumenFacturacionDto {
    private BigDecimal totalFacturadoMes;
    private long cantidadMes;
    private BigDecimal montoPendiente;
    private long cantidadPendiente;
    private BigDecimal montoPagado;
    private long cantidadPagada;
    private long cantidadAnulada;
    private long cantidadRechazada;
}
