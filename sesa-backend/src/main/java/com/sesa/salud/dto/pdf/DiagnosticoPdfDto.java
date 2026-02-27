/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto.pdf;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DiagnosticoPdfDto {
    private String codigoCie10;
    private String descripcion;
    private String tipo;
}
