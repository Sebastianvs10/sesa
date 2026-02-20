/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonalRequestDto {

    @NotBlank(message = "Nombres son obligatorios")
    private String nombres;
    private String apellidos;

    private String identificacion;
    private String primerNombre;
    private String segundoNombre;
    private String primerApellido;
    private String segundoApellido;
    private String celular;
    /** Correo para acceso (obligatorio al crear personal con usuario). */
    private String email;
    /** Contraseña para crear usuario de acceso (solo al crear). */
    private String password;
    /** Rol del profesional (ej. MEDICO, ENFERMERO). Obligatorio al crear. */
    private String rol;

    @NotNull
    private Boolean activo = true;
}
