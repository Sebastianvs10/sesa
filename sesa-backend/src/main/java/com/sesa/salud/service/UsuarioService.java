/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.dto.UsuarioDto;
import com.sesa.salud.dto.UsuarioRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UsuarioService {
    Page<UsuarioDto> findAll(Pageable pageable);
    UsuarioDto findById(Long id);
    UsuarioDto create(UsuarioRequestDto dto);
    UsuarioDto update(Long id, UsuarioRequestDto dto);
    void deleteById(Long id);
}
