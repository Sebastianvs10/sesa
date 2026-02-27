/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitaDto {

    private Long id;
    private Long pacienteId;
    private String pacienteNombre;
    private Long profesionalId;
    private String profesionalNombre;
    private String servicio;
    private LocalDateTime fechaHora;
    private String estado;
    private String notas;
    private Instant createdAt;
    // Campos normativos Res. 2953/2014
    private String tipoCita;
    private String numeroAutorizacionEps;
    private Integer duracionEstimadaMin;
    private Long diasEspera;
    private Boolean alertaOportunidad;
}
