/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import com.sesa.salud.entity.enums.EstadoTurno;
import com.sesa.salud.entity.enums.ServicioClinico;
import com.sesa.salud.entity.enums.TipoTurno;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Payload para crear o actualizar un turno. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TurnoRequestDto {

    @NotNull(message = "El profesional es obligatorio")
    private Long personalId;

    @NotNull(message = "El tipo de turno es obligatorio")
    private TipoTurno tipoTurno;

    @NotNull(message = "El servicio clínico es obligatorio")
    private ServicioClinico servicio;

    /**
     * Fecha del turno (solo la parte de fecha; la hora de inicio
     * se deriva del tipo de turno en el backend).
     */
    @NotNull(message = "La fecha del turno es obligatoria")
    private LocalDate fecha;

    /** Estado inicial del turno; si es null se asume BORRADOR. */
    private EstadoTurno estado;

    private String notas;
}
