/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import com.sesa.salud.entity.enums.EstadoTurno;
import com.sesa.salud.entity.enums.ServicioClinico;
import com.sesa.salud.entity.enums.TipoTurno;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

/** Representación de un turno devuelta al cliente. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurnoDto {

    private Long id;

    private Long personalId;
    private String personalNombres;
    private String personalApellidos;

    private Long programacionMesId;

    private ServicioClinico servicio;
    private String servicioEtiqueta;

    private TipoTurno tipoTurno;
    private String tipoTurnoEtiqueta;
    private Integer duracionHoras;

    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;

    private EstadoTurno estado;
    private Boolean esFestivo;
    private String notas;

    private Instant createdAt;
    private Instant updatedAt;
}
