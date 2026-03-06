/**
 * DTO de configuración de facturación electrónica DIAN por empresa (tenant).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class FacturacionElectronicaConfigDto {

    private Long id;
    private Boolean facturacionActiva;
    private String nit;
    private String razonSocial;
    private String nombreComercial;
    private String regimen;
    private String direccion;
    private String municipio;
    private String departamento;
    private String pais;
    private String emailContacto;
    private String ambiente;
    private String numeroResolucion;
    private LocalDate fechaResolucion;
    private String prefijo;
    private Long rangoDesde;
    private Long rangoHasta;
    private String claveTecnica;
    private String softwareId;
    private String softwarePin;
    private String plantillaPdf;
}

