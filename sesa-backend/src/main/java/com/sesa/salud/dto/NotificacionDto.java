/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificacionDto {

    private Long id;
    private String titulo;
    private String contenido;
    private String tipo;
    private Long remitenteId;
    private String remitenteNombre;
    private Instant fechaEnvio;
    private Long citaId;
    private List<AdjuntoInfo> adjuntos;
    private List<DestinatarioInfo> destinatarios;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdjuntoInfo {
        private Long id;
        private String nombreArchivo;
        private String contentType;
        private Long tamano;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DestinatarioInfo {
        private Long usuarioId;
        private String usuarioEmail;
        private String usuarioNombre;
        private Boolean leido;
        private Instant fechaLectura;
    }
}
