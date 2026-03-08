/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service;

import com.sesa.salud.dto.PlantillaSoapDto;

import java.util.List;
import java.util.Optional;

public interface PlantillaSoapService {

    List<PlantillaSoapDto> listarActivas();

    Optional<PlantillaSoapDto> findById(Long id);
}
