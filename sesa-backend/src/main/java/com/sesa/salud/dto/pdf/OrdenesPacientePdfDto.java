/**
 * DTO para PDF de órdenes clínicas y resultados del paciente.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto.pdf;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OrdenesPacientePdfDto {
    private String empresaNombre;
    private String empresaIdentificacion;
    private String logoBase64;
    private String logoContentType;
    private String pacienteNombre;
    private String pacienteDocumento;
    private String pacienteTipoDocumento;
    private String pacienteFechaNacimiento;
    private String pacienteEdad;
    private String pacienteSexo;
    private String pacienteTelefono;
    private String pacienteEmail;
    private String pacienteDireccion;
    private String fechaGeneracion;
    /** Profesional que registró el resultado (para orden individual). */
    private String profesionalNombre;
    private String profesionalRol;
    private String profesionalIdentificacion;
    private List<OrdenPdfDto> ordenes;
}
