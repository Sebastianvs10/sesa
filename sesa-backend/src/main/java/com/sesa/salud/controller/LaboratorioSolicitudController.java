/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller;

import com.sesa.salud.dto.LaboratorioSolicitudDto;
import com.sesa.salud.dto.LaboratorioSolicitudRequestDto;
import com.sesa.salud.dto.ResultadoLaboratorioDto;
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
import java.util.Map;

@RestController
@RequestMapping("/laboratorio-solicitudes")
@RequiredArgsConstructor
public class LaboratorioSolicitudController {

    private final LaboratorioSolicitudService laboratorioSolicitudService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ODONTOLOGO','BACTERIOLOGO','COORDINADOR_MEDICO')")
    public Object list(
            @RequestParam(value = "estado", required = false) String estado,
            @PageableDefault(size = 20) Pageable pageable) {
        if (estado != null && !estado.isBlank()) {
            return laboratorioSolicitudService.findByEstado(estado, pageable);
        }
        return laboratorioSolicitudService.findAll(pageable);
    }

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ODONTOLOGO','BACTERIOLOGO','COORDINADOR_MEDICO')")
    public List<LaboratorioSolicitudDto> listByPaciente(@PathVariable("pacienteId") Long pacienteId,
                                                        @PageableDefault(size = 20) Pageable pageable) {
        return laboratorioSolicitudService.findByPacienteId(pacienteId, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ODONTOLOGO','BACTERIOLOGO','COORDINADOR_MEDICO')")
    public ResponseEntity<LaboratorioSolicitudDto> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(laboratorioSolicitudService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ODONTOLOGO','BACTERIOLOGO','COORDINADOR_MEDICO')")
    public ResponseEntity<LaboratorioSolicitudDto> create(@Valid @RequestBody LaboratorioSolicitudRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(laboratorioSolicitudService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR','MEDICO','ODONTOLOGO','BACTERIOLOGO','COORDINADOR_MEDICO')")
    public ResponseEntity<LaboratorioSolicitudDto> update(@PathVariable("id") Long id, @Valid @RequestBody LaboratorioSolicitudRequestDto dto) {
        return ResponseEntity.ok(laboratorioSolicitudService.update(id, dto));
    }

    @PatchMapping("/{id}/resultado")
    @PreAuthorize("hasAnyRole('BACTERIOLOGO','ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<LaboratorioSolicitudDto> registrarResultado(
            @PathVariable("id") Long id,
            @Valid @RequestBody ResultadoLaboratorioDto dto) {
        return ResponseEntity.ok(laboratorioSolicitudService.registrarResultado(id, dto));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('BACTERIOLOGO','ADMIN','SUPERADMINISTRADOR','MEDICO','ODONTOLOGO','COORDINADOR_MEDICO')")
    public ResponseEntity<LaboratorioSolicitudDto> cambiarEstado(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body) {
        String nuevoEstado = body.get("estado");
        if (nuevoEstado == null || nuevoEstado.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(laboratorioSolicitudService.cambiarEstado(id, nuevoEstado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        laboratorioSolicitudService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
