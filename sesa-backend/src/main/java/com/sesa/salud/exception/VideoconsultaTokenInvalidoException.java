/**
 * Excepción cuando el token de invitación a videoconsulta es inválido o no coincide con la sala.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.exception;

public class VideoconsultaTokenInvalidoException extends RuntimeException {

    public VideoconsultaTokenInvalidoException(String message) {
        super(message);
    }
}
