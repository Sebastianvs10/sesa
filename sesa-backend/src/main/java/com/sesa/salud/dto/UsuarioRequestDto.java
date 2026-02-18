/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioRequestDto {
    @Email
    @NotBlank(message = "Email es obligatorio")
    private String email;
    @NotBlank(message = "Nombre completo es obligatorio")
    private String nombreCompleto;
    private String password;
    private Boolean activo;
    private Set<String> roles;
}
