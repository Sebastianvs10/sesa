/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ConsentimientoInformadoDto {
    private Long id;
    private Long pacienteId;
    private String pacienteNombre;
    private String pacienteDocumento;
    private Long profesionalId;
    private String profesionalNombre;
    private String tipo;
    private String estado;
    private String procedimiento;
    private Instant fechaSolicitud;
    private Instant fechaFirma;
    private String observaciones;
    private String firmaCanvasData;
    private Instant createdAt;
    private Instant updatedAt;
}
