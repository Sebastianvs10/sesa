/**
 * DTO para PDF de resumen de alta de urgencias.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto.pdf;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AltaUrgenciaPdfDto {
    private String empresaNombre;
    private String empresaIdentificacion;
    private String logoBase64;
    private String logoContentType;
    private String fechaGeneracion;
    private String pacienteNombre;
    private String pacienteDocumento;
    private String diagnostico;
    private String tratamiento;
    private String recomendaciones;
    private String proximaCita;
}
