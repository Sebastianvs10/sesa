/**
 * S8: Sugerencia CIE-10 — respuesta del endpoint de sugerencias.
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
public class Cie10SugerenciaDto {
    private String codigo;
    private String descripcion;
    /** Score de relevancia (mayor = más relevante) o orden. */
    private int relevancia;
}
