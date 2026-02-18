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
public class UrgenciaRegistroDto {

    private Long id;
    private Long pacienteId;
    private String pacienteNombre;
    private String nivelTriage;
    private String estado;
    private LocalDateTime fechaHoraIngreso;
    private String observaciones;
    private Instant createdAt;
}
