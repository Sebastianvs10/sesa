/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import com.sesa.salud.entity.enums.EstadoProgramacion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Estado y metadatos de la programación mensual. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgramacionMesDto {

    private Long id;
    private Integer anio;
    private Integer mes;
    private EstadoProgramacion estado;

    private Long   creadoPorId;
    private String creadoPorNombre;

    private Long   aprobadoPorId;
    private String aprobadoPorNombre;

    private Instant fechaAprobacion;
    private String  observaciones;

    private Instant createdAt;
    private Instant updatedAt;
}
