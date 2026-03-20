/**
 * S10: Cuestionario pre-consulta (ePRO) — DTO respuesta.
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
public class CuestionarioPreconsultaDto {
    private Long id;
    private Long citaId;
    private Long pacienteId;
    private String motivoPalabras;
    private Integer dolorEva;
    private Integer ansiedadEva;
    private String medicamentosActuales;
    private String alergiasReferidas;
    private Instant enviadoAt;
    private Instant createdAt;
}
