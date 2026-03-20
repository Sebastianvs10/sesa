/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvolucionRequestDto {

    @NotNull(message = "La atención es obligatoria")
    private Long atencionId;

    @NotBlank(message = "La nota de evolución es obligatoria")
    private String notaEvolucion;

    private Instant fecha;
    private Long profesionalId;
}
