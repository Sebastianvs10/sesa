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
public class HospitalizacionDto {
    private Long id;
    private Long pacienteId;
    private String pacienteNombre;
    private String servicio;
    private String cama;
    private String estado;
    private LocalDateTime fechaIngreso;
    private LocalDateTime fechaEgreso;
    private String evolucionDiaria;
    private String ordenesMedicas;
    private String epicrisis;
    private Instant createdAt;
}
