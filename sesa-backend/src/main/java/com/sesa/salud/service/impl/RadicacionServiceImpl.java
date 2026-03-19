/**
 * Implementación del servicio de radicación de facturas.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service.impl;

import com.sesa.salud.dto.RadicacionDto;
import com.sesa.salud.dto.RadicacionRequestDto;
import com.sesa.salud.entity.Factura;
import com.sesa.salud.entity.Radicacion;
import com.sesa.salud.repository.FacturaRepository;
import com.sesa.salud.repository.RadicacionRepository;
import com.sesa.salud.service.RadicacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RadicacionServiceImpl implements RadicacionService {

    private final RadicacionRepository radicacionRepository;
    private final FacturaRepository facturaRepository;

    @Override
    @Transactional
    public RadicacionDto create(RadicacionRequestDto dto) {
        Factura factura = facturaRepository.findById(dto.getFacturaId())
                .orElseThrow(() -> new RuntimeException("Factura no encontrada: " + dto.getFacturaId()));
        String epsCodigo = dto.getEpsCodigo();
        String epsNombre = dto.getEpsNombre();
        if ((epsCodigo == null || epsCodigo.isBlank()) && factura.getPaciente() != null && factura.getPaciente().getEps() != null) {
            epsCodigo = factura.getPaciente().getEps().getCodigo();
            epsNombre = factura.getPaciente().getEps().getNombre();
        }
        Radicacion r = Radicacion.builder()
                .factura(factura)
                .fechaRadicacion(dto.getFechaRadicacion() != null ? dto.getFechaRadicacion() : java.time.Instant.now())
                .numeroRadicado(dto.getNumeroRadicado())
                .epsCodigo(epsCodigo)
                .epsNombre(epsNombre)
                .estado(dto.getEstado() != null ? dto.getEstado() : "RADICADA")
                .cuv(dto.getCuv())
                .observaciones(dto.getObservaciones())
                .build();
        return toDto(radicacionRepository.save(r));
    }

    @Override
    @Transactional
    public RadicacionDto update(Long id, RadicacionRequestDto dto) {
        Radicacion r = radicacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Radicación no encontrada: " + id));
        if (dto.getFechaRadicacion() != null) r.setFechaRadicacion(dto.getFechaRadicacion());
        if (dto.getNumeroRadicado() != null) r.setNumeroRadicado(dto.getNumeroRadicado());
        if (dto.getEpsCodigo() != null) r.setEpsCodigo(dto.getEpsCodigo());
        if (dto.getEpsNombre() != null) r.setEpsNombre(dto.getEpsNombre());
        if (dto.getEstado() != null) r.setEstado(dto.getEstado());
        if (dto.getCuv() != null) r.setCuv(dto.getCuv());
        if (dto.getObservaciones() != null) r.setObservaciones(dto.getObservaciones());
        return toDto(radicacionRepository.save(r));
    }

    @Override
    @Transactional(readOnly = true)
    public RadicacionDto findById(Long id) {
        return toDto(radicacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Radicación no encontrada: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RadicacionDto> findByFacturaId(Long facturaId) {
        return radicacionRepository.findByFactura_IdOrderByFechaRadicacionDesc(facturaId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RadicacionDto> findAllFiltered(String estado, java.time.Instant desde, java.time.Instant hasta, Long facturaId, Pageable pageable) {
        return radicacionRepository.findAllFiltered(estado, desde, hasta, facturaId, pageable).map(this::toDto);
    }

    private RadicacionDto toDto(Radicacion r) {
        return RadicacionDto.builder()
                .id(r.getId())
                .facturaId(r.getFactura().getId())
                .numeroFactura(r.getFactura().getNumeroFactura())
                .fechaRadicacion(r.getFechaRadicacion())
                .numeroRadicado(r.getNumeroRadicado())
                .epsCodigo(r.getEpsCodigo())
                .epsNombre(r.getEpsNombre())
                .estado(r.getEstado())
                .cuv(r.getCuv())
                .observaciones(r.getObservaciones())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
