/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.sesa.salud.dto.PlantillaSoapDto;
import com.sesa.salud.entity.PlantillaSoap;
import com.sesa.salud.repository.PlantillaSoapRepository;
import com.sesa.salud.service.PlantillaSoapService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlantillaSoapServiceImpl implements PlantillaSoapService {

    private final PlantillaSoapRepository plantillaSoapRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PlantillaSoapDto> listarActivas() {
        return plantillaSoapRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlantillaSoapDto> findById(Long id) {
        return plantillaSoapRepository.findById(id).map(this::toDto);
    }

    private PlantillaSoapDto toDto(PlantillaSoap p) {
        return PlantillaSoapDto.builder()
                .id(p.getId())
                .nombre(p.getNombre())
                .motivoTipo(p.getMotivoTipo())
                .contenidoSubjetivo(p.getContenidoSubjetivo())
                .contenidoObjetivo(p.getContenidoObjetivo())
                .contenidoAnalisis(p.getContenidoAnalisis())
                .contenidoPlan(p.getContenidoPlan())
                .codigoCie10Sugerido(p.getCodigoCie10Sugerido())
                .activo(p.getActivo())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
