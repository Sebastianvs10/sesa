/**
 * DTO para órdenes clínicas sin factura asociada (pendientes de facturar).
 * Flujo IPS/EPS Colombia: servicios prestados → cuentas médicas → RIPS → radicación.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenPendienteFacturaDto {
    private Long id;
    private Long pacienteId;
    private String pacienteNombre;
    private String pacienteDocumento;
    private String tipo;
    private String detalle;
    private BigDecimal valorEstimado;
    private Instant fechaOrden;
    private Long consultaId;
    /** Estado de la orden: PENDIENTE, COMPLETADO, etc. */
    private String estado;
    /** Para medicamentos: estado dispensación farmacia (PENDIENTE, COMPLETADA, etc.). */
    private String estadoDispensacionFarmacia;
}
