/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.dto.AtencionDto;
import com.sesa.salud.dto.AtencionRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AtencionService {
    Page<AtencionDto> findByHistoriaId(Long historiaId, Pageable pageable);
    AtencionDto findById(Long id);
    AtencionDto create(AtencionRequestDto dto);
    AtencionDto update(Long id, AtencionRequestDto dto);
    void deleteById(Long id);
}
