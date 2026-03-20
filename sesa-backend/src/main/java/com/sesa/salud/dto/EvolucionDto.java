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
public class EvolucionDto {

    private Long id;
    private Long atencionId;
    private String notaEvolucion;
    private Instant fecha;
    private Long profesionalId;
    private String profesionalNombre;
    private Instant createdAt;
}
