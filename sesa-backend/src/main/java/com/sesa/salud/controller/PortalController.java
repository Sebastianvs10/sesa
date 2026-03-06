/**
 * Endpoints del Portal del Paciente (y acceso móvil).
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller;

import com.sesa.salud.dto.ConsentimientoInformadoDto;
import com.sesa.salud.entity.ConsentimientoInformado;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.repository.ConsentimientoInformadoRepository;
import com.sesa.salud.repository.PacienteRepository;
import com.sesa.salud.security.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/portal")
@RequiredArgsConstructor
public class PortalController {

    private final PacienteRepository pacienteRepository;
    private final ConsentimientoInformadoRepository consentimientoRepository;

    /**
     * Consentimientos pendientes de firma del paciente actual (portal/móvil).
     * El usuario debe estar vinculado a un paciente (paciente.usuarioId).
     */
    @GetMapping("/consentimientos-pendientes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ConsentimientoInformadoDto>> consentimientosPendientes(Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        Paciente paciente = pacienteRepository.findByUsuarioId(principal.userId())
                .orElse(null);
        if (paciente == null) {
            return ResponseEntity.ok(List.of());
        }
        List<ConsentimientoInformado> list = consentimientoRepository
                .findByPaciente_IdAndEstadoOrderByCreatedAtDesc(paciente.getId(), "PENDIENTE");
        List<ConsentimientoInformadoDto> dtos = list.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private ConsentimientoInformadoDto toDto(ConsentimientoInformado c) {
        var p = c.getPaciente();
        var prof = c.getProfesional();
        return ConsentimientoInformadoDto.builder()
                .id(c.getId())
                .pacienteId(p.getId())
                .pacienteNombre((p.getNombres() + " " + (p.getApellidos() != null ? p.getApellidos() : "")).trim())
                .pacienteDocumento(p.getDocumento())
                .profesionalId(prof.getId())
                .profesionalNombre((prof.getNombres() + " " + (prof.getApellidos() != null ? prof.getApellidos() : "")).trim())
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
