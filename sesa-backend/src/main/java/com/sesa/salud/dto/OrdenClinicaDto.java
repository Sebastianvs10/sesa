/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenClinicaDto {
    private Long id;
    private Long pacienteId;
    private String pacienteNombre;
    private Long consultaId;
    private String tipo;
    private String detalle;
    private String estado;
    private String resultado;
    private Instant fechaResultado;
    /** Nombre del profesional que registró el resultado (ej. bacteriólogo). */
    private String resultadoRegistradoPorNombre;
    /** Rol del profesional que registró el resultado. */
    private String resultadoRegistradoPorRol;
    private BigDecimal valorEstimado;
    private Instant createdAt;
}
