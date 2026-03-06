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
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ODONTOLOGO','BACTERIOLOGO'," +
                  "'ENFERMERO','JEFE_ENFERMERIA','AUXILIAR_ENFERMERIA','PSICOLOGO'," +
                  "'REGENTE_FARMACIA','RECEPCIONISTA','COORDINADOR_MEDICO')")
    public Page<PacienteDto> list(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "activo", required = false) Boolean activo,
            @PageableDefault(size = 20) Pageable pageable) {
        if (q != null && !q.isBlank()) {
            return pacienteService.search(q, pageable);
        }
        return pacienteService.findAll(pageable, activo);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ODONTOLOGO','BACTERIOLOGO'," +
                  "'ENFERMERO','JEFE_ENFERMERIA','AUXILIAR_ENFERMERIA','PSICOLOGO'," +
                  "'REGENTE_FARMACIA','RECEPCIONISTA','COORDINADOR_MEDICO')")
    public ResponseEntity<PacienteDto> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(pacienteService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ODONTOLOGO','RECEPCIONISTA')")
    public ResponseEntity<PacienteDto> create(@Valid @RequestBody PacienteRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pacienteService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ODONTOLOGO','RECEPCIONISTA')")
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
