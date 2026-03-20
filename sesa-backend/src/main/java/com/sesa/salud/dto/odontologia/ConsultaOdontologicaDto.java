/**
 * DTO de consulta odontológica.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto.odontologia;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ConsultaOdontologicaDto {
    private Long id;
    private Long pacienteId;
    private String pacienteNombre;
    private String pacienteDocumento;
    private Integer pacienteEdad;
    private String pacienteEps;
    private Long profesionalId;
    private String profesionalNombre;
    private Long citaId;
    // Campos normativos
    private String tipoConsulta;
    private String codigoCie10;
    private String descripcionCie10;
    private Boolean consentimientoFirmado;
    private LocalDate fechaConsentimiento;
    private String consentimientoObservaciones;
    // SOAP Subjetivo
    private String motivoConsulta;
    private String enfermedadActual;
    private String antecedentesOdontologicos;
    private String antecedentesSistemicos;
    private String medicamentosActuales;
    private String alergias;
    private String habitosOrales;
    private String higieneOral;
    // SOAP Objetivo
    private String examenExtraOral;
    private String examenIntraOral;
    // Índice CPOD (dentición permanente)
    private Integer cpodCariados;
    private Integer cpodPerdidos;
    private Integer cpodObturados;
    // Índice ceod (dentición temporal)
    private Integer ceodCariados;
    private Integer ceodExtraidos;
    private Integer ceodObturados;
    // Índice IHO-S
    private Double ihosPlaca;
    private Double ihosCalculo;
    private String condicionPeriodontal;
    private String riesgoCaries;
    // SOAP A/P
    private String diagnostico;
    private String planTratamiento;
    // Firma
    private String firmaProfesionalUrl;
    private String firmaCanvasData;
    private String estado;
    private Instant createdAt;
    private Instant updatedAt;
}
