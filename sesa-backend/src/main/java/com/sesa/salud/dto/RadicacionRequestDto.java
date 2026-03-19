/**
 * DTO para crear/actualizar radicación.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RadicacionRequestDto {
    private Long facturaId;
    private Instant fechaRadicacion;
    private String numeroRadicado;
    private String epsCodigo;
    private String epsNombre;
    private String estado;
    private String cuv;
    private String observaciones;
}
