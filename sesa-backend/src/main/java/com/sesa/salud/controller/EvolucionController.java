/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller;

import com.sesa.salud.dto.EvolucionDto;
import com.sesa.salud.dto.EvolucionRequestDto;
import com.sesa.salud.service.EvolucionService;
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
@RequestMapping("/evoluciones")
@RequiredArgsConstructor
public class EvolucionController {

    private final EvolucionService evolucionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ODONTOLOGO','COORDINADOR_MEDICO','JEFE_ENFERMERIA','ENFERMERO')")
    public ResponseEntity<List<EvolucionDto>> listByAtencion(
            @RequestParam("atencionId") Long atencionId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(evolucionService.findByAtencionId(atencionId, pageable));
    }

    /** Evoluciones del paciente (urgencias y otras atenciones) para timeline en Historia Clínica. */
    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ODONTOLOGO','COORDINADOR_MEDICO','JEFE_ENFERMERIA','ENFERMERO')")
    public ResponseEntity<List<EvolucionDto>> listByPaciente(
            @PathVariable("pacienteId") Long pacienteId,
            @PageableDefault(size = 100) Pageable pageable) {
        return ResponseEntity.ok(evolucionService.findByPacienteId(pacienteId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ODONTOLOGO','COORDINADOR_MEDICO','JEFE_ENFERMERIA','ENFERMERO')")
    public ResponseEntity<EvolucionDto> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(evolucionService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ODONTOLOGO','COORDINADOR_MEDICO')")
    public ResponseEntity<EvolucionDto> create(@Valid @RequestBody EvolucionRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(evolucionService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO')")
    public ResponseEntity<EvolucionDto> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody EvolucionRequestDto dto) {
        return ResponseEntity.ok(evolucionService.update(id, dto));
    }
}
