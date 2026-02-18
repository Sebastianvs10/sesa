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
public class HospitalizacionRequestDto {
    @NotNull
    private Long pacienteId;
    private String servicio;
    private String cama;
    private String estado;
    private String evolucionDiaria;
    private String ordenesMedicas;
    private String epicrisis;
}
