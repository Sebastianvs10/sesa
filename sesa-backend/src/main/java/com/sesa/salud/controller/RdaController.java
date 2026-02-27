/**
 * API REST para el módulo de Interoperabilidad RDA
 * Resolución 1888 de 2025 — IHCE Colombia
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.controller;

import com.sesa.salud.dto.rda.RdaStatusDto;
import com.sesa.salud.entity.RdaEnvio;
import com.sesa.salud.service.fhir.RdaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoints del módulo RDA (Resumen Digital de Atención en Salud).
 *
 * Prefijo: /rda
 *
 * Flujo recomendado:
 *  1. POST /rda/generar/{atencionId}   → genera el Bundle FHIR
 *  2. POST /rda/enviar/{atencionId}    → envía al Ministerio
 *  O en un solo paso:
 *     POST /rda/generar-y-enviar/{atencionId}
 */
@RestController
@RequestMapping("/rda")
@RequiredArgsConstructor
public class RdaController {

    private final RdaService rdaService;

    // ─── Generar RDA ────────────────────────────────────────────────────────

    @PostMapping("/generar/{atencionId:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<RdaStatusDto> generarRda(
            @PathVariable Long atencionId,
            @RequestParam(defaultValue = "CONSULTA_EXTERNA") RdaEnvio.TipoRda tipoRda) {
        return ResponseEntity.ok(rdaService.generarRda(atencionId, tipoRda));
    }

    // ─── Enviar al Ministerio ───────────────────────────────────────────────

    @PostMapping("/enviar/{atencionId:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<RdaStatusDto> enviarAlMinisterio(
            @PathVariable Long atencionId,
            @RequestParam(defaultValue = "CONSULTA_EXTERNA") RdaEnvio.TipoRda tipoRda) {
        return ResponseEntity.ok(rdaService.enviarAlMinisterio(atencionId, tipoRda));
    }

    // ─── Generar + Enviar en un paso ────────────────────────────────────────

    @PostMapping("/generar-y-enviar/{atencionId:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<RdaStatusDto> generarYEnviar(
            @PathVariable Long atencionId,
            @RequestParam(defaultValue = "CONSULTA_EXTERNA") RdaEnvio.TipoRda tipoRda) {
        return ResponseEntity.ok(rdaService.generarYEnviar(atencionId, tipoRda));
    }

    // ─── Consultar estado ───────────────────────────────────────────────────

    @GetMapping("/estado/{atencionId:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','USER','FACTURACION','SUPERADMINISTRADOR')")
    public ResponseEntity<List<RdaStatusDto>> listarPorAtencion(
            @PathVariable Long atencionId) {
        return ResponseEntity.ok(rdaService.listarPorAtencion(atencionId));
    }

    @GetMapping("/estado/{atencionId:\\d+}/ultimo")
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','USER','FACTURACION','SUPERADMINISTRADOR')")
    public ResponseEntity<RdaStatusDto> obtenerUltimo(
            @PathVariable Long atencionId,
            @RequestParam(defaultValue = "CONSULTA_EXTERNA") RdaEnvio.TipoRda tipoRda) {
        RdaStatusDto dto = rdaService.obtenerUltimo(atencionId, tipoRda);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    // ─── Descargar Bundle FHIR (JSON) ──────────────────────────────────────

    @GetMapping(value = "/bundle/{rdaId:\\d+}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','MEDICO','SUPERADMINISTRADOR')")
    public ResponseEntity<String> descargarBundle(@PathVariable Long rdaId) {
        String json = rdaService.obtenerBundleJson(rdaId, true);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/fhir+json"))
                .header("Content-Disposition",
                        "attachment; filename=\"rda-bundle-" + rdaId + ".json\"")
                .body(json);
    }

    // ─── Información de cumplimiento normativo ──────────────────────────────

    @GetMapping("/info")
    public ResponseEntity<Map<String, String>> infoNormativo() {
        return ResponseEntity.ok(Map.of(
            "norma",     "Resolución 1888 de 2025",
            "estandar",  "HL7 FHIR R4 (4.0.1)",
            "guia",      "https://vulcano.ihcecol.gov.co/guia/",
            "version",   "RDA CO v0.7.2",
            "tipos_rda", "CONSULTA_EXTERNA, HOSPITALIZACION, URGENCIAS, PACIENTE"
        ));
    }
}
