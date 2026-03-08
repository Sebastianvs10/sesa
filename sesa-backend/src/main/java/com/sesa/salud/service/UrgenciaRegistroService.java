/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service;

import com.sesa.salud.dto.UrgenciaDashboardDto;
import com.sesa.salud.dto.UrgenciaRegistroDto;
import com.sesa.salud.dto.UrgenciaRegistroRequestDto;
import com.sesa.salud.dto.UrgenciaReporteCumplimientoDto;
import com.sesa.salud.dto.UrgenciaTriagePatchDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface UrgenciaRegistroService {

    Page<UrgenciaRegistroDto> findAll(Pageable pageable);

    List<UrgenciaRegistroDto> findByEstado(String estado, Pageable pageable);

    UrgenciaRegistroDto findById(Long id);

    UrgenciaRegistroDto create(UrgenciaRegistroRequestDto dto);

    UrgenciaRegistroDto update(Long id, UrgenciaRegistroRequestDto dto);

    UrgenciaRegistroDto cambiarEstado(Long id, String nuevoEstado);

    void deleteById(Long id);

    /** Re-triage: actualiza solo nivel y opcionalmente profesional (sugerencia 4). */
    UrgenciaRegistroDto updateTriage(Long id, UrgenciaTriagePatchDto dto);

    UrgenciaDashboardDto getDashboard();

    UrgenciaReporteCumplimientoDto getReporteCumplimiento(LocalDate desde, LocalDate hasta);
}
