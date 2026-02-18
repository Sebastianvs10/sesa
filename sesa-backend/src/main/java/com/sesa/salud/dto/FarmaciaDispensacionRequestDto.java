/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FarmaciaDispensacionRequestDto {
    @NotNull
    private Long medicamentoId;
    @NotNull
    private Long pacienteId;
    @NotNull
    private Integer cantidad;
    private String entregadoPor;
}
