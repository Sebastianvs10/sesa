/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller.advice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_KEY = "error";
    private static final String MESSAGE_KEY = "message";
    private static final String STATUS_KEY = "status";

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException e) {
        log.warn("Intento de acceso con credenciales inválidas");
        Map<String, Object> body = new HashMap<>();
        body.put(ERROR_KEY, "Credenciales inválidas");
        body.put(STATUS_KEY, HttpStatus.UNAUTHORIZED.value());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        log.debug("Validación fallida en petición");
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(err -> {
            String field = ((FieldError) err).getField();
            errors.put(field, err.getDefaultMessage());
        });
        Map<String, Object> body = new HashMap<>();
        body.put(ERROR_KEY, "Validación fallida");
        body.put("errors", errors);
        body.put(STATUS_KEY, HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("Violación de integridad de datos: {}", e.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put(ERROR_KEY, "Error de integridad en los datos");
        body.put(MESSAGE_KEY, "Verifique los valores ingresados");
        body.put(STATUS_KEY, HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.debug("Mensaje HTTP no legible");
        Map<String, Object> body = new HashMap<>();
        body.put(ERROR_KEY, "Formato de petición inválido");
        body.put(MESSAGE_KEY, "Verifique el formato JSON");
        body.put(STATUS_KEY, HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        log.error("Error inesperado", e);
        Map<String, Object> body = new HashMap<>();
        body.put(ERROR_KEY, "Error en el servidor");
        body.put(MESSAGE_KEY, "Por favor intente más tarde");
        body.put(STATUS_KEY, HttpStatus.INTERNAL_SERVER_ERROR.value());
        // NUNCA exponer el stack trace o mensaje detallado en producción
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
