/**
 * DTO para actualizar módulos de un rol.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class RolModulosRequest {

    @NotEmpty(message = "Debe indicar al menos un módulo")
    private List<String> modulos;
}
