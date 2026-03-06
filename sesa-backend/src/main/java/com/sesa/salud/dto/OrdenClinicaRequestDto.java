/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenClinicaRequestDto {
    @NotNull(message = "Paciente es obligatorio")
    private Long pacienteId;
    @NotNull(message = "Consulta es obligatoria")
    private Long consultaId;
    @NotBlank(message = "Tipo de orden es obligatorio")
    private String tipo;
    private String detalle;
    private Integer cantidadPrescrita;
    private String unidadMedida;
    private String frecuencia;
    private Integer duracionDias;
    private String estado;
    private BigDecimal valorEstimado;
}
