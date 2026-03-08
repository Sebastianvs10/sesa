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
import java.util.List;

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
    /** Alergias del paciente desde Historia Clínica (seguridad del paciente, Decreto 1011/2006). */
    private String alergiasPaciente;
    /** Detalle del primer ítem o de la orden legacy (para compatibilidad). */
    private String detalle;
    private Integer cantidadPrescrita;
    private String unidadMedida;
    private String frecuencia;
    private Integer duracionDias;
    private Instant fechaOrden;
    private String medicoNombre;
    private String estadoDispensacionFarmacia;
    /** Ítems de medicamento en órdenes compuestas (varios medicamentos en una sola orden). */
    private List<OrdenFarmaciaPendienteItemDto> items;
}
