/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequestDto {
    @Email
    @NotBlank(message = "Email es obligatorio")
    private String email;
}
