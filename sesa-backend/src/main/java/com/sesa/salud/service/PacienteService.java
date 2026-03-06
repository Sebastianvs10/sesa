/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service;

import com.sesa.salud.dto.PacienteDto;
import com.sesa.salud.dto.PacienteRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PacienteService {

    Page<PacienteDto> findAll(Pageable pageable);

    /**
     * Lista pacientes opcionalmente filtrados por estado activo.
     * @param activo null = todos, true = solo activos, false = solo inactivos.
     */
    Page<PacienteDto> findAll(Pageable pageable, Boolean activo);

    Page<PacienteDto> search(String q, Pageable pageable);

    PacienteDto findById(Long id);

    PacienteDto create(PacienteRequestDto dto);

    PacienteDto update(Long id, PacienteRequestDto dto);

    void deleteById(Long id);
}
