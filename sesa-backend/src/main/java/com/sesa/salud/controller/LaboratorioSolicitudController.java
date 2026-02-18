/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller;

import com.sesa.salud.dto.LaboratorioSolicitudDto;
import com.sesa.salud.dto.LaboratorioSolicitudRequestDto;
import com.sesa.salud.service.LaboratorioSolicitudService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/laboratorio-solicitudes")
@RequiredArgsConstructor
public class LaboratorioSolicitudController {

    private final LaboratorioSolicitudService laboratorioSolicitudService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER','SUPERADMINISTRADOR')")
    public Object list(
            @RequestParam(value = "estado", required = false) String estado,
            @PageableDefault(size = 20) Pageable pageable) {
        if (estado != null && !estado.isBlank()) {
            return laboratorioSolicitudService.findByEstado(estado, pageable);
        }
        return laboratorioSolicitudService.findAll(pageable);
    }

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','SUPERADMINISTRADOR')")
    public List<LaboratorioSolicitudDto> listByPaciente(@PathVariable("pacienteId") Long pacienteId,
                                                        @PageableDefault(size = 20) Pageable pageable) {
        return laboratorioSolicitudService.findByPacienteId(pacienteId, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','SUPERADMINISTRADOR')")
    public ResponseEntity<LaboratorioSolicitudDto> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(laboratorioSolicitudService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER','SUPERADMINISTRADOR')")
    public ResponseEntity<LaboratorioSolicitudDto> create(@Valid @RequestBody LaboratorioSolicitudRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(laboratorioSolicitudService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','SUPERADMINISTRADOR')")
    public ResponseEntity<LaboratorioSolicitudDto> update(@PathVariable("id") Long id, @Valid @RequestBody LaboratorioSolicitudRequestDto dto) {
        return ResponseEntity.ok(laboratorioSolicitudService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        laboratorioSolicitudService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
