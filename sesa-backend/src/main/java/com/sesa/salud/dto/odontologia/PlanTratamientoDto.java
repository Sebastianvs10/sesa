/**
 * DTO de plan de tratamiento con sus ítems.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto.odontologia;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PlanTratamientoDto {
    private Long id;
    private Long pacienteId;
    private String pacienteNombre;
    private Long profesionalId;
    private String profesionalNombre;
    private Long consultaId;
    private String nombre;
    private Integer fase;
    private String descripcion;
    private BigDecimal valorTotal;
    private BigDecimal descuento;
    private BigDecimal valorFinal;
    private BigDecimal valorAbonado;
    private BigDecimal saldoPendiente;
    private String tipoPago;
    private String estado;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private List<PlanTratamientoItemDto> items;
    private Instant createdAt;
    private Instant updatedAt;
}
