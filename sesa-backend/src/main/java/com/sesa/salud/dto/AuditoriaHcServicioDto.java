/**
 * S16: DTO fila del reporte de auditoría HC por servicio (tipo consulta).
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
public class AuditoriaHcServicioDto {
    private String servicio;
    private long totalAtenciones;
    private long atencionesCompletas;
    private double porcentajeCompletas;
    private double puntuacionMedia;
}
