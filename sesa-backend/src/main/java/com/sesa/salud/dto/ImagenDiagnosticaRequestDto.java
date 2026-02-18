/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImagenDiagnosticaRequestDto {
    @NotNull
    private Long atencionId;
    private String tipo;
    private String resultado;
    private String urlArchivo;
}
