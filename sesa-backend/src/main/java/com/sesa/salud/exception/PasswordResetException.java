/**
 * Error controlado en flujo de recuperación de contraseña (mensaje seguro para el cliente).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PasswordResetException extends RuntimeException {

    private final HttpStatus status;

    public PasswordResetException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
