/**
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
public class PlantillaSoapDto {

    private Long id;
    private String nombre;
    private String motivoTipo;
    private String contenidoSubjetivo;
    private String contenidoObjetivo;
    private String contenidoAnalisis;
    private String contenidoPlan;
    private String codigoCie10Sugerido;
    private Boolean activo;
    private Instant createdAt;
}
