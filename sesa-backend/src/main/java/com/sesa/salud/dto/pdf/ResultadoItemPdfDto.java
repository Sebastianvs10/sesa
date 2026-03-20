/**
 * Un ítem de resultado (etiqueta + valor) para PDF de órdenes.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto.pdf;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResultadoItemPdfDto {
    private String etiqueta;
    private String valor;
}
