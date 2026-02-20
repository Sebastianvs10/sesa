/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.sesa.salud.dto.LaboratorioSolicitudDto;
import com.sesa.salud.dto.LaboratorioSolicitudRequestDto;
import com.sesa.salud.dto.ResultadoLaboratorioDto;
import com.sesa.salud.entity.LaboratorioSolicitud;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.entity.Personal;
import com.sesa.salud.repository.LaboratorioSolicitudRepository;
import com.sesa.salud.repository.PacienteRepository;
import com.sesa.salud.repository.PersonalRepository;
import com.sesa.salud.service.LaboratorioSolicitudService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LaboratorioSolicitudServiceImpl implements LaboratorioSolicitudService {

    private final LaboratorioSolicitudRepository laboratorioSolicitudRepository;
    private final PacienteRepository pacienteRepository;
    private final PersonalRepository personalRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<LaboratorioSolicitudDto> findAll(Pageable pageable) {
        return laboratorioSolicitudRepository.findAll(pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LaboratorioSolicitudDto> findByPacienteId(Long pacienteId, Pageable pageable) {
        return laboratorioSolicitudRepository.findByPaciente_Id(pacienteId, pageable).getContent().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LaboratorioSolicitudDto> findByEstado(String estado, Pageable pageable) {
        return laboratorioSolicitudRepository.findByEstado(estado, pageable).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LaboratorioSolicitudDto findById(Long id) {
        LaboratorioSolicitud l = laboratorioSolicitudRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud de laboratorio no encontrada: " + id));
        return toDto(l);
    }

    @Override
    @Transactional
    public LaboratorioSolicitudDto create(LaboratorioSolicitudRequestDto dto) {
        Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + dto.getPacienteId()));
        Personal solicitante = dto.getSolicitanteId() != null
                ? personalRepository.findById(dto.getSolicitanteId()).orElse(null)
                : null;
        LaboratorioSolicitud l = LaboratorioSolicitud.builder()
                .paciente(paciente)
                .solicitante(solicitante)
                .tipoPrueba(dto.getTipoPrueba())
                .estado(dto.getEstado() != null ? dto.getEstado() : "PENDIENTE")
                .fechaSolicitud(LocalDate.now())
                .build();
        l = laboratorioSolicitudRepository.save(l);
        return toDto(l);
    }

    @Override
    @Transactional
    public LaboratorioSolicitudDto update(Long id, LaboratorioSolicitudRequestDto dto) {
        LaboratorioSolicitud l = laboratorioSolicitudRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud de laboratorio no encontrada: " + id));
        l.setTipoPrueba(dto.getTipoPrueba());
        l.setEstado(dto.getEstado());
        if (dto.getSolicitanteId() != null) {
            Personal solicitante = personalRepository.findById(dto.getSolicitanteId()).orElse(null);
            l.setSolicitante(solicitante);
        }
        l = laboratorioSolicitudRepository.save(l);
        return toDto(l);
    }

    @Override
    @Transactional
    public LaboratorioSolicitudDto registrarResultado(Long id, ResultadoLaboratorioDto dto) {
        LaboratorioSolicitud l = laboratorioSolicitudRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud de laboratorio no encontrada: " + id));
        l.setResultado(dto.getResultado());
        l.setObservaciones(dto.getObservaciones());
        l.setFechaResultado(Instant.now());
        l.setEstado("COMPLETADO");
        if (dto.getBacteriologoId() != null) {
            personalRepository.findById(dto.getBacteriologoId()).ifPresent(l::setBacteriologo);
        }
        return toDto(laboratorioSolicitudRepository.save(l));
    }

    @Override
    @Transactional
    public LaboratorioSolicitudDto cambiarEstado(Long id, String nuevoEstado) {
        LaboratorioSolicitud l = laboratorioSolicitudRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud de laboratorio no encontrada: " + id));
        l.setEstado(nuevoEstado);
        return toDto(laboratorioSolicitudRepository.save(l));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (!laboratorioSolicitudRepository.existsById(id)) {
            throw new RuntimeException("Solicitud de laboratorio no encontrada: " + id);
        }
        laboratorioSolicitudRepository.deleteById(id);
    }

    private LaboratorioSolicitudDto toDto(LaboratorioSolicitud l) {
        String pacienteNombre = l.getPaciente().getNombres() + " "
                + (l.getPaciente().getApellidos() != null ? l.getPaciente().getApellidos() : "");
        String solicitanteNombre = l.getSolicitante() != null
                ? l.getSolicitante().getNombres() + " "
                    + (l.getSolicitante().getApellidos() != null ? l.getSolicitante().getApellidos() : "")
                : null;
        String bacteriologoNombre = l.getBacteriologo() != null
                ? l.getBacteriologo().getNombres() + " "
                    + (l.getBacteriologo().getApellidos() != null ? l.getBacteriologo().getApellidos() : "")
                : null;
        return LaboratorioSolicitudDto.builder()
                .id(l.getId())
                .pacienteId(l.getPaciente().getId())
                .pacienteNombre(pacienteNombre.trim())
                .pacienteDocumento(l.getPaciente().getDocumento())
                .solicitanteId(l.getSolicitante() != null ? l.getSolicitante().getId() : null)
                .solicitanteNombre(solicitanteNombre != null ? solicitanteNombre.trim() : null)
                .tipoPrueba(l.getTipoPrueba())
                .estado(l.getEstado())
                .fechaSolicitud(l.getFechaSolicitud())
                .resultado(l.getResultado())
                .observaciones(l.getObservaciones())
                .fechaResultado(l.getFechaResultado())
                .bacteriologoId(l.getBacteriologo() != null ? l.getBacteriologo().getId() : null)
                .bacteriologoNombre(bacteriologoNombre != null ? bacteriologoNombre.trim() : null)
                .createdAt(l.getCreatedAt())
                .build();
    }
}
