/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImagenDiagnosticaDto {
    private Long id;
    private Long atencionId;
    /** Para listado global: ID del paciente (historia clínica). */
    private Long pacienteId;
    /** Para listado global: nombres del paciente. */
    private String pacienteNombres;
    /** Para listado global: fecha de la atención. */
    private Instant fechaAtencion;
    private String tipo;
    private String resultado;
    private String urlArchivo;
    private Instant createdAt;
}
