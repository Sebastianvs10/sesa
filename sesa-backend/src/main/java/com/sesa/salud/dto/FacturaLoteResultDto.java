/**
 * Resultado de facturación por lote: facturas creadas y errores.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacturaLoteResultDto {
    private List<FacturaDto> facturasCreadas;
    private List<String> errores;
    private int totalProcesadas;
    private int totalCreadas;
}
