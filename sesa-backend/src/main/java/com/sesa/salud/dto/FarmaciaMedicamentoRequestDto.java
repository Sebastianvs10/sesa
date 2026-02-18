/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FarmaciaMedicamentoRequestDto {
    private String nombre;
    private String lote;
    private LocalDate fechaVencimiento;
    private Integer cantidad;
    private BigDecimal precio;
    private Integer stockMinimo;
    private Boolean activo;
}
