/**
 * S5: Request para guardar reconciliación.
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
public class ReconciliacionAtencionRequestDto {

    private List<String> medicamentosReferidos;
    private List<String> alergiasReferidas;
    private String observaciones;
}
