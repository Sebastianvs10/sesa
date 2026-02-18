/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmaciaDispensacionDto {
    private Long id;
    private Long medicamentoId;
    private String medicamentoNombre;
    private Long pacienteId;
    private String pacienteNombre;
    private Integer cantidad;
    private LocalDateTime fechaDispensacion;
    private String entregadoPor;
    private Instant createdAt;
}
