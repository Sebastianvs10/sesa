/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service;

import com.sesa.salud.dto.CitaDto;
import com.sesa.salud.dto.CitaRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface CitaService {

    Page<CitaDto> findAll(Pageable pageable);

    List<CitaDto> findByFecha(LocalDate fecha);

    List<CitaDto> findByPacienteId(Long pacienteId, Pageable pageable);

    CitaDto findById(Long id);

    CitaDto create(CitaRequestDto dto);

    CitaDto update(Long id, CitaRequestDto dto);

    void deleteById(Long id);
}
