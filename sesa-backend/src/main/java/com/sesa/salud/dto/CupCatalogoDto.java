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
public class CupCatalogoDto {
    private Long id;
    private String codigo;
    private String descripcion;
    private String capitulo;
    private String tipoServicio;
    private BigDecimal precioSugerido;
}
