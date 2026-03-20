/**
 * S5: Reconciliación de medicamentos y alergias por atención.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.dto.ReconciliacionAtencionDto;
import com.sesa.salud.dto.ReconciliacionAtencionRequestDto;

public interface ReconciliacionService {

    /**
     * Obtiene la reconciliación de una atención si existe; si no, devuelve datos de HC (medicamentos/alergias) para prellenar.
     */
    ReconciliacionAtencionDto getByAtencionId(Long atencionId);

    /**
     * Guarda o actualiza la reconciliación para la atención. El profesional se toma del usuario autenticado.
     */
    ReconciliacionAtencionDto guardar(Long atencionId, ReconciliacionAtencionRequestDto request);
}
