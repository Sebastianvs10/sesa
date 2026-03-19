/**
 * Servicio opcional para consultar datos por documento en ADRES/BDUA (vía Apitude).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.dto.ConsultaDocumentoDto;

import java.util.Optional;

public interface AdresConsultaService {

    /**
     * Consulta datos básicos por tipo y número de documento.
     * Si la integración ADRES/Apitude no está configurada, retorna vacío.
     *
     * @param tipoDocumento CC, CE, TI, PA, etc.
     * @param documento     Número de documento sin espacios
     * @return Datos básicos si la consulta tiene resultado; vacío si no configurado o sin datos
     */
    Optional<ConsultaDocumentoDto> consultarPorDocumento(String tipoDocumento, String documento);
}
