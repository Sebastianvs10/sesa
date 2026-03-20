/**
 * S5: DTO de respuesta para reconciliación de medicamentos y alergias.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliacionAtencionDto {

    private Long id;
    private Long atencionId;
    private Long profesionalId;
    private String nombreProfesional;
    private List<String> medicamentosReferidos;
    private List<String> medicamentosHc;
    private List<String> alergiasReferidas;
    private List<String> alergiasHc;
    private Instant reconciliadoAt;
    private String observaciones;
}
