/**
 * Request para facturación por lote: agrupa órdenes por paciente y genera una factura por paciente.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacturaLoteRequestDto {
    @NotEmpty(message = "Debe indicar al menos una orden")
    private List<Long> ordenIds;
}
