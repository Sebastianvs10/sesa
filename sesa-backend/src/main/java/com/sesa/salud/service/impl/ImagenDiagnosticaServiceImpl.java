/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service.impl;

import com.sesa.salud.dto.ImagenDiagnosticaDto;
import com.sesa.salud.dto.ImagenDiagnosticaRequestDto;
import com.sesa.salud.entity.Atencion;
import com.sesa.salud.entity.ImagenDiagnostica;
import com.sesa.salud.entity.Paciente;
import com.sesa.salud.repository.AtencionRepository;
import com.sesa.salud.repository.ImagenDiagnosticaRepository;
import com.sesa.salud.service.ImagenDiagnosticaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImagenDiagnosticaServiceImpl implements ImagenDiagnosticaService {

    private final ImagenDiagnosticaRepository imagenRepository;
    private final AtencionRepository atencionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ImagenDiagnosticaDto> findByAtencionId(Long atencionId, Pageable pageable) {
        return imagenRepository.findByAtencion_IdOrderByCreatedAtDesc(atencionId, pageable)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ImagenDiagnosticaDto> findGlobal(Long pacienteId, Long atencionId, String tipo, Instant fechaDesde, Instant fechaHasta, Pageable pageable) {
        Specification<ImagenDiagnostica> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (pacienteId != null) {
                predicates.add(cb.equal(root.get("atencion").get("historiaClinica").get("paciente").get("id"), pacienteId));
            }
            if (atencionId != null) {
                predicates.add(cb.equal(root.get("atencion").get("id"), atencionId));
            }
            if (tipo != null && !tipo.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("tipo")), "%" + tipo.toLowerCase() + "%"));
            }
            if (fechaDesde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fechaDesde));
            }
            if (fechaHasta != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), fechaHasta));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<ImagenDiagnostica> page = imagenRepository.findAll(spec, pageable);
        List<ImagenDiagnostica> content = page.getContent();
        if (content.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0L);
        }
        List<Long> ids = content.stream().map(ImagenDiagnostica::getId).toList();
        List<ImagenDiagnostica> withFetch = imagenRepository.findAllByIdWithAtencionPaciente(ids);
        Map<Long, ImagenDiagnostica> byId = withFetch.stream().collect(Collectors.toMap(ImagenDiagnostica::getId, Function.identity()));
        List<ImagenDiagnosticaDto> dtos = content.stream()
                .map(i -> toDtoGlobal(byId.get(i.getId())))
                .toList();
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public ImagenDiagnosticaDto findById(Long id) {
        return toDto(imagenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Imagen diagnóstica no encontrada: " + id)));
    }

    @Override
    @Transactional
    public ImagenDiagnosticaDto create(ImagenDiagnosticaRequestDto dto) {
        Atencion atencion = atencionRepository.findById(dto.getAtencionId())
                .orElseThrow(() -> new RuntimeException("Atención no encontrada: " + dto.getAtencionId()));
        ImagenDiagnostica i = ImagenDiagnostica.builder()
                .atencion(atencion)
                .tipo(dto.getTipo())
                .resultado(dto.getResultado())
                .urlArchivo(dto.getUrlArchivo())
                .build();
        return toDto(imagenRepository.save(i));
    }

    @Override
    @Transactional
    public ImagenDiagnosticaDto update(Long id, ImagenDiagnosticaRequestDto dto) {
        ImagenDiagnostica i = imagenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Imagen diagnóstica no encontrada: " + id));
        if (dto.getTipo() != null) i.setTipo(dto.getTipo());
        if (dto.getResultado() != null) i.setResultado(dto.getResultado());
        if (dto.getUrlArchivo() != null) i.setUrlArchivo(dto.getUrlArchivo());
        return toDto(imagenRepository.save(i));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (!imagenRepository.existsById(id)) {
            throw new RuntimeException("Imagen diagnóstica no encontrada: " + id);
        }
        imagenRepository.deleteById(id);
    }

    private ImagenDiagnosticaDto toDto(ImagenDiagnostica i) {
        return ImagenDiagnosticaDto.builder()
                .id(i.getId())
                .atencionId(i.getAtencion().getId())
                .tipo(i.getTipo())
                .resultado(i.getResultado())
                .urlArchivo(i.getUrlArchivo())
                .createdAt(i.getCreatedAt())
                .build();
    }

    private ImagenDiagnosticaDto toDtoGlobal(ImagenDiagnostica i) {
        Atencion a = i.getAtencion();
        Paciente p = a.getHistoriaClinica().getPaciente();
        String nombres = (p.getNombres() != null ? p.getNombres() : "") + " " + (p.getApellidos() != null ? p.getApellidos() : "").trim();
        return ImagenDiagnosticaDto.builder()
                .id(i.getId())
                .atencionId(a.getId())
                .pacienteId(p.getId())
                .pacienteNombres(nombres.isEmpty() ? null : nombres.trim())
                .fechaAtencion(a.getFechaAtencion())
                .tipo(i.getTipo())
                .resultado(i.getResultado())
                .urlArchivo(i.getUrlArchivo())
                .createdAt(i.getCreatedAt())
                .build();
    }
}
