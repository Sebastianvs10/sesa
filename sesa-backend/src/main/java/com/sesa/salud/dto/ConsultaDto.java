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
public class ConsultaDto {

    private Long id;
    private Long pacienteId;
    private String pacienteNombre;
    private Long profesionalId;
    private String profesionalNombre;
    private Long citaId;
    private String motivoConsulta;
    private String enfermedadActual;
    private String antecedentesPersonales;
    private String antecedentesFamiliares;
    private String alergias;
    private Instant fechaConsulta;
    private Instant createdAt;
}
