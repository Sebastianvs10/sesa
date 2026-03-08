/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.sesa.salud.dto.EvolucionDto;
import com.sesa.salud.dto.EvolucionRequestDto;
import com.sesa.salud.entity.Atencion;
import com.sesa.salud.entity.Evolucion;
import com.sesa.salud.entity.Personal;
import com.sesa.salud.repository.AtencionRepository;
import com.sesa.salud.repository.EvolucionRepository;
import com.sesa.salud.repository.PersonalRepository;
import com.sesa.salud.service.EvolucionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EvolucionServiceImpl implements EvolucionService {

    private final EvolucionRepository evolucionRepository;
    private final AtencionRepository atencionRepository;
    private final PersonalRepository personalRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EvolucionDto> findByAtencionId(Long atencionId, Pageable pageable) {
        return evolucionRepository.findByAtencion_IdOrderByFechaDesc(atencionId, pageable)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvolucionDto> findByPacienteId(Long pacienteId, Pageable pageable) {
        return evolucionRepository.findByAtencion_HistoriaClinica_Paciente_IdOrderByFechaDesc(pacienteId, pageable)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EvolucionDto findById(Long id) {
        return toDto(evolucionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evolución no encontrada: " + id)));
    }

    @Override
    @Transactional
    public EvolucionDto create(EvolucionRequestDto dto) {
        Atencion atencion = atencionRepository.findById(dto.getAtencionId())
                .orElseThrow(() -> new RuntimeException("Atención no encontrada: " + dto.getAtencionId()));
        Personal profesional = null;
        if (dto.getProfesionalId() != null) {
            profesional = personalRepository.findById(dto.getProfesionalId())
                    .orElseThrow(() -> new RuntimeException("Profesional no encontrado: " + dto.getProfesionalId()));
        }
        Evolucion evolucion = Evolucion.builder()
                .atencion(atencion)
                .notaEvolucion(dto.getNotaEvolucion())
                .fecha(dto.getFecha() != null ? dto.getFecha() : Instant.now())
                .profesional(profesional)
                .build();
        return toDto(evolucionRepository.save(evolucion));
    }

    @Override
    @Transactional
    public EvolucionDto update(Long id, EvolucionRequestDto dto) {
        Evolucion evolucion = evolucionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evolución no encontrada: " + id));
        evolucion.setNotaEvolucion(dto.getNotaEvolucion());
        if (dto.getFecha() != null) {
            evolucion.setFecha(dto.getFecha());
        }
        if (dto.getProfesionalId() != null) {
            Personal profesional = personalRepository.findById(dto.getProfesionalId())
                    .orElseThrow(() -> new RuntimeException("Profesional no encontrado: " + dto.getProfesionalId()));
            evolucion.setProfesional(profesional);
        }
        return toDto(evolucionRepository.save(evolucion));
    }

    private EvolucionDto toDto(Evolucion e) {
        String nombreProfesional = null;
        Long profesionalId = null;
        if (e.getProfesional() != null) {
            profesionalId = e.getProfesional().getId();
            nombreProfesional = (e.getProfesional().getNombres() + " " +
                    (e.getProfesional().getApellidos() != null ? e.getProfesional().getApellidos() : "")).trim();
        }
        return EvolucionDto.builder()
                .id(e.getId())
                .atencionId(e.getAtencion() != null ? e.getAtencion().getId() : null)
                .notaEvolucion(e.getNotaEvolucion())
                .fecha(e.getFecha())
                .profesionalId(profesionalId)
                .profesionalNombre(nombreProfesional)
                .createdAt(e.getCreatedAt())
                .build();
    }
}
