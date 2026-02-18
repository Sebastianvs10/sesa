/**
 * DTO para crear un nuevo rol.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RolCreateRequest {

    @NotBlank(message = "El código del rol es obligatorio")
    @Size(min = 2, max = 50)
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "El código debe contener solo letras, números y guiones bajos")
    private String codigo;

    @NotBlank(message = "El nombre del rol es obligatorio")
    @Size(min = 2, max = 100)
    private String nombre;
}
