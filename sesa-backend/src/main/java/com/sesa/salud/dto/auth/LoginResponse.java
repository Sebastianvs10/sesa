/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String accessToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private Long expiresInMs;
    private Long userId;
    private String email;
    private String nombreCompleto;
    private String role;
    /** Esquema del tenant (empresa) para enviar en header X-Tenant-Schema en peticiones */
    private String schema;
    /** Nombre de la empresa (razón social) del tenant actual, para mostrar en la UI */
    private String empresaNombre;
}
