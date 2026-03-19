/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturaRequestDto {
    private String numeroFactura;
    @NotNull(message = "Paciente es obligatorio")
    private Long pacienteId;
    private Long ordenId;
    /** Valor total. Si se envían items, puede calcularse como suma de ítems. */
    @NotNull(message = "Valor total es obligatorio")
    private BigDecimal valorTotal;
    /** Detalle multiclínea (opcional). Si no se envía, se usa un solo ítem implícito con codigoCups/descripcionCups/tipoServicio/valorTotal de cabecera. */
    private List<FacturaItemRequestDto> items;
    private String estado;
    private String descripcion;
    private Instant fechaFactura;
    // Campos normativos Decreto 4747/2007 + RIPS
    private String codigoCups;
    private String descripcionCups;
    private String tipoServicio;
    private String responsablePago;
    private BigDecimal cuotaModeradora;
    private String numeroAutorizacionEps;
}
