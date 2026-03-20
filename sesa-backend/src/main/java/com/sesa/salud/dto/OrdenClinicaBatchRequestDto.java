/**
 * Petición para crear varias órdenes clínicas en una sola emisión (misma consulta).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenClinicaBatchRequestDto {
    @NotNull(message = "Paciente es obligatorio")
    private Long pacienteId;
    @NotNull(message = "Consulta es obligatoria")
    private Long consultaId;
    @NotEmpty(message = "Debe incluir al menos un ítem")
    @Valid
    private List<OrdenClinicaItemDto> items;
}
