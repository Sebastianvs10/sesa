/**
 * Indicador de calidad en salud (Res. 0256/2016 - Sistema de Información para la Calidad).
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
public class IndicadorCalidadDto {

    private String codigo;
    private String nombre;
    private String categoria;
    private String valor;
    private String meta;
    private String unidad;
    private String interpretacion;
}
