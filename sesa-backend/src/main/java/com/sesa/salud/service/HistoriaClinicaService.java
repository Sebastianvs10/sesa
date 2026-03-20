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

    /**
     * Devuelve la HC del paciente; si no existe pero el paciente tiene al menos una consulta (nota SOAP),
     * crea una HC mínima y la devuelve para que la UI no muestre "Sin historia clínica".
     */
    Optional<HistoriaClinicaDto> findOrCreateMinimalIfHasConsultas(Long pacienteId);

    HistoriaClinicaDto createForPaciente(Long pacienteId, HistoriaClinicaRequestDto dto);
    HistoriaClinicaDto createCompleta(Long pacienteId, CrearHistoriaCompletaRequestDto dto);
    HistoriaClinicaDto update(Long id, HistoriaClinicaRequestDto dto);
}
