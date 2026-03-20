/**
 * Implementación inicial (stub) de integración con DIAN.
 * Deja todos los puntos de extensión listos para conectar con los servicios reales de la DIAN.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service.impl;

import com.sesa.salud.entity.Factura;
import com.sesa.salud.entity.FacturacionElectronicaConfig;
import com.sesa.salud.repository.FacturacionElectronicaConfigRepository;
import com.sesa.salud.service.FacturacionElectronicaDianService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class FacturacionElectronicaDianServiceStub implements FacturacionElectronicaDianService {

    private final FacturacionElectronicaConfigRepository configRepository;

    @Override
    @Transactional(readOnly = true)
    public void emitirFactura(Factura factura) {
        FacturacionElectronicaConfig config = configRepository.findTopByOrderByIdAsc()
                .orElse(null);
        if (config == null || Boolean.FALSE.equals(config.getFacturacionActiva())) {
            factura.setDianEstado("NO_ACTIVA");
            factura.setDianMensaje("Facturación electrónica DIAN no está activa para esta empresa.");
            return;
        }

        // Punto de extensión: aquí se debe implementar:
        // 1) Mapeo de Factura -> XML UBL 2.1 según anexo técnico DIAN.
        // 2) Firma XAdES-BES del XML con el certificado digital.
        // 3) Envío del XML firmado al servicio DIAN (habilitación/producción).
        // 4) Actualización de estado, CUFE/CUDE, URL QR y paths de almacenamiento XML/PDF.

        // Por ahora, dejamos un estado simulado de "PENDIENTE_DIAN" para entorno de desarrollo.
        factura.setDianEstado("PENDIENTE_DIAN");
        factura.setDianMensaje("Integración DIAN pendiente de implementación. Se generó una marca de simulación.");
        factura.setDianFechaEnvio(Instant.now());
    }
}

