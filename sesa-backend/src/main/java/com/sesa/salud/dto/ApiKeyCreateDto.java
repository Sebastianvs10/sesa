/**
 * S12: DTO respuesta al crear API Key (la clave en texto solo se devuelve una vez).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyCreateDto {
    private Long id;
    private String nombreIntegrador;
    /** Clave en texto plano; solo se devuelve al crear, no se almacena. */
    private String apiKeyRaw;
    private String permisos;
}
