/**
 * Controlador de plantillas SOAP para historia clínica (Res. 1995/1999).
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller;

import com.sesa.salud.dto.PlantillaSoapDto;
import com.sesa.salud.service.PlantillaSoapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/plantillas-soap")
@RequiredArgsConstructor
public class PlantillaSoapController {

    private final PlantillaSoapService plantillaSoapService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','COORDINADOR_MEDICO','ODONTOLOGO','PSICOLOGO')")
    public ResponseEntity<List<PlantillaSoapDto>> listarActivas() {
        return ResponseEntity.ok(plantillaSoapService.listarActivas());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','COORDINADOR_MEDICO','ODONTOLOGO','PSICOLOGO')")
    public ResponseEntity<PlantillaSoapDto> getById(@PathVariable("id") Long id) {
        return plantillaSoapService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
