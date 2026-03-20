/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** Resultado de un broadcast de notificación a múltiples schemas. */
@Data
@Builder
public class NotificacionBroadcastResult {

    /** Número de schemas en los que se creó la notificación. */
    private int schemasProcessados;

    /** Total de destinatarios creados en todos los schemas. */
    private int totalDestinatarios;

    /** Mensajes de error por schema fallido (vacío si todo fue OK). */
    private List<String> errores;
}
