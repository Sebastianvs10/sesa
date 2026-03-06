/**
 * DTO para órdenes clínicas tipo MEDICAMENTO pendientes o parciales de dispensar en farmacia.
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
public class OrdenFarmaciaPendienteDto {
    private Long id;
    private Long pacienteId;
    private String pacienteNombre;
    private String pacienteDocumento;
    private String tipoDocumentoPaciente;
    private String detalle;
    private Integer cantidadPrescrita;
    private String unidadMedida;
    private String frecuencia;
    private Integer duracionDias;
    private Instant fechaOrden;
    private String medicoNombre;
    private String estadoDispensacionFarmacia;
}
