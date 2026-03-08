/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignosVitalesUrgenciaRequestDto {

    private Long urgenciaRegistroId;
    private LocalDateTime fechaHora;
    private String presionArterial;
    private String frecuenciaCardiaca;
    private String frecuenciaRespiratoria;
    private String temperatura;
    private String saturacionO2;
    private String peso;
    private String dolorEva;
    private Integer glasgowOcular;
    private Integer glasgowVerbal;
    private Integer glasgowMotor;
}
