/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.dto.ImagenDiagnosticaDto;
import com.sesa.salud.dto.ImagenDiagnosticaRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface ImagenDiagnosticaService {
    List<ImagenDiagnosticaDto> findByAtencionId(Long atencionId, Pageable pageable);
    /** Listado global con filtros (paciente, atención, tipo, rango de fechas) y paginación. */
    Page<ImagenDiagnosticaDto> findGlobal(Long pacienteId, Long atencionId, String tipo, Instant fechaDesde, Instant fechaHasta, Pageable pageable);
    ImagenDiagnosticaDto findById(Long id);
    ImagenDiagnosticaDto create(ImagenDiagnosticaRequestDto dto);
    ImagenDiagnosticaDto update(Long id, ImagenDiagnosticaRequestDto dto);
    void deleteById(Long id);
}
