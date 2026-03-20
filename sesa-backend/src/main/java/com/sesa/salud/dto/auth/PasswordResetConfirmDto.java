/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetConfirmDto {
    @NotBlank(message = "El código de verificación es obligatorio")
    @Size(max = 128, message = "Código inválido")
    private String token;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, max = 128, message = "La contraseña debe tener entre 8 y 128 caracteres")
    @Pattern(
            regexp = "^(?=.*[A-Za-zÁÉÍÓÚáéíóúÑñ])(?=.*\\d).+$",
            message = "La contraseña debe incluir al menos una letra y un número")
    private String newPassword;
}
