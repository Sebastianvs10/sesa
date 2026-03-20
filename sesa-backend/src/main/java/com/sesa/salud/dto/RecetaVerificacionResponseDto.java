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
public class RecetaVerificacionResponseDto {
    private boolean valida;
    private String mensaje;
    private RecetaVerificacionDataDto receta;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecetaVerificacionDataDto {
        private String pacienteNombre;
        private String medicoNombre;
        private String fechaEmision;
        private List<FormulaMedicaDto> medicamentos;
        private String diagnostico;
    }
}
