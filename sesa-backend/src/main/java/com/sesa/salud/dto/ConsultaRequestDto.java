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
public class ConsultaRequestDto {

    @NotNull(message = "Paciente es obligatorio")
    private Long pacienteId;
    private Long profesionalId;
    private Long citaId;
    private String motivoConsulta;
    private String enfermedadActual;
    private String antecedentesPersonales;
    private String antecedentesFamiliares;
    private String alergias;
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
    private String diagnostico;
    private String planTratamiento;
    private String tratamientoFarmacologico;
    private String observacionesClincias;
    private String recomendaciones;
}
