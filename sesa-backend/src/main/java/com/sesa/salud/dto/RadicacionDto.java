/**
 * DTO de radicación para API.
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
public class RadicacionDto {
    private Long id;
    private Long facturaId;
    private String numeroFactura;
    private Instant fechaRadicacion;
    private String numeroRadicado;
    private String epsCodigo;
    private String epsNombre;
    private String estado;
    private String cuv;
    private String observaciones;
    private Instant createdAt;
}
