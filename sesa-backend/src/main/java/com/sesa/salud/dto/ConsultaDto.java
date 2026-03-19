/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultaDto {

    private Long id;
    private Long pacienteId;
    private String pacienteNombre;
    private Long profesionalId;
    private String profesionalNombre;
    private String profesionalTarjetaProfesional;
    private Long citaId;
    private String motivoConsulta;
    private String enfermedadActual;
    private String antecedentesPersonales;
    private String antecedentesFamiliares;
    private String alergias;
    private Instant fechaConsulta;
    private Instant createdAt;
    // Campos normativos Res. 1995/1999 y RIPS
    private String tipoConsulta;
    private String codigoCie10;
    private String codigoCie10Secundario;
    private String dolorEva;
    private String perimetroAbdominal;
    private String perimetroCefalico;
    private String saturacionO2;
    private String presionArterial;
    private String frecuenciaCardiaca;
    private String frecuenciaRespiratoria;
    private String temperatura;
    private String peso;
    private String talla;
    private String imc;
    private String hallazgosExamen;
    /** JSON: examen físico por subáreas (areas[].id, bien, texto; otros). */
    private String examenFisicoEstructurado;
    private String diagnostico;
    private String planTratamiento;
    private String tratamientoFarmacologico;
    private String observacionesClincias;
    private String recomendaciones;
}
