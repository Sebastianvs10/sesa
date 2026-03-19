/**
 * DTO con datos básicos obtenidos de consulta por documento (ej. ADRES/BDUA).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultaDocumentoDto {

    private String tipoDocumento;
    private String documento;
    private String nombres;
    private String apellidos;
    private LocalDate fechaNacimiento;
    private String sexo;
    private String municipioResidencia;
    private String departamentoResidencia;
    private String regimenAfiliacion;
    private String tipoUsuario;
    private String epsNombre;
    private Long epsId;
    private String estadoAfiliacion;
}
