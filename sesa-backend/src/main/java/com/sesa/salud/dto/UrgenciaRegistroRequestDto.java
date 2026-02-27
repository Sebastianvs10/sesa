/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UrgenciaRegistroRequestDto {

    @NotNull(message = "Paciente es obligatorio")
    private Long pacienteId;
    private String nivelTriage;
    private String estado = "EN_ESPERA";
    private String observaciones;
    // Campos normativos Res. 5596/2015
    private String tipoLlegada;
    private String motivoConsulta;
    private Long profesionalTriageId;
    private String svPresionArterial;
    private String svFrecuenciaCardiaca;
    private String svFrecuenciaRespiratoria;
    private String svTemperatura;
    private String svSaturacionO2;
    private String svPeso;
    private String svDolorEva;
    private Integer glasgowOcular;
    private Integer glasgowVerbal;
    private Integer glasgowMotor;
}
