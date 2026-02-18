/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service.impl;

import com.sesa.salud.dto.FacturaDto;
import com.sesa.salud.dto.FacturaRequestDto;
import com.sesa.salud.entity.Factura;
import com.sesa.salud.entity.OrdenClinica;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.repository.FacturaRepository;
import com.sesa.salud.repository.OrdenClinicaRepository;
import com.sesa.salud.repository.PacienteRepository;
import com.sesa.salud.service.FacturaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FacturaServiceImpl implements FacturaService {

    private final FacturaRepository facturaRepository;
    private final PacienteRepository pacienteRepository;
    private final OrdenClinicaRepository ordenClinicaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FacturaDto> findByPacienteId(Long pacienteId, Pageable pageable) {
        return facturaRepository.findByPaciente_IdOrderByFechaFacturaDesc(pacienteId, pageable)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FacturaDto findById(Long id) {
        return toDto(facturaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada: " + id)));
    }

    @Override
    @Transactional
    public FacturaDto create(FacturaRequestDto dto) {
        Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + dto.getPacienteId()));
        OrdenClinica orden = dto.getOrdenId() != null
                ? ordenClinicaRepository.findById(dto.getOrdenId()).orElse(null)
                : null;

        Factura factura = Factura.builder()
                .paciente(paciente)
                .orden(orden)
                .valorTotal(dto.getValorTotal())
                .estado(dto.getEstado() != null ? dto.getEstado() : "PENDIENTE")
                .descripcion(dto.getDescripcion())
                .fechaFactura(Instant.now())
                .build();
        return toDto(facturaRepository.save(factura));
    }

    @Override
    @Transactional
    public FacturaDto update(Long id, FacturaRequestDto dto) {
        Factura factura = facturaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada: " + id));
        if (dto.getValorTotal() != null) factura.setValorTotal(dto.getValorTotal());
        if (dto.getEstado() != null) factura.setEstado(dto.getEstado());
        if (dto.getDescripcion() != null) factura.setDescripcion(dto.getDescripcion());
        return toDto(facturaRepository.save(factura));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (!facturaRepository.existsById(id)) {
            throw new RuntimeException("Factura no encontrada: " + id);
        }
        facturaRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public String exportRipsCsv(Instant desde, Instant hasta) {
        List<Factura> facturas = facturaRepository.findByFechaFacturaBetween(desde, hasta);
        StringBuilder sb = new StringBuilder();
        sb.append("factura_id,paciente_id,paciente,orden_id,fecha,valor,estado\n");
        for (Factura f : facturas) {
            String paciente = (f.getPaciente().getNombres() + " " +
                    (f.getPaciente().getApellidos() != null ? f.getPaciente().getApellidos() : "")).trim()
                    .replace(",", " ");
            sb.append(f.getId()).append(",")
                    .append(f.getPaciente().getId()).append(",")
                    .append(paciente).append(",")
                    .append(f.getOrden() != null ? f.getOrden().getId() : "").append(",")
                    .append(f.getFechaFactura()).append(",")
                    .append(f.getValorTotal()).append(",")
                    .append(f.getEstado())
                    .append("\n");
        }
        return sb.toString();
    }

    private FacturaDto toDto(Factura f) {
        String pacienteNombre = f.getPaciente().getNombres() + " " +
                (f.getPaciente().getApellidos() != null ? f.getPaciente().getApellidos() : "");
        return FacturaDto.builder()
                .id(f.getId())
                .pacienteId(f.getPaciente().getId())
                .pacienteNombre(pacienteNombre.trim())
                .ordenId(f.getOrden() != null ? f.getOrden().getId() : null)
                .valorTotal(f.getValorTotal())
                .estado(f.getEstado())
                .descripcion(f.getDescripcion())
                .fechaFactura(f.getFechaFactura())
                .createdAt(f.getCreatedAt())
                .build();
    }
}
