/**
 * DTO de evolución odontológica.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto.odontologia;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class EvolucionOdontologicaDto {
    private Long id;
    private Long pacienteId;
    private Long profesionalId;
    private String profesionalNombre;
    private Long consultaId;
    private Long planId;
    private String notaEvolucion;
    private String controlPostTratamiento;
    private Instant proximaCitaRecomendada;
    private Instant createdAt;
}
