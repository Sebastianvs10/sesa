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
public class UrgenciaRegistroDto {

    private Long id;
    private Long pacienteId;
    private String pacienteNombre;
    private String pacienteDocumento;
    private String nivelTriage;
    private String estado;
    private LocalDateTime fechaHoraIngreso;
    private String observaciones;
    private Long atencionId;
    private Instant createdAt;
    // Campos normativos Res. 5596/2015
    private String tipoLlegada;
    private String motivoConsulta;
    private Long profesionalTriageId;
    private String profesionalTriageNombre;
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
    /** S6: Datos de alta (diagnóstico, tratamiento, recomendaciones, próxima cita). */
    private String altaDiagnostico;
    private String altaTratamiento;
    private String altaRecomendaciones;
    private String altaProximaCita;
}
