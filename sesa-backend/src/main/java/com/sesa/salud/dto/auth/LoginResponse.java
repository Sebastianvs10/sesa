/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
    /** ID del registro Personal asociado al usuario (usado para filtrar citas, consultas, etc.) */
    private Long personalId;
    private String email;
    private String nombreCompleto;
    /** Rol primario (SUPERADMINISTRADOR > ADMIN > primero) — retrocompatibilidad. */
    private String role;
    /** Todos los roles asignados al usuario. */
    private List<String> roles;
    /**
     * Rol activo seleccionado por el usuario en la sesión actual.
     * Inicialmente igual a {@code role}; puede cambiarse en el frontend sin necesidad
     * de reautenticación (se persiste en localStorage).
     */
    private String rolActivo;
    /** Esquema del tenant (empresa) para enviar en header X-Tenant-Schema en peticiones */
    private String schema;
    /** Nombre de la empresa (razón social) del tenant actual, para mostrar en la UI */
    private String empresaNombre;

    /**
     * UUID del logo de la empresa (si existe en archivo_almacenamiento).
     * El frontend construye la URL directa: {@code /api/archivos/{empresaLogoUuid}}.
     * Al incluirlo en el login, el logo está disponible inmediatamente sin un GET extra.
     */
    private String empresaLogoUuid;
}
