/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.dto.AtencionDto;
import com.sesa.salud.dto.AtencionRequestDto;
import com.sesa.salud.dto.AltaReferenciaRequestDto;
import com.sesa.salud.dto.SignosVitalesIntegracionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AtencionService {
    Page<AtencionDto> findByHistoriaId(Long historiaId, Pageable pageable);
    AtencionDto findById(Long id);
    AtencionDto create(AtencionRequestDto dto);
    AtencionDto update(Long id, AtencionRequestDto dto);
    void deleteById(Long id);

    /** S6: Guardar datos de referencia para PDF. */
    AtencionDto guardarReferencia(Long id, AltaReferenciaRequestDto request);

    /** S12: Actualizar solo signos vitales (desde API integradores). */
    AtencionDto actualizarSignosVitales(Long id, SignosVitalesIntegracionDto dto);
}
