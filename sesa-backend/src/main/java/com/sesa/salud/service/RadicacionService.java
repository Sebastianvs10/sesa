/**
 * Servicio de radicación de facturas ante EPS.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.dto.RadicacionDto;
import com.sesa.salud.dto.RadicacionRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface RadicacionService {

    RadicacionDto create(RadicacionRequestDto dto);

    RadicacionDto update(Long id, RadicacionRequestDto dto);

    RadicacionDto findById(Long id);

    List<RadicacionDto> findByFacturaId(Long facturaId);

    Page<RadicacionDto> findAllFiltered(String estado, Instant desde, Instant hasta, Long facturaId, Pageable pageable);
}
