/**
 * Respuesta al crear o actualizar un turno: incluye el turno y advertencias (exceso horas, descanso, etc.) sin bloquear.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TurnoResponseDto {

    private TurnoDto turno;
    private List<String> advertencias = new ArrayList<>();
}
