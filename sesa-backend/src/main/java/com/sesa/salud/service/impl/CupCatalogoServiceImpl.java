/**
 * Servicio del catálogo CUPS (Colombia).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service.impl;

import com.sesa.salud.dto.CupCatalogoDto;
import com.sesa.salud.entity.CupCatalogo;
import com.sesa.salud.repository.CupCatalogoRepository;
import com.sesa.salud.service.CupCatalogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CupCatalogoServiceImpl implements CupCatalogoService {

    private static final int SEARCH_LIMIT = 100;

    private final CupCatalogoRepository cupCatalogoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CupCatalogoDto> listarActivos() {
        return cupCatalogoRepository.findByActivoTrueOrderByCodigoAsc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CupCatalogoDto> buscar(String q, int limit) {
        int max = limit > 0 && limit <= 200 ? limit : SEARCH_LIMIT;
        if (q == null || q.isBlank()) {
            return cupCatalogoRepository.findByActivoTrueOrderByCodigoAsc().stream()
                    .limit(max)
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }
        return cupCatalogoRepository.buscarActivos(q.trim(), PageRequest.of(0, max)).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CupCatalogoDto> porCodigo(String codigo) {
        return cupCatalogoRepository.findByCodigo(codigo != null ? codigo.trim() : null)
                .map(this::toDto);
    }

    private CupCatalogoDto toDto(CupCatalogo c) {
        return CupCatalogoDto.builder()
                .id(c.getId())
                .codigo(c.getCodigo())
                .descripcion(c.getDescripcion())
                .capitulo(c.getCapitulo())
                .tipoServicio(c.getTipoServicio())
                .precioSugerido(c.getPrecioSugerido())
                .build();
    }
}
