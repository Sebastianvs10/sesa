/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto.pdf;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LaboratorioPdfDto {
    private String tipoExamen;
    private String resultado;
    private String fechaResultado;
}
