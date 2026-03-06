/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionCreateRequest {

    @NotBlank(message = "El título es obligatorio")
    private String titulo;

    @NotBlank(message = "El contenido es obligatorio")
    private String contenido;

    private String tipo = "GENERAL";

    /**
     * IDs de usuarios destinatarios. Requerido salvo que {@code broadcastTodos} sea {@code true}.
     */
    private List<Long> destinatarioIds;

    /**
     * Cuando {@code true} (solo ADMIN/SUPERADMINISTRADOR), la notificación se envía
     * a todos los usuarios activos del schema actual ignorando {@code destinatarioIds}.
     */
    private boolean broadcastTodos = false;

    /** Opcional: ID de cita asociada (recordatorios automáticos). */
    private Long citaId;
}
