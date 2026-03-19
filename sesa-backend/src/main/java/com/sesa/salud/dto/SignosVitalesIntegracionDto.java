/**
 * S12: DTO para recibir signos vitales desde integradores externos.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignosVitalesIntegracionDto {
    private String presionArterial;
    private String frecuenciaCardiaca;
    private String frecuenciaRespiratoria;
    private String temperatura;
    private String peso;
    private String talla;
    private String imc;
}
