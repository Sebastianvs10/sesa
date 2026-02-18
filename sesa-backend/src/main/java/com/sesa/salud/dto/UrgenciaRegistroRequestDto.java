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
public class UrgenciaRegistroRequestDto {

    @NotNull(message = "Paciente es obligatorio")
    private Long pacienteId;
    private String nivelTriage;
    private String estado = "EN_ESPERA";
    private String observaciones;
}
