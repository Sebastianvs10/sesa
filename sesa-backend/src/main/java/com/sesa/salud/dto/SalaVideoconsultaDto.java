/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaVideoconsultaDto {
    private String salaId;
    private String role; // "creador" | "participante"
    private String token;
    private Long citaId;
    private Long profesionalId;
    private Long pacienteId;
}
