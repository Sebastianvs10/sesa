/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import lombok.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtencionDto {
    private Long id;
    private Long historiaId;
    private Long profesionalId;
    private String profesionalNombre;
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
    @Builder.Default
    private List<DiagnosticoDto> diagnosticos = new ArrayList<>();
    @Builder.Default
    private List<ProcedimientoDto> procedimientos = new ArrayList<>();
    @Builder.Default
    private List<FormulaMedicaDto> formulasMedicas = new ArrayList<>();
}
