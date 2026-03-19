/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.dto.CupCatalogoDto;

import java.util.List;
import java.util.Optional;

public interface CupCatalogoService {

    List<CupCatalogoDto> listarActivos();

    List<CupCatalogoDto> buscar(String q, int limit);

    Optional<CupCatalogoDto> porCodigo(String codigo);
}
