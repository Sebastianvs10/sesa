/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.sesa.salud.dto.DolorDto;
import com.sesa.salud.dto.DolorRequestDto;
import com.sesa.salud.entity.Dolor;
import com.sesa.salud.entity.HistoriaClinica;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.repository.DolorRepository;
import com.sesa.salud.repository.HistoriaClinicaRepository;
import com.sesa.salud.repository.PacienteRepository;
import com.sesa.salud.service.DolorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DolorServiceImpl implements DolorService {

    private final DolorRepository dolorRepository;
    private final PacienteRepository pacienteRepository;
    private final HistoriaClinicaRepository historiaClinicaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DolorDto> findByPacienteId(Long pacienteId) {
        return dolorRepository.findByPaciente_IdOrderByFechaInicioDesc(pacienteId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DolorDto> findByHistoriaClinicaId(Long historiaClinicaId) {
        return dolorRepository.findByHistoriaClinica_IdOrderByFechaInicioDesc(historiaClinicaId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DolorDto findById(Long id) {
        Dolor d = dolorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dolor no encontrado: " + id));
        return toDto(d);
    }

    @Override
    @Transactional
    public DolorDto create(DolorRequestDto dto) {
        Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + dto.getPacienteId()));

        HistoriaClinica hc = null;
        if (dto.getHistoriaClinicaId() != null) {
            hc = historiaClinicaRepository.findById(dto.getHistoriaClinicaId()).orElse(null);
        }

        Instant fechaInicio = dto.getFechaInicio() != null ? Instant.parse(dto.getFechaInicio() + "T00:00:00Z")
                : Instant.now();
        Instant fechaResolucion = dto.getFechaResolucion() != null
                ? Instant.parse(dto.getFechaResolucion() + "T00:00:00Z")
                : null;

        Dolor dolor = Dolor.builder()
                .paciente(paciente)
                .historiaClinica(hc)
                .zonaCorporal(dto.getZonaCorporal())
                .zonaLabel(dto.getZonaLabel())
                .tipoDolor(dto.getTipoDolor())
                .intensidad(dto.getIntensidad())
                .severidad(dto.getSeveridad() != null ? dto.getSeveridad() : "leve")
                .estado(dto.getEstado() != null ? dto.getEstado() : "activo")
                .fechaInicio(fechaInicio)
                .fechaResolucion(fechaResolucion)
                .descripcion(dto.getDescripcion())
                .factoresAgravantes(dto.getFactoresAgravantes())
                .factoresAliviantes(dto.getFactoresAliviantes())
                .tratamiento(dto.getTratamiento())
                .notas(dto.getNotas())
                .vista(dto.getVista() != null ? dto.getVista() : "front")
                .build();

        dolor = dolorRepository.save(dolor);
        return toDto(dolor);
    }

    @Override
    @Transactional
    public DolorDto update(Long id, DolorRequestDto dto) {
        Dolor d = dolorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dolor no encontrado: " + id));

        d.setZonaCorporal(dto.getZonaCorporal());
        d.setZonaLabel(dto.getZonaLabel());
        d.setTipoDolor(dto.getTipoDolor());
        d.setIntensidad(dto.getIntensidad());
        if (dto.getSeveridad() != null)
            d.setSeveridad(dto.getSeveridad());
        if (dto.getEstado() != null)
            d.setEstado(dto.getEstado());
        if (dto.getFechaInicio() != null)
            d.setFechaInicio(Instant.parse(dto.getFechaInicio() + "T00:00:00Z"));
        if (dto.getFechaResolucion() != null) {
            d.setFechaResolucion(Instant.parse(dto.getFechaResolucion() + "T00:00:00Z"));
        }
        d.setDescripcion(dto.getDescripcion());
        d.setFactoresAgravantes(dto.getFactoresAgravantes());
        d.setFactoresAliviantes(dto.getFactoresAliviantes());
        d.setTratamiento(dto.getTratamiento());
        d.setNotas(dto.getNotas());
        if (dto.getVista() != null)
            d.setVista(dto.getVista());

        d = dolorRepository.save(d);
        return toDto(d);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (!dolorRepository.existsById(id)) {
            throw new RuntimeException("Dolor no encontrado: " + id);
        }
        dolorRepository.deleteById(id);
    }

    private DolorDto toDto(Dolor d) {
        String pacienteNombre = d.getPaciente().getNombres() + " "
                + (d.getPaciente().getApellidos() != null ? d.getPaciente().getApellidos() : "");
        return DolorDto.builder()
                .id(d.getId())
                .pacienteId(d.getPaciente().getId())
                .pacienteNombre(pacienteNombre.trim())
                .historiaClinicaId(d.getHistoriaClinica() != null ? d.getHistoriaClinica().getId() : null)
                .zonaCorporal(d.getZonaCorporal())
                .zonaLabel(d.getZonaLabel())
                .tipoDolor(d.getTipoDolor())
                .intensidad(d.getIntensidad())
                .severidad(d.getSeveridad())
                .estado(d.getEstado())
                .fechaInicio(d.getFechaInicio())
                .fechaResolucion(d.getFechaResolucion())
                .descripcion(d.getDescripcion())
                .factoresAgravantes(d.getFactoresAgravantes())
                .factoresAliviantes(d.getFactoresAliviantes())
                .tratamiento(d.getTratamiento())
                .notas(d.getNotas())
                .vista(d.getVista())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
