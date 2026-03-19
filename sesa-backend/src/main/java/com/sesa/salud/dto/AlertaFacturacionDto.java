/**
 * Una alerta para el dashboard de facturación (predictivo y recordatorios).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertaFacturacionDto {
    public enum Tipo { POR_VENCER_RADICACION, VENCIDA_RADICACION, GLOSA_PENDIENTE, ORDEN_SIN_FACTURA }
    private Tipo tipo;
    private String titulo;
    private String mensaje;
    private Long facturaId;
    private String numeroFactura;
    private Long glosaId;
    private Long ordenId;
    private Integer diasRestantes;
    private BigDecimal monto;
    private String epsNombre;
}
