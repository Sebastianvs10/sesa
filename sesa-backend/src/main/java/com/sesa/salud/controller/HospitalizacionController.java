/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.controller;

import com.sesa.salud.dto.HospitalizacionDto;
import com.sesa.salud.dto.HospitalizacionRequestDto;
import com.sesa.salud.service.HospitalizacionService;
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
@RequestMapping("/hospitalizaciones")
@RequiredArgsConstructor
public class HospitalizacionController {

    private final HospitalizacionService hospitalizacionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','ENFERMERIA','SUPERADMINISTRADOR')")
    public List<HospitalizacionDto> listByEstado(@RequestParam(value = "estado", required = false) String estado,
                                                 @PageableDefault(size = 200) Pageable pageable) {
        if (estado != null && !estado.isBlank()) return hospitalizacionService.findByEstado(estado, pageable);
        return hospitalizacionService.findAll(pageable);
    }

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','ENFERMERIA','SUPERADMINISTRADOR')")
    public List<HospitalizacionDto> listByPaciente(@PathVariable("pacienteId") Long pacienteId,
                                                   @PageableDefault(size = 20) Pageable pageable) {
        return hospitalizacionService.findByPacienteId(pacienteId, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','ENFERMERIA','SUPERADMINISTRADOR')")
    public ResponseEntity<HospitalizacionDto> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(hospitalizacionService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<HospitalizacionDto> create(@Valid @RequestBody HospitalizacionRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hospitalizacionService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','ENFERMERIA','SUPERADMINISTRADOR')")
    public ResponseEntity<HospitalizacionDto> update(@PathVariable("id") Long id, @Valid @RequestBody HospitalizacionRequestDto dto) {
        return ResponseEntity.ok(hospitalizacionService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        hospitalizacionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
