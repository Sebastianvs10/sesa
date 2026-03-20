/**
 * Resultado de la importación desde Excel
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ExcelImportResultDto {
    private int importados;
    private int omitidos;
    private String mensaje;
    private List<String> errores;
}
