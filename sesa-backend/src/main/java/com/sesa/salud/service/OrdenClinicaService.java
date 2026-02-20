/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.dto.OrdenClinicaDto;
import com.sesa.salud.dto.OrdenClinicaRequestDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrdenClinicaService {
    List<OrdenClinicaDto> findByPacienteId(Long pacienteId, Pageable pageable);
    List<OrdenClinicaDto> findByTipo(String tipo, Pageable pageable);
    OrdenClinicaDto findById(Long id);
    OrdenClinicaDto create(OrdenClinicaRequestDto dto);
    OrdenClinicaDto update(Long id, OrdenClinicaRequestDto dto);
    void deleteById(Long id);
}
