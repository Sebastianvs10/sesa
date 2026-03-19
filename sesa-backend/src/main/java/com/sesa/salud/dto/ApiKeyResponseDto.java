/**
 * S12: DTO para listar API Keys (sin exponer la clave).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyResponseDto {
    private Long id;
    private String nombreIntegrador;
    private String permisos;
    private Boolean activo;
    private Instant createdAt;
}
