/**
 * Request para generación automática de RIPS (periodo opcional).
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RipsGenerarRequestDto {

    /** Fecha inicio (yyyy-MM-dd). Si no se envía, se usa el primer día del mes anterior. */
    private String desde;

    /** Fecha fin (yyyy-MM-dd). Si no se envía, se usa el último día del mes anterior. */
    private String hasta;
}
