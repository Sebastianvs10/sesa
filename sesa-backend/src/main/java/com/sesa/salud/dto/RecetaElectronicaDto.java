/**
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
public class RecetaElectronicaDto {
    private Long id;
    private String tokenVerificacion;
    private String urlVerificacion;
    private Long atencionId;
    private Long consultaId;
    private String pacienteNombre;
    private String pacienteDocumento;
    private String medicoNombre;
    private String medicoTarjetaProfesional;
    private String fechaEmision;
    private List<FormulaMedicaDto> medicamentos;
    private String diagnostico;
    private String observaciones;
    private String validaHasta;
}
