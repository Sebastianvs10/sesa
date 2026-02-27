/**
 * DTO de ítem de plan de tratamiento.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto.odontologia;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PlanTratamientoItemDto {
    private Long id;
    private Long planId;
    private Long procedimientoId;
    private String procedimientoNombre;
    private String procedimientoCodigo;
    private Integer piezaFdi;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal descuento;
    private BigDecimal valorTotal;
    private String estado;
    private String observaciones;
    private Instant createdAt;
}
