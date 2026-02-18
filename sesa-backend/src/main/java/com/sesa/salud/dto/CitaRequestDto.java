/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitaRequestDto {

    @NotNull(message = "Paciente es obligatorio")
    private Long pacienteId;
    @NotNull(message = "Profesional es obligatorio")
    private Long profesionalId;
    @NotNull(message = "Servicio es obligatorio")
    private String servicio;
    @NotNull(message = "Fecha y hora son obligatorias")
    private LocalDateTime fechaHora;
    private String estado = "AGENDADA";
    private String notas;
}
