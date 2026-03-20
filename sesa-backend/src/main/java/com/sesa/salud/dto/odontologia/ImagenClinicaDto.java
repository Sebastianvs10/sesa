/**
 * DTO de imagen clínica / radiografía.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto.odontologia;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ImagenClinicaDto {
    private Long id;
    private Long pacienteId;
    private Long profesionalId;
    private String profesionalNombre;
    private Long consultaId;
    private Integer piezaFdi;
    private String tipo;
    private String nombreArchivo;
    private String url;
    private String thumbnailBase64;
    private String descripcion;
    private Instant createdAt;
}
