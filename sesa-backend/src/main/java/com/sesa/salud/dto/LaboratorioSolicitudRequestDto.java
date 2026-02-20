/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LaboratorioSolicitudRequestDto {

    @NotNull(message = "Paciente es obligatorio")
    private Long pacienteId;
    private Long solicitanteId;
    @NotBlank(message = "Tipo de prueba es obligatorio")
    private String tipoPrueba;
    private String estado = "PENDIENTE";
    private String resultado;
    private String observaciones;
    private Long bacteriologoId;
}
