/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoLaboratorioDto {

    @NotBlank(message = "El resultado es obligatorio")
    private String resultado;
    private String observaciones;
    private Long bacteriologoId;
}
