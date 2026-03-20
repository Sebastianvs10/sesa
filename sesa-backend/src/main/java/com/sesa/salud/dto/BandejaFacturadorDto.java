/**
 * Bandeja del facturador: tareas y pendientes (radicaciones, glosas).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BandejaFacturadorDto {
    private List<TareaFacturadorDto> tareas;
    private int totalFacturasPorRadicar;
    private int totalGlosasPendientes;
}
