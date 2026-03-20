/**
 * Respuesta de solicitud de recuperación (mensaje uniforme; token solo en entornos controlados).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequestResponse {

    /** Mensaje genérico (misma redacción haya o no cuenta registrada). */
    private String message;

    /** Solo si {@code sesa.auth.password-reset.expose-token=true} (desarrollo). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String devToken;
}
