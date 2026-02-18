/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service;

import com.sesa.salud.dto.LaboratorioSolicitudDto;
import com.sesa.salud.dto.LaboratorioSolicitudRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LaboratorioSolicitudService {

    Page<LaboratorioSolicitudDto> findAll(Pageable pageable);

    List<LaboratorioSolicitudDto> findByPacienteId(Long pacienteId, Pageable pageable);

    List<LaboratorioSolicitudDto> findByEstado(String estado, Pageable pageable);

    LaboratorioSolicitudDto findById(Long id);

    LaboratorioSolicitudDto create(LaboratorioSolicitudRequestDto dto);

    LaboratorioSolicitudDto update(Long id, LaboratorioSolicitudRequestDto dto);

    void deleteById(Long id);
}
