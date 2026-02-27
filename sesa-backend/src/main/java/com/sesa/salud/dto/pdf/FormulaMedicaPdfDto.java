/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto.pdf;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FormulaMedicaPdfDto {
    private String medicamento;
    private String dosis;
    private String frecuencia;
    private String duracion;
}
