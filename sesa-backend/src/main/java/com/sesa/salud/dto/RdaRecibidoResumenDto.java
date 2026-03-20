/**
 * S17: Resumen de un RDA recibido de otra IPS (IHCE).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RdaRecibidoResumenDto {
    private Long id;
    private Long pacienteId;
    private String idMinisterio;
    private String tipoRda;
    private Instant fechaAtencion;
    private String institucionOrigen;
    private Instant fetchedAt;
}
