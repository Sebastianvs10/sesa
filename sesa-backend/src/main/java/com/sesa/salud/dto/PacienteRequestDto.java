/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PacienteRequestDto {

    private String tipoDocumento;
    @NotBlank(message = "Documento es obligatorio")
    private String documento;
    @NotBlank(message = "Nombres son obligatorios")
    private String nombres;
    private String apellidos;
    private LocalDate fechaNacimiento;
    private String sexo;
    private String grupoSanguineo;
    private String telefono;
    private String email;
    private String direccion;
    private Long epsId;
    @NotNull
    private Boolean activo = true;
    // Campos normativos Res. 3374/2000 (RIPS)
    private String municipioResidencia;
    private String departamentoResidencia;
    private String zonaResidencia;
    private String regimenAfiliacion;
    private String tipoUsuario;
    private String contactoEmergenciaNombre;
    private String contactoEmergenciaTelefono;
    private String estadoCivil;
    private String escolaridad;
    private String ocupacion;
    private String pertenenciaEtnica;
}
