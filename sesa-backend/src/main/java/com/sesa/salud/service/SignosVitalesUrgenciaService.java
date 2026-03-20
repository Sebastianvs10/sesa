/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service;

import com.sesa.salud.dto.SignosVitalesUrgenciaDto;
import com.sesa.salud.dto.SignosVitalesUrgenciaRequestDto;

import java.util.List;

public interface SignosVitalesUrgenciaService {

    List<SignosVitalesUrgenciaDto> findByUrgenciaRegistroId(Long urgenciaRegistroId);

    SignosVitalesUrgenciaDto create(SignosVitalesUrgenciaRequestDto dto);
}
