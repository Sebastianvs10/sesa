/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccesoAuditoriaDto {
    private Long id;
    private String email;
    private String evento;
    private String ip;
    private String detalle;
    private Instant fecha;
}
