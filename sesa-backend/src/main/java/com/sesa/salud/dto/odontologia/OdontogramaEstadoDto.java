/**
 * DTO de estado de pieza dental (odontograma).
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto.odontologia;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OdontogramaEstadoDto {
    private Long id;
    private Long pacienteId;
    private Long profesionalId;
    private String profesionalNombre;
    private Long consultaId;
    private Integer piezaFdi;
    private String superficie;
    private String estado;
    private String observacion;
    private Instant createdAt;
}
