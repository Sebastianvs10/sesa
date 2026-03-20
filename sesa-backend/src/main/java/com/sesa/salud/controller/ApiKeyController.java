/**
 * S12: CRUD de API Keys para administradores (crear, listar, desactivar).
 * La clave en texto solo se devuelve al crear.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.controller;

import com.sesa.salud.dto.ApiKeyCreateDto;
import com.sesa.salud.dto.ApiKeyResponseDto;
import com.sesa.salud.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR')")
    public List<ApiKeyResponseDto> listar() {
        return apiKeyService.listar();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<ApiKeyCreateDto> crear(@RequestBody Map<String, String> body) {
        String nombre = body != null ? body.get("nombreIntegrador") : null;
        String permisos = body != null ? body.get("permisos") : null;
        ApiKeyCreateDto created = apiKeyService.crear(nombre, permisos);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        apiKeyService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
