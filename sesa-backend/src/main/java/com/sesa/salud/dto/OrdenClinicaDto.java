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
public class OrdenClinicaDto {
    private Long id;
    private Long pacienteId;
    private String pacienteNombre;
    private Long consultaId;
    /** Tipo/detalle en cabecera (órdenes legacy de un solo ítem). */
    private String tipo;
    private String detalle;
    private Integer cantidadPrescrita;
    private String unidadMedida;
    private String frecuencia;
    private Integer duracionDias;
    private String estado;
    private String resultado;
    private Instant fechaResultado;
    /** Nombre del profesional que registró el resultado (ej. bacteriólogo). */
    private String resultadoRegistradoPorNombre;
    /** Rol del profesional que registró el resultado. */
    private String resultadoRegistradoPorRol;
    private BigDecimal valorEstimado;
    private Instant createdAt;
    /** Ítems de la orden (varios medicamentos/labs/procedimientos en una sola orden). */
    private List<OrdenClinicaItemDto> items;
    /** Si el resultado fue marcado como crítico (S2). */
    private Boolean resultadoCritico;
    /** Si el usuario actual ya registró lectura del resultado crítico (S2). */
    private Boolean leidoPorUsuarioActual;
}
