/**
 * Cuerpo para guardar notas del asistente en videoconsulta.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotasVideoconsultaDto {
    private String texto;
}
