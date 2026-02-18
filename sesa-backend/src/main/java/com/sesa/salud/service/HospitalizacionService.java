/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.dto.HospitalizacionDto;
import com.sesa.salud.dto.HospitalizacionRequestDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface HospitalizacionService {
    List<HospitalizacionDto> findByPacienteId(Long pacienteId, Pageable pageable);
    List<HospitalizacionDto> findByEstado(String estado, Pageable pageable);
    HospitalizacionDto findById(Long id);
    HospitalizacionDto create(HospitalizacionRequestDto dto);
    HospitalizacionDto update(Long id, HospitalizacionRequestDto dto);
    void deleteById(Long id);
}
