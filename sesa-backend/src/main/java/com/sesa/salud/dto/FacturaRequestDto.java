/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacturaRequestDto {
    @NotNull(message = "Paciente es obligatorio")
    private Long pacienteId;
    private Long ordenId;
    @NotNull(message = "Valor total es obligatorio")
    private BigDecimal valorTotal;
    private String estado;
    private String descripcion;
}
