/**
 * DTO para crear historia clínica + primera atención en una sola operación
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrearHistoriaCompletaRequestDto {
    /** Si se envía, se usa este profesional para la primera atención; si no, se usa el asociado al usuario logueado. */
    private Long profesionalId;

    // HC base
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

    // Primera atención
    @NotBlank(message = "Motivo de consulta es requerido")
    private String motivoConsulta;
    @NotBlank(message = "Enfermedad actual es requerida")
    private String enfermedadActual;
    private String versionEnfermedad;
    private String sintomasAsociados;
    private String factoresMejoran;
    private String factoresEmpeoran;
    private String revisionSistemas;
    private String presionArterial;
    private String frecuenciaCardiaca;
    private String frecuenciaRespiratoria;
    private String temperatura;
    private String peso;
    private String talla;
    private String imc;
    private String evaluacionGeneral;
    private String hallazgos;
    private String diagnostico;
    private String codigoCie10;
    private String planTratamiento;
    private String tratamientoFarmacologico;
    private String ordenesMedicas;
    private String examenesSolicitados;
    private String incapacidad;
    private String recomendaciones;
}
