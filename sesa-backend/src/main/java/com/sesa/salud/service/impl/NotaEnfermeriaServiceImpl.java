/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.sesa.salud.dto.NotaEnfermeriaDto;
import com.sesa.salud.dto.NotaEnfermeriaRequestDto;
import com.sesa.salud.entity.Atencion;
import com.sesa.salud.entity.NotaEnfermeria;
import com.sesa.salud.entity.Personal;
import com.sesa.salud.repository.AtencionRepository;
import com.sesa.salud.repository.NotaEnfermeriaRepository;
import com.sesa.salud.repository.PersonalRepository;
import com.sesa.salud.service.NotaEnfermeriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotaEnfermeriaServiceImpl implements NotaEnfermeriaService {

    private final NotaEnfermeriaRepository notaEnfermeriaRepository;
    private final AtencionRepository atencionRepository;
    private final PersonalRepository personalRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NotaEnfermeriaDto> findByAtencionId(Long atencionId, Pageable pageable) {
        return notaEnfermeriaRepository.findByAtencion_IdOrderByFechaNotaDesc(atencionId, pageable)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public NotaEnfermeriaDto findById(Long id) {
        return toDto(notaEnfermeriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nota de enfermería no encontrada: " + id)));
    }

    @Override
    @Transactional
    public NotaEnfermeriaDto create(NotaEnfermeriaRequestDto dto) {
        Atencion atencion = atencionRepository.findById(dto.getAtencionId())
                .orElseThrow(() -> new RuntimeException("Atención no encontrada: " + dto.getAtencionId()));
        Personal profesional = null;
        if (dto.getProfesionalId() != null) {
            profesional = personalRepository.findById(dto.getProfesionalId())
                    .orElseThrow(() -> new RuntimeException("Profesional no encontrado: " + dto.getProfesionalId()));
        }
        NotaEnfermeria nota = NotaEnfermeria.builder()
                .atencion(atencion)
                .nota(dto.getNota())
                .fechaNota(dto.getFechaNota() != null ? dto.getFechaNota() : Instant.now())
                .profesional(profesional)
                .build();
        return toDto(notaEnfermeriaRepository.save(nota));
    }

    @Override
    @Transactional
    public NotaEnfermeriaDto update(Long id, NotaEnfermeriaRequestDto dto) {
        NotaEnfermeria nota = notaEnfermeriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nota de enfermería no encontrada: " + id));
        nota.setNota(dto.getNota());
        if (dto.getFechaNota() != null) {
            nota.setFechaNota(dto.getFechaNota());
        }
        if (dto.getProfesionalId() != null) {
            Personal profesional = personalRepository.findById(dto.getProfesionalId())
                    .orElseThrow(() -> new RuntimeException("Profesional no encontrado: " + dto.getProfesionalId()));
            nota.setProfesional(profesional);
        }
        return toDto(notaEnfermeriaRepository.save(nota));
    }

    private NotaEnfermeriaDto toDto(NotaEnfermeria n) {
        String nombreProfesional = null;
        Long profesionalId = null;
        if (n.getProfesional() != null) {
            profesionalId = n.getProfesional().getId();
            nombreProfesional = (n.getProfesional().getNombres() + " " +
                    (n.getProfesional().getApellidos() != null ? n.getProfesional().getApellidos() : "")).trim();
        }
        return NotaEnfermeriaDto.builder()
                .id(n.getId())
                .atencionId(n.getAtencion() != null ? n.getAtencion().getId() : null)
                .nota(n.getNota())
                .fechaNota(n.getFechaNota())
                .profesionalId(profesionalId)
                .profesionalNombre(nombreProfesional)
                .createdAt(n.getCreatedAt())
                .build();
    }
}
