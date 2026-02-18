/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetConfirmDto {
    @NotBlank(message = "Token es obligatorio")
    private String token;
    @NotBlank(message = "Nueva contraseña es obligatoria")
    private String newPassword;
}
