/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.dto.OrdenClinicaBatchRequestDto;
import com.sesa.salud.dto.OrdenClinicaDto;
import com.sesa.salud.dto.OrdenClinicaRequestDto;
import com.sesa.salud.dto.ResultadoOrdenDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrdenClinicaService {
    List<OrdenClinicaDto> findByPacienteId(Long pacienteId, Pageable pageable);
    List<OrdenClinicaDto> findByTipo(String tipo, Pageable pageable);
    OrdenClinicaDto findById(Long id);
    OrdenClinicaDto create(OrdenClinicaRequestDto dto);
    /** Crea una sola orden con todos los ítems (medicamentos, labs, procedimientos) en una única emisión. */
    OrdenClinicaDto createBatch(OrdenClinicaBatchRequestDto batch);
    OrdenClinicaDto update(Long id, OrdenClinicaRequestDto dto);
    OrdenClinicaDto registrarResultado(Long id, ResultadoOrdenDto dto);
    void deleteById(Long id);
}
