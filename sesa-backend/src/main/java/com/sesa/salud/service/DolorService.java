/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service;

import com.sesa.salud.dto.DolorDto;
import com.sesa.salud.dto.DolorRequestDto;

import java.util.List;

public interface DolorService {

    List<DolorDto> findByPacienteId(Long pacienteId);

    List<DolorDto> findByHistoriaClinicaId(Long historiaClinicaId);

    DolorDto findById(Long id);

    DolorDto create(DolorRequestDto dto);

    DolorDto update(Long id, DolorRequestDto dto);

    void deleteById(Long id);
}
