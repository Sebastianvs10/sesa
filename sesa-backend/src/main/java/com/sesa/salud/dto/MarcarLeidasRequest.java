/**
 * Solicitud para marcar varias notificaciones como leídas (solo destinatario actual).
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarcarLeidasRequest {

    @NotNull(message = "La lista de IDs no puede ser nula")
    private List<Long> notificacionIds;
}
