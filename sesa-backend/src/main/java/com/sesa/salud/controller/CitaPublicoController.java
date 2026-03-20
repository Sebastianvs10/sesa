/**
 * Endpoints públicos para confirmar/cancelar cita por enlace (S3).
 * Sin autenticación; se valida por token en query.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.controller;

import com.sesa.salud.service.CitaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/cita")
@RequiredArgsConstructor
public class CitaPublicoController {

    private final CitaService citaService;

    /**
     * Confirma la cita por token (enlace del recordatorio).
     * GET /api/cita/confirmar?t=TOKEN
     */
    @GetMapping(value = "/confirmar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> confirmar(@RequestParam("t") String token) {
        try {
            String mensaje = citaService.confirmarCitaPorToken(token);
            return ResponseEntity.ok(Map.of("ok", true, "mensaje", mensaje));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    /**
     * Cancela la cita por token.
     * GET /api/cita/cancelar?t=TOKEN
     * POST /api/cita/cancelar con body {"t":"TOKEN","motivo":"opcional"}
     */
    @GetMapping(value = "/cancelar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> cancelarGet(@RequestParam("t") String token) {
        return cancelar(token, null);
    }

    @PostMapping(value = "/cancelar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> cancelarPost(@RequestBody Map<String, String> body) {
        String token = body != null ? body.get("t") : null;
        String motivo = body != null ? body.get("motivo") : null;
        return cancelar(token, motivo);
    }

    private ResponseEntity<Map<String, Object>> cancelar(String token, String motivo) {
        try {
            String mensaje = citaService.cancelarCitaPorToken(token, motivo);
            return ResponseEntity.ok(Map.of("ok", true, "mensaje", mensaje));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", e.getMessage()));
        }
    }
}
