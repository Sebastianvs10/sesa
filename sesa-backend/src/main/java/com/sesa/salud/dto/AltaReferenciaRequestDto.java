/**
 * S6: Request para dar alta (urgencias) o generar referencia (consulta/atención).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AltaReferenciaRequestDto {

    private String diagnostico;
    private String tratamiento;
    private String recomendaciones;
    private String proximaCita;
    /** Solo referencia: motivo y nivel. */
    private String motivoReferencia;
    private String nivelReferencia;
}
