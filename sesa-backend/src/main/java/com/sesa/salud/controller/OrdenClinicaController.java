/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller;

import com.sesa.salud.dto.OrdenClinicaDto;
import com.sesa.salud.dto.OrdenClinicaRequestDto;
import com.sesa.salud.service.OrdenClinicaService;
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
@RequestMapping("/ordenes-clinicas")
@RequiredArgsConstructor
public class OrdenClinicaController {

    private final OrdenClinicaService ordenClinicaService;

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ODONTOLOGO','BACTERIOLOGO','COORDINADOR_MEDICO','ENFERMERO','JEFE_ENFERMERIA')")
    public List<OrdenClinicaDto> listByPaciente(@PathVariable("pacienteId") Long pacienteId,
                                                 @PageableDefault(size = 50) Pageable pageable) {
        return ordenClinicaService.findByPacienteId(pacienteId, pageable);
    }

    @GetMapping("/laboratorio")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','BACTERIOLOGO','MEDICO','ODONTOLOGO','COORDINADOR_MEDICO')")
    public List<OrdenClinicaDto> listOrdenesLaboratorio(@PageableDefault(size = 100) Pageable pageable) {
        return ordenClinicaService.findByTipo("LABORATORIO", pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ODONTOLOGO','BACTERIOLOGO','COORDINADOR_MEDICO','ENFERMERO','JEFE_ENFERMERIA')")
    public ResponseEntity<OrdenClinicaDto> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ordenClinicaService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ODONTOLOGO','COORDINADOR_MEDICO')")
    public ResponseEntity<OrdenClinicaDto> create(@Valid @RequestBody OrdenClinicaRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ordenClinicaService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ODONTOLOGO','COORDINADOR_MEDICO')")
    public ResponseEntity<OrdenClinicaDto> update(@PathVariable("id") Long id, @Valid @RequestBody OrdenClinicaRequestDto dto) {
        return ResponseEntity.ok(ordenClinicaService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        ordenClinicaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
