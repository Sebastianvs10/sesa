/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalDto {

    private Long id;
    private String nombres;
    private String apellidos;
    /** Tipo de documento (CC, CE, PA, PEP, TI, RC — Res. 3374/2000 RIPS). */
    private String tipoDocumento;
    private String identificacion;
    private String primerNombre;
    private String segundoNombre;
    private String primerApellido;
    private String segundoApellido;
    private String celular;
    /** Correo profesional para documentos clínicos. */
    private String email;
    /** Rol primario (compatibilidad). */
    private String rol;
    /** Todos los roles profesionales asignados. */
    private Set<String> roles;
    private String fotoUrl;
    private String firmaUrl;
    private Boolean activo;
    private Instant createdAt;
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
