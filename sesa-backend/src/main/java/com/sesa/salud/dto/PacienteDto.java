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
}
