/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImagenDiagnosticaDto {
    private Long id;
    private Long atencionId;
    private String tipo;
    private String resultado;
    private String urlArchivo;
    private Instant createdAt;
}
