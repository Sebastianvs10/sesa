/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LaboratorioSolicitudDto {

    private Long id;
    private Long pacienteId;
    private String pacienteNombre;
    private Long solicitanteId;
    private String solicitanteNombre;
    private String tipoPrueba;
    private String estado;
    private LocalDate fechaSolicitud;
    private Instant createdAt;
}
