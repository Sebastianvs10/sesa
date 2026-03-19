/**
 * S14: DTO para listar órdenes con resultado en el portal del paciente (con interpretación en lenguaje sencillo).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrdenConResultadoPortalDto {
    private Long ordenId;
    private String tipo;
    private String detalle;
    private String resultado;
    private Instant fechaResultado;
    /** S14: Interpretación breve en lenguaje sencillo para el paciente. */
    private String interpretacionLenguajeSencillo;
    /** Ruta relativa para descargar PDF (ej. /api/portal/paciente/orden/1/pdf). */
    private String enlaceDescargaPdf;
}
