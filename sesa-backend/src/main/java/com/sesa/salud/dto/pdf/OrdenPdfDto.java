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
    /** ID de la orden (número de orden para el PDF). */
    private Long idOrden;
    private String tipo;
    private String detalle;
    private Integer cantidadPrescrita;
    private String unidadMedida;
    private String frecuencia;
    private Integer duracionDias;
    private String estado;
    private String resultado;
    private String fechaResultado;
    /** Resultado parseado por ítem (etiqueta en negrita + valor) para presentación en PDF. */
    private List<ResultadoItemPdfDto> resultadoItems;
}
