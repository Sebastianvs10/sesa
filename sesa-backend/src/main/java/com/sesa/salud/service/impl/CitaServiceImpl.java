/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.sesa.salud.dto.CitaDto;
import com.sesa.salud.dto.CitaRequestDto;
import com.sesa.salud.entity.Cita;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.entity.Personal;
import com.sesa.salud.repository.CitaRepository;
import com.sesa.salud.repository.PacienteRepository;
import com.sesa.salud.repository.PersonalRepository;
import com.sesa.salud.service.CitaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CitaServiceImpl implements CitaService {

    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final PersonalRepository personalRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<CitaDto> findAll(Pageable pageable) {
        return citaRepository.findAll(pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitaDto> findByFecha(LocalDate fecha) {
        return citaRepository.findByFecha(fecha).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitaDto> findByPacienteId(Long pacienteId, Pageable pageable) {
        return citaRepository.findByPaciente_Id(pacienteId, pageable).getContent().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CitaDto findById(Long id) {
        Cita c = citaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada: " + id));
        return toDto(c);
    }

    @Override
    @Transactional
    public CitaDto create(CitaRequestDto dto) {
        Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + dto.getPacienteId()));
        Personal profesional = personalRepository.findById(dto.getProfesionalId())
                .orElseThrow(() -> new RuntimeException("Personal no encontrado: " + dto.getProfesionalId()));
        Cita c = Cita.builder()
                .paciente(paciente)
                .profesional(profesional)
                .servicio(dto.getServicio())
                .fechaHora(dto.getFechaHora())
                .estado(dto.getEstado() != null ? dto.getEstado() : "AGENDADA")
                .notas(dto.getNotas())
                .build();
        c = citaRepository.save(c);
        return toDto(c);
    }

    @Override
    @Transactional
    public CitaDto update(Long id, CitaRequestDto dto) {
        Cita c = citaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada: " + id));
        Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + dto.getPacienteId()));
        Personal profesional = personalRepository.findById(dto.getProfesionalId())
                .orElseThrow(() -> new RuntimeException("Personal no encontrado: " + dto.getProfesionalId()));
        c.setPaciente(paciente);
        c.setProfesional(profesional);
        c.setServicio(dto.getServicio());
        c.setFechaHora(dto.getFechaHora());
        c.setEstado(dto.getEstado());
        c.setNotas(dto.getNotas());
        c = citaRepository.save(c);
        return toDto(c);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (!citaRepository.existsById(id)) {
            throw new RuntimeException("Cita no encontrada: " + id);
        }
        citaRepository.deleteById(id);
    }

    private CitaDto toDto(Cita c) {
        String pacienteNombre = c.getPaciente().getNombres() + " " + (c.getPaciente().getApellidos() != null ? c.getPaciente().getApellidos() : "");
        String profesionalNombre = c.getProfesional().getNombres() + " " + (c.getProfesional().getApellidos() != null ? c.getProfesional().getApellidos() : "");
        return CitaDto.builder()
                .id(c.getId())
                .pacienteId(c.getPaciente().getId())
                .pacienteNombre(pacienteNombre.trim())
                .profesionalId(c.getProfesional().getId())
                .profesionalNombre(profesionalNombre.trim())
                .servicio(c.getServicio())
                .fechaHora(c.getFechaHora())
                .estado(c.getEstado())
                .notas(c.getNotas())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
