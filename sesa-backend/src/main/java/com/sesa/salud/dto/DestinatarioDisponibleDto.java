/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Usuario disponible como destinatario de notificación. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DestinatarioDisponibleDto {

    private Long   id;
    private String nombre;
    private String email;
    private String rol;
}
