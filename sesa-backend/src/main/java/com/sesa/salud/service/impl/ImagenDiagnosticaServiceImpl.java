/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service.impl;

import com.sesa.salud.dto.ImagenDiagnosticaDto;
import com.sesa.salud.dto.ImagenDiagnosticaRequestDto;
import com.sesa.salud.entity.Atencion;
import com.sesa.salud.entity.ImagenDiagnostica;
import com.sesa.salud.repository.AtencionRepository;
import com.sesa.salud.repository.ImagenDiagnosticaRepository;
import com.sesa.salud.service.ImagenDiagnosticaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
}
