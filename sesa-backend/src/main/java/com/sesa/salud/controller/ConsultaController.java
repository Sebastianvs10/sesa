/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller;

import com.sesa.salud.dto.ConsultaDto;
import com.sesa.salud.dto.ConsultaRequestDto;
import com.sesa.salud.service.ConsultaService;
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
@RequestMapping("/consultas")
@RequiredArgsConstructor
public class ConsultaController {

    private final ConsultaService consultaService;

    @GetMapping("/mis-consultas")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','SUPERADMINISTRADOR')")
    public List<ConsultaDto> listMisConsultas(@PageableDefault(size = 50) Pageable pageable) {
        return consultaService.findMisConsultas(pageable);
    }

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','SUPERADMINISTRADOR')")
    public List<ConsultaDto> listByPaciente(@PathVariable("pacienteId") Long pacienteId,
                                            @PageableDefault(size = 20) Pageable pageable) {
        return consultaService.findByPacienteId(pacienteId, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<ConsultaDto> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(consultaService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<ConsultaDto> create(@Valid @RequestBody ConsultaRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(consultaService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<ConsultaDto> update(@PathVariable("id") Long id, @Valid @RequestBody ConsultaRequestDto dto) {
        return ResponseEntity.ok(consultaService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        consultaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
