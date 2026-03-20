/**
 * S17: Detalle de un RDA recibido (resumen legible, no Bundle completo).
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
public class RdaRecibidoDetalleDto {
    private Long id;
    private Long pacienteId;
    private String idMinisterio;
    private String tipoRda;
    private Instant fechaAtencion;
    private String institucionOrigen;
    private String resumenLegible;
    private Instant fetchedAt;
}
