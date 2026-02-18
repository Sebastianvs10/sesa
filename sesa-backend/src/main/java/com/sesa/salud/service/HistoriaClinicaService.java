/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.dto.CrearHistoriaCompletaRequestDto;
import com.sesa.salud.dto.HistoriaClinicaDto;
import com.sesa.salud.dto.HistoriaClinicaRequestDto;

import java.util.Optional;

public interface HistoriaClinicaService {
    Optional<HistoriaClinicaDto> findByPacienteId(Long pacienteId);
    HistoriaClinicaDto createForPaciente(Long pacienteId, HistoriaClinicaRequestDto dto);
    HistoriaClinicaDto createCompleta(Long pacienteId, CrearHistoriaCompletaRequestDto dto);
    HistoriaClinicaDto update(Long id, HistoriaClinicaRequestDto dto);
}
