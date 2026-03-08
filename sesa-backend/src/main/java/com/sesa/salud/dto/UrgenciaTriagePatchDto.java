/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body para PATCH re-triage (sugerencia 4).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UrgenciaTriagePatchDto {

    private String nivelTriage;
    private Long profesionalTriageId;
}
