/**
 * S15: DTO de sugerencia GPC para el frontend (Análisis/Plan SOAP).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GuiaGpcSugerenciaDto {
    private Long id;
    private String titulo;
    private String criteriosControl;
    private String medicamentosPrimeraLinea;
    private String estudiosSeguimiento;
    private String fuente;
}
