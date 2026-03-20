/**
 * Un ítem de medicamento dentro de una orden pendiente de farmacia (para órdenes compuestas).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenFarmaciaPendienteItemDto {
    private Long id;
    private String detalle;
    private Integer cantidadPrescrita;
    private String unidadMedida;
    private String frecuencia;
    private Integer duracionDias;
}
