/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto.rda;

import com.sesa.salud.entity.RdaEnvio;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class RdaStatusDto {
    private Long rdaId;
    private Long atencionId;
    private RdaEnvio.TipoRda tipoRda;
    private RdaEnvio.EstadoRda estadoEnvio;
    private String idMinisterio;
    private Instant fechaGeneracion;
    private Instant fechaEnvio;
    private Instant fechaConfirmacion;
    private String errorMensaje;
    private Integer reintentos;
    private String bundleJson;
}
