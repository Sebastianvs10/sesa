/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrearRecetaConFormulasRequestDto {
    private Long pacienteId;
    private Long consultaId;
    private List<FormulaMedicaDto> medicamentos;
    private String medicoNombre;
    private String pacienteNombre;
    private String diagnostico;
    private String observaciones;
}
