/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller;

import com.sesa.salud.dto.UrgenciaRegistroDto;
import com.sesa.salud.dto.UrgenciaRegistroRequestDto;
import com.sesa.salud.service.UrgenciaRegistroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/urgencias")
@RequiredArgsConstructor
public class UrgenciaRegistroController {

    private final UrgenciaRegistroService urgenciaRegistroService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER','SUPERADMINISTRADOR')")
    public Object list(
            @RequestParam(value = "estado", required = false) String estado,
            @PageableDefault(size = 20) Pageable pageable) {
        if (estado != null && !estado.isBlank()) {
            return urgenciaRegistroService.findByEstado(estado, pageable);
        }
        return urgenciaRegistroService.findAll(pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','SUPERADMINISTRADOR')")
    public ResponseEntity<UrgenciaRegistroDto> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(urgenciaRegistroService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER','SUPERADMINISTRADOR')")
    public ResponseEntity<UrgenciaRegistroDto> create(@Valid @RequestBody UrgenciaRegistroRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(urgenciaRegistroService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','SUPERADMINISTRADOR')")
    public ResponseEntity<UrgenciaRegistroDto> update(@PathVariable("id") Long id, @Valid @RequestBody UrgenciaRegistroRequestDto dto) {
        return ResponseEntity.ok(urgenciaRegistroService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        urgenciaRegistroService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
