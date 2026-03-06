/**
 * DTO para órdenes clínicas en PDF de historia clínica.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto.pdf;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OrdenPdfDto {
    private String tipo;
    private String detalle;
    private String estado;
    private String resultado;
    private String fechaResultado;
    /** Resultado parseado por ítem (etiqueta en negrita + valor) para presentación en PDF. */
    private List<ResultadoItemPdfDto> resultadoItems;
}
