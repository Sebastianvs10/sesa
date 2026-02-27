/**
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
public class FacturaDto {
    private Long id;
    private String numeroFactura;
    private Long pacienteId;
    private String pacienteNombre;
    private String pacienteDocumento;
    private String pacienteTipoDocumento;
    private String epsNombre;
    private String epsCodigo;
    private Long ordenId;
    private BigDecimal valorTotal;
    private String estado;
    private String descripcion;
    private Instant fechaFactura;
    private Instant createdAt;
    // Campos normativos Decreto 4747/2007 + RIPS
    private String codigoCups;
    private String descripcionCups;
    private String tipoServicio;
    private String responsablePago;
    private BigDecimal cuotaModeradora;
    private String numeroAutorizacionEps;
}
