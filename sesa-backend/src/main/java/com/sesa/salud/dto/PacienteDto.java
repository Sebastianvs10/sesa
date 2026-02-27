/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PacienteDto {

    private Long id;
    private String tipoDocumento;
    private String documento;
    private String nombres;
    private String apellidos;
    private LocalDate fechaNacimiento;
    private String sexo;
    private String grupoSanguineo;
    private String telefono;
    private String email;
    private String direccion;
    private Long epsId;
    private String epsNombre;
    private Boolean activo;
    private Instant createdAt;
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
