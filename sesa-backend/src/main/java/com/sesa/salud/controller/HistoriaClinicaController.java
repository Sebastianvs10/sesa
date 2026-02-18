/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.controller;

import com.sesa.salud.dto.CrearHistoriaCompletaRequestDto;
import com.sesa.salud.dto.HistoriaClinicaDto;
import com.sesa.salud.dto.HistoriaClinicaRequestDto;
import com.sesa.salud.service.HistoriaClinicaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/historia-clinica")
@RequiredArgsConstructor
public class HistoriaClinicaController {

    private final HistoriaClinicaService historiaClinicaService;

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<HistoriaClinicaDto> getByPaciente(@PathVariable("pacienteId") Long pacienteId) {
        return historiaClinicaService.findByPacienteId(pacienteId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<HistoriaClinicaDto> create(
            @PathVariable("pacienteId") Long pacienteId,
            @Valid @RequestBody HistoriaClinicaRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(historiaClinicaService.createForPaciente(pacienteId, dto));
    }

    @PostMapping("/paciente/{pacienteId}/completa")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<HistoriaClinicaDto> createCompleta(
            @PathVariable("pacienteId") Long pacienteId,
            @Valid @RequestBody CrearHistoriaCompletaRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(historiaClinicaService.createCompleta(pacienteId, dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<HistoriaClinicaDto> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody HistoriaClinicaRequestDto dto) {
        return ResponseEntity.ok(historiaClinicaService.update(id, dto));
    }
}
