/**
 * Servicio responsable de integrar con la DIAN para facturación electrónica.
 * Esta capa aísla la generación UBL 2.1, firma y envío.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service;

import com.sesa.salud.entity.Factura;

public interface FacturacionElectronicaDianService {

    /**
     * Emite una factura electrónica ante la DIAN.
     * Debe actualizar la entidad Factura con CUFE/CUDE, estado DIAN y mensaje de respuesta.
     */
    void emitirFactura(Factura factura);
}

