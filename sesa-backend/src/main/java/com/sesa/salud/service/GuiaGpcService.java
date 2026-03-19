/**
 * S15: Servicio de guías de práctica clínica (GPC) por CIE-10.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.dto.GuiaGpcRegistroVisualizacionDto;
import com.sesa.salud.dto.GuiaGpcSugerenciaDto;

import java.util.List;

public interface GuiaGpcService {

    /**
     * Busca guías GPC por código CIE-10 (exacto o por prefijo).
     * Devuelve sugerencias para criterios de control, medicamentos de primera línea y estudios de seguimiento.
     */
    List<GuiaGpcSugerenciaDto> sugerirPorCie10(String codigoCie10);

    /**
     * Registra que se mostró una sugerencia GPC al profesional (auditoría).
     */
    void registrarVisualizacion(GuiaGpcRegistroVisualizacionDto dto);
}
