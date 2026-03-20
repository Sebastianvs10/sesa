/**
 * S10: Request para enviar cuestionario pre-consulta (ePRO).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuestionarioPreconsultaRequestDto {
    @NotNull
    private Long citaId;
    private String motivoPalabras;
    private Integer dolorEva;
    private Integer ansiedadEva;
    private String medicamentosActuales;
    private String alergiasReferidas;
}
