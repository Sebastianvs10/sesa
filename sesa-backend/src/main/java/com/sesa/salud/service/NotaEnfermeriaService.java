/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service;

import com.sesa.salud.dto.NotaEnfermeriaDto;
import com.sesa.salud.dto.NotaEnfermeriaRequestDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotaEnfermeriaService {

    List<NotaEnfermeriaDto> findByAtencionId(Long atencionId, Pageable pageable);

    NotaEnfermeriaDto findById(Long id);

    NotaEnfermeriaDto create(NotaEnfermeriaRequestDto dto);

    NotaEnfermeriaDto update(Long id, NotaEnfermeriaRequestDto dto);
}
