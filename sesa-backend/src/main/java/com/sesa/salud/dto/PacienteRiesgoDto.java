/**
 * DTO para score de riesgo del paciente (S1 - cabecera HC).
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
public class PacienteRiesgoDto {
    /** Nivel: BAJO, MEDIO, ALTO */
    private String nivelRiesgo;
    /** Puntos del score (0-100). */
    private int puntos;
    /** Factores que aportan al score (ej. "Alergias registradas", "Múltiples atenciones recientes"). */
    private List<String> factores;
    /** Recomendaciones según factores (ej. "Verificar reconciliación de medicamentos"). */
    private List<String> recomendaciones;
}
