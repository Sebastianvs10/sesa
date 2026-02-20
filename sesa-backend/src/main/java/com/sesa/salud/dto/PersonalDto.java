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
public class PersonalDto {

    private Long id;
    private String nombres;
    private String apellidos;
    private String identificacion;
    private String primerNombre;
    private String segundoNombre;
    private String primerApellido;
    private String segundoApellido;
    private String celular;
    private String email;
    private String rol;
    private String fotoUrl;
    private String firmaUrl;
    private Boolean activo;
    private Instant createdAt;
}
