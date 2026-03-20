/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroPushRequestDto {

    @NotBlank(message = "El token es obligatorio")
    private String token;

    /** WEB, ANDROID, IOS */
    private String plataforma = "WEB";
}
