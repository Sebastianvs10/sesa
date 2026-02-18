/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacturaDto {
    private Long id;
    private Long pacienteId;
    private String pacienteNombre;
    private Long ordenId;
    private BigDecimal valorTotal;
    private String estado;
    private String descripcion;
    private Instant fechaFactura;
    private Instant createdAt;
}
