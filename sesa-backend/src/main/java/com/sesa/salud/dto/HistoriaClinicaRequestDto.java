/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoriaClinicaRequestDto {
    private String estado;
    private String grupoSanguineo;
    private String alergiasGenerales;
    private String antecedentesPersonales;
    private String antecedentesQuirurgicos;
    private String antecedentesFarmacologicos;
    private String antecedentesTraumaticos;
    private String antecedentesGinecoobstetricos;
    private String antecedentesFamiliares;
    private Boolean habitosTabaco;
    private Boolean habitosAlcohol;
    private Boolean habitosSustancias;
    private String habitosDetalles;
}
