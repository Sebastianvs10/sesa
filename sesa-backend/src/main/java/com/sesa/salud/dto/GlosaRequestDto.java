/**
 * S9: Request para crear/actualizar glosa.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlosaRequestDto {
    @NotNull
    private Long facturaId;
    @NotBlank
    private String motivoRechazo;
    private String estado;
    private String observaciones;
}
