/**
 * Checklist pre-radicación: validaciones antes de radicar una factura ante EPS.
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
public class ChecklistRadicacionDto {
    private boolean listo;
    private List<String> errores;
    private List<String> advertencias;
    private String resumen;
}
