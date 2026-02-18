/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service.impl;

import com.sesa.salud.dto.*;
import com.sesa.salud.entity.FarmaciaDispensacion;
import com.sesa.salud.entity.FarmaciaMedicamento;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.repository.FarmaciaDispensacionRepository;
import com.sesa.salud.repository.FarmaciaMedicamentoRepository;
import com.sesa.salud.repository.PacienteRepository;
import com.sesa.salud.service.FarmaciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FarmaciaServiceImpl implements FarmaciaService {

    private final FarmaciaMedicamentoRepository medicamentoRepository;
    private final FarmaciaDispensacionRepository dispensacionRepository;
    private final PacienteRepository pacienteRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FarmaciaMedicamentoDto> listMedicamentos(Pageable pageable) {
        return medicamentoRepository.findAll(pageable).stream().map(this::toMedicamentoDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FarmaciaMedicamentoDto createMedicamento(FarmaciaMedicamentoRequestDto dto) {
        FarmaciaMedicamento m = FarmaciaMedicamento.builder()
                .nombre(dto.getNombre())
                .lote(dto.getLote())
                .fechaVencimiento(dto.getFechaVencimiento())
                .cantidad(dto.getCantidad() != null ? dto.getCantidad() : 0)
                .precio(dto.getPrecio())
                .stockMinimo(dto.getStockMinimo() != null ? dto.getStockMinimo() : 0)
                .activo(dto.getActivo() != null ? dto.getActivo() : true)
                .build();
        return toMedicamentoDto(medicamentoRepository.save(m));
    }

    @Override
    @Transactional
    public FarmaciaMedicamentoDto updateMedicamento(Long id, FarmaciaMedicamentoRequestDto dto) {
        FarmaciaMedicamento m = medicamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medicamento no encontrado: " + id));
        if (dto.getNombre() != null) m.setNombre(dto.getNombre());
        if (dto.getLote() != null) m.setLote(dto.getLote());
        if (dto.getFechaVencimiento() != null) m.setFechaVencimiento(dto.getFechaVencimiento());
        if (dto.getCantidad() != null) m.setCantidad(dto.getCantidad());
        if (dto.getPrecio() != null) m.setPrecio(dto.getPrecio());
        if (dto.getStockMinimo() != null) m.setStockMinimo(dto.getStockMinimo());
        if (dto.getActivo() != null) m.setActivo(dto.getActivo());
        return toMedicamentoDto(medicamentoRepository.save(m));
    }

    @Override
    @Transactional
    public void deleteMedicamento(Long id) {
        if (!medicamentoRepository.existsById(id)) {
            throw new RuntimeException("Medicamento no encontrado: " + id);
        }
        medicamentoRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FarmaciaDispensacionDto> listDispensacionesByPaciente(Long pacienteId, Pageable pageable) {
        return dispensacionRepository.findByPaciente_IdOrderByFechaDispensacionDesc(pacienteId, pageable)
                .stream().map(this::toDispensacionDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FarmaciaDispensacionDto dispensar(FarmaciaDispensacionRequestDto dto) {
        FarmaciaMedicamento med = medicamentoRepository.findById(dto.getMedicamentoId())
                .orElseThrow(() -> new RuntimeException("Medicamento no encontrado"));
        Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));
        if (dto.getCantidad() <= 0) throw new RuntimeException("Cantidad inválida");
        if (med.getCantidad() < dto.getCantidad()) throw new RuntimeException("Stock insuficiente");

        med.setCantidad(med.getCantidad() - dto.getCantidad());
        medicamentoRepository.save(med);

        FarmaciaDispensacion d = FarmaciaDispensacion.builder()
                .medicamento(med)
                .paciente(paciente)
                .cantidad(dto.getCantidad())
                .entregadoPor(dto.getEntregadoPor())
                .build();
        return toDispensacionDto(dispensacionRepository.save(d));
    }

    private FarmaciaMedicamentoDto toMedicamentoDto(FarmaciaMedicamento m) {
        return FarmaciaMedicamentoDto.builder()
                .id(m.getId())
                .nombre(m.getNombre())
                .lote(m.getLote())
                .fechaVencimiento(m.getFechaVencimiento())
                .cantidad(m.getCantidad())
                .precio(m.getPrecio())
                .stockMinimo(m.getStockMinimo())
                .activo(m.getActivo())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private FarmaciaDispensacionDto toDispensacionDto(FarmaciaDispensacion d) {
        String pacienteNombre = d.getPaciente().getNombres() + " " +
                (d.getPaciente().getApellidos() != null ? d.getPaciente().getApellidos() : "");
        return FarmaciaDispensacionDto.builder()
                .id(d.getId())
                .medicamentoId(d.getMedicamento().getId())
                .medicamentoNombre(d.getMedicamento().getNombre())
                .pacienteId(d.getPaciente().getId())
                .pacienteNombre(pacienteNombre.trim())
                .cantidad(d.getCantidad())
                .fechaDispensacion(d.getFechaDispensacion())
                .entregadoPor(d.getEntregadoPor())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
