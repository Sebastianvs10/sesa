/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.sesa.salud.dto.UrgenciaRegistroDto;
import com.sesa.salud.dto.UrgenciaRegistroRequestDto;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.entity.UrgenciaRegistro;
import com.sesa.salud.repository.PacienteRepository;
import com.sesa.salud.repository.UrgenciaRegistroRepository;
import com.sesa.salud.service.UrgenciaRegistroService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UrgenciaRegistroServiceImpl implements UrgenciaRegistroService {

    private final UrgenciaRegistroRepository urgenciaRegistroRepository;
    private final PacienteRepository pacienteRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<UrgenciaRegistroDto> findAll(Pageable pageable) {
        return urgenciaRegistroRepository.findAll(pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UrgenciaRegistroDto> findByEstado(String estado, Pageable pageable) {
        return urgenciaRegistroRepository.findByEstado(estado, pageable).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UrgenciaRegistroDto findById(Long id) {
        UrgenciaRegistro u = urgenciaRegistroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro de urgencia no encontrado: " + id));
        return toDto(u);
    }

    @Override
    @Transactional
    public UrgenciaRegistroDto create(UrgenciaRegistroRequestDto dto) {
        Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + dto.getPacienteId()));
        UrgenciaRegistro u = UrgenciaRegistro.builder()
                .paciente(paciente)
                .nivelTriage(dto.getNivelTriage())
                .estado(dto.getEstado() != null ? dto.getEstado() : "EN_ESPERA")
                .fechaHoraIngreso(LocalDateTime.now())
                .observaciones(dto.getObservaciones())
                .build();
        u = urgenciaRegistroRepository.save(u);
        return toDto(u);
    }

    @Override
    @Transactional
    public UrgenciaRegistroDto update(Long id, UrgenciaRegistroRequestDto dto) {
        UrgenciaRegistro u = urgenciaRegistroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro de urgencia no encontrado: " + id));
        u.setNivelTriage(dto.getNivelTriage());
        u.setEstado(dto.getEstado());
        u.setObservaciones(dto.getObservaciones());
        u = urgenciaRegistroRepository.save(u);
        return toDto(u);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (!urgenciaRegistroRepository.existsById(id)) {
            throw new RuntimeException("Registro de urgencia no encontrado: " + id);
        }
        urgenciaRegistroRepository.deleteById(id);
    }

    private UrgenciaRegistroDto toDto(UrgenciaRegistro u) {
        String pacienteNombre = u.getPaciente().getNombres() + " " + (u.getPaciente().getApellidos() != null ? u.getPaciente().getApellidos() : "");
        return UrgenciaRegistroDto.builder()
                .id(u.getId())
                .pacienteId(u.getPaciente().getId())
                .pacienteNombre(pacienteNombre.trim())
                .nivelTriage(u.getNivelTriage())
                .estado(u.getEstado())
                .fechaHoraIngreso(u.getFechaHoraIngreso())
                .observaciones(u.getObservaciones())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
