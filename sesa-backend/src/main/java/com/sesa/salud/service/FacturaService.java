/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.dto.FacturaDto;
import com.sesa.salud.dto.FacturaRequestDto;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface FacturaService {
    List<FacturaDto> findByPacienteId(Long pacienteId, Pageable pageable);
    FacturaDto findById(Long id);
    FacturaDto create(FacturaRequestDto dto);
    FacturaDto update(Long id, FacturaRequestDto dto);
    void deleteById(Long id);
    String exportRipsCsv(Instant desde, Instant hasta);
}
