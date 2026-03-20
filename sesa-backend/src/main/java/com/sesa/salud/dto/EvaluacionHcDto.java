/**
 * S16: DTO de evaluación de calidad de una atención/consulta para auditoría HC.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EvaluacionHcDto {
    private Long consultaId;
    private boolean camposCompletos;
    private List<String> camposFaltantes;
    private int puntuacion;
    private static final int UMBRAL_BAJO = 70;
    public boolean isBajoUmbral() { return puntuacion < UMBRAL_BAJO; }
}
