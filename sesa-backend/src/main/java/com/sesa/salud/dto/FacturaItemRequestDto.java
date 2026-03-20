/**
 * DTO para crear/actualizar ítem de factura.
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
public class FacturaItemRequestDto {
    private Integer itemIndex;
    private String codigoCups;
    private String descripcionCups;
    private String tipoServicio;
    private Integer cantidad;
    private BigDecimal valorUnitario;
    private BigDecimal valorTotal;
}
