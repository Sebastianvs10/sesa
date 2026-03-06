/**
 * Una línea de dispensación: medicamento del inventario, lote (opcional) y cantidad a entregar.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineaDispensacionDto {
    @NotNull(message = "El medicamento es obligatorio")
    private Long medicamentoId;
    private String lote;
    @NotNull(message = "La cantidad es obligatoria")
    @Min(1)
    private Integer cantidad;
}
