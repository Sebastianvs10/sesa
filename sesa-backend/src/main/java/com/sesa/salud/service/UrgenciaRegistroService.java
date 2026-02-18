/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service;

import com.sesa.salud.dto.UrgenciaRegistroDto;
import com.sesa.salud.dto.UrgenciaRegistroRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UrgenciaRegistroService {

    Page<UrgenciaRegistroDto> findAll(Pageable pageable);

    List<UrgenciaRegistroDto> findByEstado(String estado, Pageable pageable);

    UrgenciaRegistroDto findById(Long id);

    UrgenciaRegistroDto create(UrgenciaRegistroRequestDto dto);

    UrgenciaRegistroDto update(Long id, UrgenciaRegistroRequestDto dto);

    void deleteById(Long id);
}
