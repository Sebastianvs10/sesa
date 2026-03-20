/**
 * DTO para última consulta SOAP en PDF de historia clínica.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto.pdf;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ConsultaSoapPdfDto {

    private String fechaConsulta;
    private String motivoConsulta;
    private String enfermedadActual;
    private String codigoCie10;
    private String codigoCie10Secundario;

    /* Signos vitales */
    private String presionArterial;
    private String frecuenciaCardiaca;
    private String frecuenciaRespiratoria;
    private String temperatura;
    private String peso;
    private String talla;
    private String imc;
    private String saturacionO2;
    private String dolorEva;
    private String perimetroAbdominal;
    private String perimetroCefalico;

    private String hallazgosExamen;
    private String diagnostico;
    private String planTratamiento;
    private String tratamientoFarmacologico;
    private String observacionesClinicas;
    private String recomendaciones;

    /* Profesional */
    private String profesionalNombre;
    private String profesionalRol;
    private String profesionalEspecialidad;
    private String profesionalIdentificacion;
    private String profesionalTarjetaProfesional;
    private String profesionalNumeroRethus;
    private String firmaBase64;
    private String firmaContentType;

    /* Órdenes de la consulta */
    private List<OrdenPdfDto> ordenes;
}
