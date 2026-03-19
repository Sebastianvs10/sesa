/**
 * S9: Glosa — DTO para listado y detalle.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlosaDto {
    private Long id;
    private Long facturaId;
    private String numeroFactura;
    private String motivoRechazo;
    private String estado;
    private Instant fechaRegistro;
    private Instant fechaRespuesta;
    private String observaciones;
    private Long creadoPorId;
    private String creadoPorNombre;
    private Instant createdAt;
    private Instant updatedAt;
    @Builder.Default
    private List<GlosaAdjuntoDto> adjuntos = List.of();
}
