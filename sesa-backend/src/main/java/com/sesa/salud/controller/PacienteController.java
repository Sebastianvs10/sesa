/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller;

import com.sesa.salud.dto.PacienteDto;
import com.sesa.salud.dto.PacienteRequestDto;
import com.sesa.salud.service.PacienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pacientes")
@RequiredArgsConstructor
public class PacienteController {

    private final PacienteService pacienteService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public Page<PacienteDto> list(
            @RequestParam(value = "q", required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return q != null && !q.isBlank() ? pacienteService.search(q, pageable) : pacienteService.findAll(pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<PacienteDto> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(pacienteService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<PacienteDto> create(@Valid @RequestBody PacienteRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pacienteService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<PacienteDto> update(@PathVariable("id") Long id, @Valid @RequestBody PacienteRequestDto dto) {
        return ResponseEntity.ok(pacienteService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        pacienteService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
