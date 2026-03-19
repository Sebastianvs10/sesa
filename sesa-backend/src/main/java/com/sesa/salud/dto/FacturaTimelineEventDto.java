/**
 * Un evento en la línea de tiempo de trazabilidad de una factura.
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
public class FacturaTimelineEventDto {
    public enum TipoEvento { CREADA, EMITIDA_FEV, RADICADA, GLOSA, PAGADA, RECHAZADA, ANULADA }
    private TipoEvento tipo;
    private Instant fecha;
    private String titulo;
    private String descripcion;
    private String referencia;
}
