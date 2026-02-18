/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DolorRequestDto {

    @NotNull
    private Long pacienteId;

    private Long historiaClinicaId;

    @NotBlank
    private String zonaCorporal;

    @NotBlank
    private String zonaLabel;

    private String tipoDolor;

    @NotNull
    @Min(1)
    @Max(10)
    private Integer intensidad;

    private String severidad;
    private String estado;
    private String fechaInicio;
    private String fechaResolucion;
    private String descripcion;
    private String factoresAgravantes;
    private String factoresAliviantes;
    private String tratamiento;
    private String notas;
    private String vista;
}
