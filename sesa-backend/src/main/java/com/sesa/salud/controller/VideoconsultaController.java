/**
 * Telemedicina — Videoconsulta WebRTC (señalización REST).
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.controller;

import com.sesa.salud.dto.*;
import com.sesa.salud.service.VideoconsultaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/videoconsulta")
@RequiredArgsConstructor
public class VideoconsultaController {

    private final VideoconsultaService videoconsultaService;

    @PostMapping("/salas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SalaVideoconsultaDto> crearSala(@RequestBody(required = false) CrearSalaVideoconsultaRequestDto request) {
        if (request == null) {
            request = new CrearSalaVideoconsultaRequestDto();
        }
        return ResponseEntity.ok(videoconsultaService.crearSala(request));
    }

    @GetMapping("/salas/{salaId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<SalaVideoconsultaDto> unirseSala(
            @PathVariable String salaId,
            @RequestParam String token) {
        return ResponseEntity.ok(videoconsultaService.unirseSala(salaId, token));
    }

    @PostMapping("/salas/{salaId}/signaling")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Void> enviarSignaling(
            @PathVariable String salaId,
            @RequestParam String token,
            @RequestBody SignalingEventDto event) {
        videoconsultaService.enviarSignaling(salaId, token, event);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/salas/{salaId}/signaling")
    @PreAuthorize("permitAll()")
    public ResponseEntity<SignalingResponseDto> obtenerSignaling(
            @PathVariable String salaId,
            @RequestParam String token,
            @RequestParam(value = "after", defaultValue = "0") int after) {
        return ResponseEntity.ok(new SignalingResponseDto(
                videoconsultaService.obtenerSignaling(salaId, token, after)));
    }

    /** Creador: habilita asistente y devuelve token para el enlace del asistente. */
    @PostMapping("/salas/{salaId}/habilitar-asistente")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Map<String, String>> habilitarAsistente(
            @PathVariable String salaId,
            @RequestParam String token) {
        String tokenAsistente = videoconsultaService.habilitarAsistente(salaId, token);
        return ResponseEntity.ok(Map.of("tokenAsistente", tokenAsistente));
    }

    /** Paciente: registra consentimiento para que un asistente tome notas. */
    @PostMapping("/salas/{salaId}/consentimiento-asistente")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Void> registrarConsentimientoAsistente(
            @PathVariable String salaId,
            @RequestParam String token) {
        videoconsultaService.registrarConsentimientoAsistente(salaId, token);
        return ResponseEntity.ok().build();
    }

    /** Asistente: guarda las notas de la reunión. */
    @PutMapping("/salas/{salaId}/notas")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Void> guardarNotas(
            @PathVariable String salaId,
            @RequestParam String token,
            @RequestBody(required = false) NotasVideoconsultaDto body) {
        String texto = body != null && body.getTexto() != null ? body.getTexto() : "";
        videoconsultaService.guardarNotas(salaId, token, texto);
        return ResponseEntity.ok().build();
    }

    /** Creador: obtiene el resumen (notas) de la reunión. */
    @GetMapping("/salas/{salaId}/notas")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Map<String, String>> obtenerNotas(
            @PathVariable String salaId,
            @RequestParam String token) {
        String notas = videoconsultaService.obtenerNotas(salaId, token);
        return ResponseEntity.ok(Map.of("notas", notas != null ? notas : ""));
    }

    /** Valida token de asistente (para vista de notas). */
    @GetMapping("/salas/{salaId}/validar-asistente")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Map<String, Boolean>> validarAsistente(
            @PathVariable String salaId,
            @RequestParam String token) {
        boolean valido = videoconsultaService.validarAsistente(salaId, token);
        return ResponseEntity.ok(Map.of("valido", valido));
    }

    /** Paciente: true si el profesional solicitó asistente y el paciente aún no ha autorizado. */
    @GetMapping("/salas/{salaId}/solicitud-asistente-pendiente")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Map<String, Boolean>> solicitudAsistentePendiente(
            @PathVariable String salaId,
            @RequestParam String token) {
        boolean pendiente = videoconsultaService.solicitudAsistentePendiente(salaId, token);
        return ResponseEntity.ok(Map.of("pendiente", pendiente));
    }
}
