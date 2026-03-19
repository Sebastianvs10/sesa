/**
 * Una tarea en la bandeja del facturador (radicar, responder glosa, etc.).
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
public class TareaFacturadorDto {
    public enum TipoTarea { RADICAR, RESPONDER_GLOSA }
    private TipoTarea tipo;
    private Long id;           // facturaId o glosaId
    private String referencia; // número factura o "Glosa #X"
    private String descripcion;
    private Integer diasRestantes;
    private boolean vencida;
    private BigDecimal monto;
}
