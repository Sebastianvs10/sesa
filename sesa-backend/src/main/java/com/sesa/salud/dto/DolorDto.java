/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DolorDto {

    private Long id;
    private Long pacienteId;
    private String pacienteNombre;
    private Long historiaClinicaId;
    private String zonaCorporal;
    private String zonaLabel;
    private String tipoDolor;
    private Integer intensidad;
    private String severidad;
    private String estado;
    private Instant fechaInicio;
    private Instant fechaResolucion;
    private String descripcion;
    private String factoresAgravantes;
    private String factoresAliviantes;
    private String tratamiento;
    private String notas;
    private String vista;
    private Instant createdAt;
}
