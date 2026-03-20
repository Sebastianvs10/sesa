/**
 * Request para dispensar una orden médica (múltiples líneas: medicamento, lote, cantidad).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DispensarOrdenRequestDto {
    @NotNull(message = "El ID de la orden es obligatorio")
    private Long ordenId;
    @Valid
    private List<LineaDispensacionDto> lineas;
}
