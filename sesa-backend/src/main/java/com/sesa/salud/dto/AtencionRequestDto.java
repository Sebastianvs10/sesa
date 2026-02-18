/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AtencionRequestDto {
    @NotNull
    private Long historiaId;
    @NotNull
    private Long profesionalId;
    private Instant fechaAtencion;
    private String motivoConsulta;
    private String enfermedadActual;
    private String versionEnfermedad;
    private String sintomasAsociados;
    private String factoresMejoran;
    private String factoresEmpeoran;
    private String revisionSistemas;
    private String presionArterial;
    private String frecuenciaCardiaca;
    private String frecuenciaRespiratoria;
    private String temperatura;
    private String peso;
    private String talla;
    private String imc;
    private String evaluacionGeneral;
    private String hallazgos;
    private String diagnostico;
    private String codigoCie10;
    private String planTratamiento;
    private String tratamientoFarmacologico;
    private String ordenesMedicas;
    private String examenesSolicitados;
    private String incapacidad;
    private String recomendaciones;
    private List<DiagnosticoDto> diagnosticos;
    private List<ProcedimientoDto> procedimientos;
    private List<FormulaMedicaDto> formulasMedicas;
}
