/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service.impl;

import com.sesa.salud.dto.HospitalizacionDto;
import com.sesa.salud.dto.HospitalizacionRequestDto;
import com.sesa.salud.entity.Hospitalizacion;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.repository.HospitalizacionRepository;
import com.sesa.salud.repository.PacienteRepository;
import com.sesa.salud.service.HospitalizacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HospitalizacionServiceImpl implements HospitalizacionService {

    private final HospitalizacionRepository hospitalizacionRepository;
    private final PacienteRepository pacienteRepository;

    @Override
    @Transactional(readOnly = true)
    public List<HospitalizacionDto> findByPacienteId(Long pacienteId, Pageable pageable) {
        return hospitalizacionRepository.findByPaciente_IdOrderByFechaIngresoDesc(pacienteId, pageable)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HospitalizacionDto> findByEstado(String estado, Pageable pageable) {
        return hospitalizacionRepository.findByEstadoOrderByFechaIngresoDesc(estado, pageable)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public HospitalizacionDto findById(Long id) {
        return toDto(hospitalizacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hospitalización no encontrada: " + id)));
    }

    @Override
    @Transactional
    public HospitalizacionDto create(HospitalizacionRequestDto dto) {
        Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + dto.getPacienteId()));
        Hospitalizacion h = Hospitalizacion.builder()
                .paciente(paciente)
                .servicio(dto.getServicio())
                .cama(dto.getCama())
                .estado(dto.getEstado() != null ? dto.getEstado() : "INGRESADO")
                .evolucionDiaria(dto.getEvolucionDiaria())
                .ordenesMedicas(dto.getOrdenesMedicas())
                .epicrisis(dto.getEpicrisis())
                .build();
        if ("EGRESADO".equalsIgnoreCase(h.getEstado())) {
            h.setFechaEgreso(LocalDateTime.now());
        }
        return toDto(hospitalizacionRepository.save(h));
    }

    @Override
    @Transactional
    public HospitalizacionDto update(Long id, HospitalizacionRequestDto dto) {
        Hospitalizacion h = hospitalizacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hospitalización no encontrada: " + id));
        if (dto.getServicio() != null) h.setServicio(dto.getServicio());
        if (dto.getCama() != null) h.setCama(dto.getCama());
        if (dto.getEstado() != null) {
            h.setEstado(dto.getEstado());
            if ("EGRESADO".equalsIgnoreCase(dto.getEstado()) && h.getFechaEgreso() == null) {
                h.setFechaEgreso(LocalDateTime.now());
            }
        }
        if (dto.getEvolucionDiaria() != null) h.setEvolucionDiaria(dto.getEvolucionDiaria());
        if (dto.getOrdenesMedicas() != null) h.setOrdenesMedicas(dto.getOrdenesMedicas());
        if (dto.getEpicrisis() != null) h.setEpicrisis(dto.getEpicrisis());
        return toDto(hospitalizacionRepository.save(h));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (!hospitalizacionRepository.existsById(id)) {
            throw new RuntimeException("Hospitalización no encontrada: " + id);
        }
        hospitalizacionRepository.deleteById(id);
    }

    private HospitalizacionDto toDto(Hospitalizacion h) {
        String pacienteNombre = h.getPaciente().getNombres() + " " +
                (h.getPaciente().getApellidos() != null ? h.getPaciente().getApellidos() : "");
        return HospitalizacionDto.builder()
                .id(h.getId())
                .pacienteId(h.getPaciente().getId())
                .pacienteNombre(pacienteNombre.trim())
                .servicio(h.getServicio())
                .cama(h.getCama())
                .estado(h.getEstado())
                .fechaIngreso(h.getFechaIngreso())
                .fechaEgreso(h.getFechaEgreso())
                .evolucionDiaria(h.getEvolucionDiaria())
                .ordenesMedicas(h.getOrdenesMedicas())
                .epicrisis(h.getEpicrisis())
                .createdAt(h.getCreatedAt())
                .build();
    }
}
