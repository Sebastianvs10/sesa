/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller;

import com.sesa.salud.dto.DolorDto;
import com.sesa.salud.dto.DolorRequestDto;
import com.sesa.salud.service.DolorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dolores")
@RequiredArgsConstructor
public class DolorController {

    private final DolorService dolorService;

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<List<DolorDto>> getByPaciente(@PathVariable("pacienteId") Long pacienteId) {
        return ResponseEntity.ok(dolorService.findByPacienteId(pacienteId));
    }

    @GetMapping("/historia/{historiaClinicaId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<List<DolorDto>> getByHistoriaClinica(@PathVariable("historiaClinicaId") Long historiaClinicaId) {
        return ResponseEntity.ok(dolorService.findByHistoriaClinicaId(historiaClinicaId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<DolorDto> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(dolorService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<DolorDto> create(@Valid @RequestBody DolorRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dolorService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<DolorDto> update(@PathVariable("id") Long id, @Valid @RequestBody DolorRequestDto dto) {
        return ResponseEntity.ok(dolorService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        dolorService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
