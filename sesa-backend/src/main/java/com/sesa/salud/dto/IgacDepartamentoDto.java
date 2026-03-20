/**
 * DTO catálogo IGAC – Departamento.
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
public class IgacDepartamentoDto {
    private Long id;
    private String codigoDane;
    private String nombre;
}
