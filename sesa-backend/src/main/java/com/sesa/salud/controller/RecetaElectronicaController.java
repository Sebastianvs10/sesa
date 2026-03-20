/**
 * Receta electrónica con token verificable (QR anti-falsificación).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.controller;

import com.sesa.salud.dto.*;
import com.sesa.salud.service.RecetaElectronicaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/recetas")
@RequiredArgsConstructor
public class RecetaElectronicaController {

    private final RecetaElectronicaService recetaElectronicaService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','ODONTOLOGO','SUPERADMINISTRADOR')")
    public ResponseEntity<RecetaElectronicaDto> crear(@RequestBody(required = false) CrearRecetaRequestDto request) {
        if (request == null || request.getAtencionId() == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recetaElectronicaService.crearDesdeAtencion(request.getAtencionId(), request.getObservaciones()));
    }

    @PostMapping("/crear-con-formulas")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','ODONTOLOGO','SUPERADMINISTRADOR')")
    public ResponseEntity<RecetaElectronicaDto> crearConFormulas(@Valid @RequestBody CrearRecetaConFormulasRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recetaElectronicaService.crearConFormulas(request));
    }

    @GetMapping("/verificar/{token}")
    public ResponseEntity<RecetaVerificacionResponseDto> verificar(@PathVariable String token) {
        return ResponseEntity.ok(recetaElectronicaService.verificar(token));
    }
}
