/**
 * S10: Cuestionarios pre-consulta (ePRO) — crear y consultar por cita/consulta.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.dto.CuestionarioPreconsultaDto;
import com.sesa.salud.dto.CuestionarioPreconsultaRequestDto;

import java.util.Optional;

public interface CuestionarioPreconsultaService {

    /**
     * Crea el cuestionario para la cita (desde portal con sesión paciente o con token de cita).
     * Valida que la cita pertenezca al paciente del usuario autenticado.
     */
    CuestionarioPreconsultaDto create(CuestionarioPreconsultaRequestDto dto, Long pacienteIdAutenticado);

    Optional<CuestionarioPreconsultaDto> getByCitaId(Long citaId);

    /**
     * Obtiene el cuestionario asociado a la consulta (vía cita de la consulta). Para que el médico vea lo enviado.
     */
    Optional<CuestionarioPreconsultaDto> getByConsultaId(Long consultaId);
}
