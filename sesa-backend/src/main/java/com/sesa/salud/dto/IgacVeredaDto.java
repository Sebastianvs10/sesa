/**
 * DTO catálogo IGAC – Vereda.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IgacVeredaDto {
    private Long id;
    private String codigo;
    private String municipioCodigo;
    private String nombre;
    private String geometryJson;
}
