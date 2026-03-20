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
import java.util.List;

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
    private Long consultaId;
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
    // Facturación electrónica DIAN / FEV (Res. 2275/2023, FEV-RIPS)
    private String dianEstado;
    private String dianCufe;
    private String dianQrUrl;
    /** Días hábiles restantes para radicación ante EPS (plazo 22 d hábiles — normativa vigente). */
    private Integer diasParaRadicacion;
    /** true si ya pasaron los 22 días hábiles sin radicar. */
    private Boolean vencidaRadicacion;
    /** Detalle multiclínea (cuenta médica). */
    private List<FacturaItemDto> items;
}
