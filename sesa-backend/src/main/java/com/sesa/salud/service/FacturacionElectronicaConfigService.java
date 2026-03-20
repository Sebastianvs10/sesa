/**
 * Servicio para gestionar la configuración de facturación electrónica DIAN por tenant.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.dto.FacturacionElectronicaConfigDto;

public interface FacturacionElectronicaConfigService {

    /** Obtiene la configuración actual; si no existe, crea un registro con valores por defecto. */
    FacturacionElectronicaConfigDto getOrCreate();

    /** Actualiza la configuración de facturación electrónica. */
    FacturacionElectronicaConfigDto update(FacturacionElectronicaConfigDto dto);
}

