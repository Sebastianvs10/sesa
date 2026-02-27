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
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonalRequestDto {

    @NotBlank(message = "Nombres son obligatorios")
    private String nombres;
    private String apellidos;

    /** Tipo de documento (CC, CE, PA, PEP, TI, RC — Res. 3374/2000). */
    private String tipoDocumento;
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
    /**
     * Rol primario del profesional (ej. MEDICO). Obligatorio al crear.
     * Se mantiene por compatibilidad; se recomienda usar {@code roles}.
     */
    private String rol;
    /**
     * Todos los roles profesionales asignados. Si se envía, reemplaza al campo {@code rol}
     * y se sincroniza con Usuario.roles.
     */
    private Set<String> roles;

    @NotNull
    private Boolean activo = true;
    // ── Normativos Res. 1449/2016 (RETHUS) y habilitación ──
    private String tarjetaProfesional;
    private String especialidadFormal;
    private String numeroRethus;
    // ── Demográficos (RIPS Res. 3374/2000) ──
    private LocalDate fechaNacimiento;
    private String sexo;
    // ── Lugar de práctica (Res. 2003/2014) ──
    private String municipio;
    private String departamento;
    // ── Vínculo laboral (Circular 047/2007 Min. Protección Social) ──
    private String tipoVinculacion;
    private LocalDate fechaIngreso;
    private LocalDate fechaRetiro;
}
