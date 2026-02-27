/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto.pdf;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AtencionPdfDto {
    private Long id;
    private String fechaAtencion;
    private String profesionalNombre;
    private String profesionalRol;
    private String profesionalIdentificacion;
    private String firmaBase64;
    private String firmaContentType;

    /* Anamnesis */
    private String motivoConsulta;
    private String enfermedadActual;
    private String sintomasAsociados;
    private String factoresMejoran;
    private String factoresEmpeoran;
    private String revisionSistemas;

    /* Signos vitales */
    private String presionArterial;
    private String frecuenciaCardiaca;
    private String frecuenciaRespiratoria;
    private String temperatura;
    private String peso;
    private String talla;
    private String imc;

    /* Examen físico y hallazgos */
    private String evaluacionGeneral;
    private String hallazgos;

    /* Diagnósticos */
    private List<DiagnosticoPdfDto> diagnosticos;
    private String diagnosticoTexto;
    private String codigoCie10;

    /* Plan terapéutico */
    private String planTratamiento;
    private String tratamientoFarmacologico;
    private String ordenesMedicas;
    private String examenesSolicitados;
    private String incapacidad;
    private String recomendaciones;

    /* Medicamentos (fórmula) */
    private List<FormulaMedicaPdfDto> formulasMedicas;

    /* Laboratorios */
    private List<LaboratorioPdfDto> laboratorios;
}
