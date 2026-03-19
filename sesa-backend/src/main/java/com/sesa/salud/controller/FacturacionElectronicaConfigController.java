/**
 * API para gestionar la configuración de facturación electrónica DIAN por empresa (tenant actual).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.controller;

import com.sesa.salud.dto.FacturacionElectronicaConfigDto;
import com.sesa.salud.service.FacturacionElectronicaConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/facturacion/config-electronica")
@RequiredArgsConstructor
public class FacturacionElectronicaConfigController {

    private final FacturacionElectronicaConfigService service;

    /** Obtiene la configuración actual (o crea una por defecto si no existe). */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','FACTURACION','RECEPCIONISTA','SUPERADMINISTRADOR')")
    public ResponseEntity<FacturacionElectronicaConfigDto> get() {
        return ResponseEntity.ok(service.getOrCreate());
    }

    /** Actualiza la configuración de facturación electrónica. */
    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','FACTURACION','SUPERADMINISTRADOR')")
    public ResponseEntity<FacturacionElectronicaConfigDto> update(@RequestBody FacturacionElectronicaConfigDto dto) {
        return ResponseEntity.ok(service.update(dto));
    }
}

