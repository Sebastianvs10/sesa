/**
 * S15: Request para registrar visualización de una guía GPC (auditoría).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuiaGpcRegistroVisualizacionDto {
    private Long atencionId;
    private String codigoCie10;
    private Long guiaId;
}
