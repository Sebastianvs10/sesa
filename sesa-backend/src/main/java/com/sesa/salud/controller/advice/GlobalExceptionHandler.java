/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller.advice;

import com.sesa.salud.exception.PasswordResetException;
import com.sesa.salud.exception.VideoconsultaTokenInvalidoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_KEY = "error";
    private static final String MESSAGE_KEY = "message";
    private static final String STATUS_KEY = "status";

    @ExceptionHandler(PasswordResetException.class)
    public ResponseEntity<Map<String, Object>> handlePasswordReset(PasswordResetException e) {
        log.debug("Recuperación de contraseña: {}", e.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put(ERROR_KEY, "Recuperación de contraseña");
        body.put(MESSAGE_KEY, e.getMessage());
        body.put(STATUS_KEY, e.getStatus().value());
        return ResponseEntity.status(e.getStatus()).body(body);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException e) {
        log.warn("Intento de acceso con credenciales inválidas");
        Map<String, Object> body = new HashMap<>();
        body.put(ERROR_KEY, "Credenciales inválidas");
        body.put(MESSAGE_KEY, e.getMessage() != null ? e.getMessage() : "Credenciales inválidas");
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

    @ExceptionHandler(VideoconsultaTokenInvalidoException.class)
    public ResponseEntity<Map<String, Object>> handleVideoconsultaTokenInvalido(VideoconsultaTokenInvalidoException e) {
        log.warn("Token de videoconsulta inválido: {}", e.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put(ERROR_KEY, "Enlace inválido o expirado");
        body.put(MESSAGE_KEY, e.getMessage());
        body.put(STATUS_KEY, HttpStatus.FORBIDDEN.value());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
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

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUpload(MaxUploadSizeExceededException e) {
        log.warn("Archivo multipart demasiado grande: {}", e.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put(ERROR_KEY, "Archivo demasiado grande");
        body.put(
                MESSAGE_KEY,
                "La foto o la firma supera el tamaño máximo permitido. Use una imagen más pequeña (p. ej. comprimir JPG/PNG) o pida al administrador aumentar spring.servlet.multipart.max-file-size.");
        body.put(STATUS_KEY, HttpStatus.PAYLOAD_TOO_LARGE.value());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Argumento inválido: {}", e.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put(ERROR_KEY, "Datos inválidos");
        body.put(MESSAGE_KEY, e.getMessage() != null ? e.getMessage() : "Revise los datos enviados");
        body.put(STATUS_KEY, HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException e) {
        log.debug("Recurso no encontrado: {}", e.getResourcePath());
        Map<String, Object> body = new HashMap<>();
        body.put(ERROR_KEY, "Recurso no encontrado");
        body.put(MESSAGE_KEY, "La ruta solicitada no existe");
        body.put(STATUS_KEY, HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        log.error("Error en videoconsulta o lógica de negocio: {}", e.getMessage(), e);
        Map<String, Object> body = new HashMap<>();
        body.put(ERROR_KEY, "Error en el servidor");
        body.put(MESSAGE_KEY, e.getMessage() != null ? e.getMessage() : "Por favor intente más tarde");
        body.put(STATUS_KEY, HttpStatus.INTERNAL_SERVER_ERROR.value());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        log.error("Error inesperado", e);
        Map<String, Object> body = new HashMap<>();
        body.put(ERROR_KEY, "Error en el servidor");
        body.put(MESSAGE_KEY, "Por favor intente más tarde");
        body.put(STATUS_KEY, HttpStatus.INTERNAL_SERVER_ERROR.value());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
