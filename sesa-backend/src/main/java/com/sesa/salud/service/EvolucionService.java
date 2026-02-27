/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service;

import com.sesa.salud.dto.EvolucionDto;
import com.sesa.salud.dto.EvolucionRequestDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EvolucionService {

    List<EvolucionDto> findByAtencionId(Long atencionId, Pageable pageable);

    EvolucionDto findById(Long id);

    EvolucionDto create(EvolucionRequestDto dto);

    EvolucionDto update(Long id, EvolucionRequestDto dto);
}
