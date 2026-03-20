/**
 * S12: API REST para integradores externos (laboratorio, PACS, signos vitales).
 * Autenticación por X-API-Key + X-Tenant-Schema.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.controller;

import com.sesa.salud.dto.ResultadoOrdenDto;
import com.sesa.salud.dto.SignosVitalesIntegracionDto;
import com.sesa.salud.security.ApiKeyPrincipal;
import com.sesa.salud.service.AtencionService;
import com.sesa.salud.service.OrdenClinicaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/integracion")
@RequiredArgsConstructor
public class IntegracionController {

    private final OrdenClinicaService ordenClinicaService;
    private final AtencionService atencionService;

    /** Registra resultado de laboratorio en la orden. Requiere permiso LABORATORIO. */
    @PostMapping("/ordenes/{ordenId:\\d+}/resultado")
    @PreAuthorize("hasAuthority('PERMISO_LABORATORIO')")
    public ResponseEntity<?> registrarResultadoOrden(
            @AuthenticationPrincipal ApiKeyPrincipal principal,
            @PathVariable Long ordenId,
            @Valid @RequestBody ResultadoOrdenDto dto) {
        return ResponseEntity.ok(ordenClinicaService.registrarResultado(ordenId, dto));
    }

    /** Registra signos vitales en la atención. Requiere permiso SIGNOS_VITALES. */
    @PostMapping("/atenciones/{atencionId:\\d+}/signos-vitales")
    @PreAuthorize("hasAuthority('PERMISO_SIGNOS_VITALES')")
    public ResponseEntity<?> registrarSignosVitales(
            @AuthenticationPrincipal ApiKeyPrincipal principal,
            @PathVariable Long atencionId,
            @RequestBody SignosVitalesIntegracionDto dto) {
        return ResponseEntity.ok(atencionService.actualizarSignosVitales(atencionId, dto));
    }
}
