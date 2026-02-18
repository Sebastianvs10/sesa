/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmaciaMedicamentoDto {
    private Long id;
    private String nombre;
    private String lote;
    private LocalDate fechaVencimiento;
    private Integer cantidad;
    private BigDecimal precio;
    private Integer stockMinimo;
    private Boolean activo;
    private Instant createdAt;
}
