/**
 * DTO para registrar resultado de una orden clínica (ej. resultado de laboratorio).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultadoOrdenDto {
    @NotBlank(message = "El resultado es obligatorio")
    private String resultado;
    /** Si true, la orden se marca como resultado crítico (alertas y trazabilidad de lectura). */
    private Boolean resultadoCritico;
}
