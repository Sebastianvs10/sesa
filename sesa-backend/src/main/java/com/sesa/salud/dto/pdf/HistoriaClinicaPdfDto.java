/**
 * DTO completo para generación de PDF de Historia Clínica
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto.pdf;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HistoriaClinicaPdfDto {

    /* ── Empresa / IPS ───────────────────────────────── */
    private String empresaNombre;
    private String empresaIdentificacion;
    private String empresaTipoDocumento;
    private String empresaDireccion;
    private String empresaTelefono;
    private String empresaMunicipio;
    private String empresaDepartamento;
    private String empresaRegimen;
    private String logoBase64;
    private String logoContentType;

    /* ── Historia Clínica ────────────────────────────── */
    private Long historiaId;
    private String estadoHistoria;
    private String fechaApertura;
    private String fechaGeneracion;

    /* ── Paciente ─────────────────────────────────────── */
    private String pacienteNombre;
    private String pacienteDocumento;
    private String pacienteTipoDocumento;
    private String pacienteFechaNacimiento;
    private String pacienteEdad;
    private String pacienteSexo;
    private String pacienteTelefono;
    private String pacienteEmail;
    private String pacienteDireccion;
    private String epsNombre;
    private String epsCodigo;

    /* ── Antecedentes ─────────────────────────────────── */
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

    /* ── Atenciones ───────────────────────────────────── */
    private List<AtencionPdfDto> atenciones;
}
