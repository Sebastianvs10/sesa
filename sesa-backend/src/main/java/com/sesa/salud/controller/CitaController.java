/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller;

import com.sesa.salud.dto.CitaDto;
import com.sesa.salud.dto.CitaRequestDto;
import com.sesa.salud.service.CitaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/citas")
@RequiredArgsConstructor
public class CitaController {

    private final CitaService citaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public List<CitaDto> listByFecha(
            @RequestParam(value = "fecha", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        if (fecha != null) {
            return citaService.findByFecha(fecha);
        }
        return citaService.findByFecha(LocalDate.now());
    }

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public List<CitaDto> listByPaciente(@PathVariable("pacienteId") Long pacienteId,
                                        @RequestParam(value = "page", defaultValue = "0") int page,
                                        @RequestParam(value = "size", defaultValue = "20") int size) {
        return citaService.findByPacienteId(pacienteId, org.springframework.data.domain.PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<CitaDto> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(citaService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<CitaDto> create(@Valid @RequestBody CitaRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(citaService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<CitaDto> update(@PathVariable("id") Long id, @Valid @RequestBody CitaRequestDto dto) {
        return ResponseEntity.ok(citaService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        citaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
