/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller;

import com.sesa.salud.dto.ConsentimientoInformadoDto;
import com.sesa.salud.entity.ConsentimientoInformado;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.entity.Personal;
import com.sesa.salud.repository.ConsentimientoInformadoRepository;
import com.sesa.salud.repository.PacienteRepository;
import com.sesa.salud.repository.PersonalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/consentimientos")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsentimientoInformadoController {

    private final ConsentimientoInformadoRepository repo;
    private final PacienteRepository pacienteRepo;
    private final PersonalRepository personalRepo;

    @GetMapping("/paciente/{pacienteId}")
    public List<ConsentimientoInformadoDto> listByPaciente(@PathVariable Long pacienteId) {
        return repo.findByPaciente_IdOrderByCreatedAtDesc(pacienteId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @PostMapping
    @Transactional(readOnly = false)
    public ResponseEntity<ConsentimientoInformadoDto> create(@RequestBody ConsentimientoInformadoDto dto) {
        if (dto.getPacienteId() == null) {
            throw new IllegalArgumentException("pacienteId es obligatorio");
        }
        if (dto.getProfesionalId() == null) {
            throw new IllegalArgumentException("El usuario debe tener un profesional vinculado (personalId). Asigne el usuario a un registro de personal.");
        }
        Paciente p = pacienteRepo.findById(dto.getPacienteId())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));
        Personal prof = personalRepo.findById(dto.getProfesionalId())
                .orElseThrow(() -> new RuntimeException("Profesional no encontrado"));
        ConsentimientoInformado c = ConsentimientoInformado.builder()
                .paciente(p).profesional(prof)
                .tipo(dto.getTipo())
                .estado(dto.getEstado() != null ? dto.getEstado() : "PENDIENTE")
                .procedimiento(dto.getProcedimiento())
                .observaciones(dto.getObservaciones())
                .firmaCanvasData(dto.getFirmaCanvasData())
                .build();
        return ResponseEntity.ok(toDto(repo.save(c)));
    }

    @PatchMapping("/{id}/firmar")
    @Transactional(readOnly = false)
    public ResponseEntity<ConsentimientoInformadoDto> firmar(
            @PathVariable Long id,
            @RequestBody ConsentimientoInformadoDto dto) {
        ConsentimientoInformado c = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Consentimiento no encontrado: " + id));
        c.setEstado("FIRMADO");
        c.setFechaFirma(Instant.now());
        if (dto.getFirmaCanvasData() != null) c.setFirmaCanvasData(dto.getFirmaCanvasData());
        if (dto.getObservaciones() != null) c.setObservaciones(dto.getObservaciones());
        return ResponseEntity.ok(toDto(repo.save(c)));
    }

    @PatchMapping("/{id}/rechazar")
    @Transactional(readOnly = false)
    public ResponseEntity<ConsentimientoInformadoDto> rechazar(
            @PathVariable Long id,
            @RequestBody(required = false) ConsentimientoInformadoDto dto) {
        ConsentimientoInformado c = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Consentimiento no encontrado: " + id));
        c.setEstado("RECHAZADO");
        if (dto != null && dto.getObservaciones() != null) c.setObservaciones(dto.getObservaciones());
        return ResponseEntity.ok(toDto(repo.save(c)));
    }

    @DeleteMapping("/{id}")
    @Transactional(readOnly = false)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ConsentimientoInformadoDto toDto(ConsentimientoInformado c) {
        Paciente p = c.getPaciente();
        Personal prof = c.getProfesional();
        return ConsentimientoInformadoDto.builder()
                .id(c.getId())
                .pacienteId(p != null ? p.getId() : null)
                .pacienteNombre(p != null ? (p.getNombres() + " " + (p.getApellidos() != null ? p.getApellidos() : "")).trim() : null)
                .pacienteDocumento(p != null ? p.getDocumento() : null)
                .profesionalId(prof != null ? prof.getId() : null)
                .profesionalNombre(prof != null ? (prof.getNombres() + " " + (prof.getApellidos() != null ? prof.getApellidos() : "")).trim() : null)
                .tipo(c.getTipo())
                .estado(c.getEstado())
                .procedimiento(c.getProcedimiento())
                .fechaSolicitud(c.getFechaSolicitud())
                .fechaFirma(c.getFechaFirma())
                .observaciones(c.getObservaciones())
                .firmaCanvasData(c.getFirmaCanvasData())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
