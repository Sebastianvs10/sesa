/**
 * S9: Adjunto de glosa — DTO.
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
public class GlosaAdjuntoDto {
    private Long id;
    private Long glosaId;
    private String nombreArchivo;
    private String tipo;
    private String urlOBlob;
    private Instant createdAt;
}
