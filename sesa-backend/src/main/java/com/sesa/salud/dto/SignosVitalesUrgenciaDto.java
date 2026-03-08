/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignosVitalesUrgenciaDto {

    private Long id;
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
    private Instant createdAt;
}
