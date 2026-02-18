/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service.impl;

import com.sesa.salud.dto.OrdenClinicaDto;
import com.sesa.salud.dto.OrdenClinicaRequestDto;
import com.sesa.salud.entity.Consulta;
import com.sesa.salud.entity.OrdenClinica;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.repository.ConsultaRepository;
import com.sesa.salud.repository.OrdenClinicaRepository;
import com.sesa.salud.repository.PacienteRepository;
import com.sesa.salud.service.OrdenClinicaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrdenClinicaServiceImpl implements OrdenClinicaService {

    private final OrdenClinicaRepository ordenClinicaRepository;
    private final PacienteRepository pacienteRepository;
    private final ConsultaRepository consultaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<OrdenClinicaDto> findByPacienteId(Long pacienteId, Pageable pageable) {
        return ordenClinicaRepository.findByPaciente_IdOrderByCreatedAtDesc(pacienteId, pageable)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OrdenClinicaDto findById(Long id) {
        return toDto(ordenClinicaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden clínica no encontrada: " + id)));
    }

    @Override
    @Transactional
    public OrdenClinicaDto create(OrdenClinicaRequestDto dto) {
        Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + dto.getPacienteId()));
        Consulta consulta = consultaRepository.findById(dto.getConsultaId())
                .orElseThrow(() -> new RuntimeException("Consulta no encontrada: " + dto.getConsultaId()));

        OrdenClinica orden = OrdenClinica.builder()
                .paciente(paciente)
                .consulta(consulta)
                .tipo(dto.getTipo())
                .detalle(dto.getDetalle())
                .estado(dto.getEstado() != null ? dto.getEstado() : "PENDIENTE")
                .valorEstimado(dto.getValorEstimado())
                .build();
        return toDto(ordenClinicaRepository.save(orden));
    }

    @Override
    @Transactional
    public OrdenClinicaDto update(Long id, OrdenClinicaRequestDto dto) {
        OrdenClinica orden = ordenClinicaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden clínica no encontrada: " + id));
        if (dto.getTipo() != null) orden.setTipo(dto.getTipo());
        if (dto.getDetalle() != null) orden.setDetalle(dto.getDetalle());
        if (dto.getEstado() != null) orden.setEstado(dto.getEstado());
        if (dto.getValorEstimado() != null) orden.setValorEstimado(dto.getValorEstimado());
        return toDto(ordenClinicaRepository.save(orden));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (!ordenClinicaRepository.existsById(id)) {
            throw new RuntimeException("Orden clínica no encontrada: " + id);
        }
        ordenClinicaRepository.deleteById(id);
    }

    private OrdenClinicaDto toDto(OrdenClinica o) {
        String pacienteNombre = o.getPaciente().getNombres() + " " +
                (o.getPaciente().getApellidos() != null ? o.getPaciente().getApellidos() : "");
        return OrdenClinicaDto.builder()
                .id(o.getId())
                .pacienteId(o.getPaciente().getId())
                .pacienteNombre(pacienteNombre.trim())
                .consultaId(o.getConsulta().getId())
                .tipo(o.getTipo())
                .detalle(o.getDetalle())
                .estado(o.getEstado())
                .valorEstimado(o.getValorEstimado())
                .createdAt(o.getCreatedAt())
                .build();
    }
}
