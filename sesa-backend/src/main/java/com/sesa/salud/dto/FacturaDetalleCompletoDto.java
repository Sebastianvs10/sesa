/**
 * Detalle completo de factura con trazabilidad a orden y consulta (HC).
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
public class FacturaDetalleCompletoDto {
    private FacturaDto factura;
    private Long ordenId;
    private Long consultaId;
    private String fechaConsultaIso;
    private String codigoCie10Consulta;
    private String tipoOrden;
    private BigDecimal valorEstimadoOrden;
}
