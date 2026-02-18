/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service;

import com.sesa.salud.dto.ConsultaDto;
import com.sesa.salud.dto.ConsultaRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ConsultaService {

    Page<ConsultaDto> findAll(Pageable pageable);

    List<ConsultaDto> findByPacienteId(Long pacienteId, Pageable pageable);

    List<ConsultaDto> findMisConsultas(Pageable pageable);

    ConsultaDto findById(Long id);

    ConsultaDto create(ConsultaRequestDto dto);

    ConsultaDto update(Long id, ConsultaRequestDto dto);

    void deleteById(Long id);
}
