/**
 * S16: DTO fila del reporte de auditoría HC por profesional.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaHcProfesionalDto {
    private Long profesionalId;
    private String profesionalNombre;
    private long totalAtenciones;
    private long atencionesCompletas;
    private double porcentajeCompletas;
    private double puntuacionMedia;
    private boolean bajoUmbral;
}
