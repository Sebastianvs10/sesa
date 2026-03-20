/**
 * KPIs de inventario farmacia (conteos sin listar todos los ítems).
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
public class FarmaciaIndicadoresDto {
    private long totalSkusActivos;
    private long stockBajo;
    private long proximosAVencer30Dias;
}
