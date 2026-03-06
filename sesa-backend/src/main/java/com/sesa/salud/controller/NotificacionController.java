/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller;

import com.sesa.salud.dto.DestinatarioDisponibleDto;
import com.sesa.salud.dto.MarcarLeidasRequest;
import com.sesa.salud.dto.NotificacionBroadcastResult;
import com.sesa.salud.dto.NotificacionCreateRequest;
import com.sesa.salud.dto.NotificacionDto;
import com.sesa.salud.entity.NotificacionAdjunto;
import com.sesa.salud.entity.Usuario;
import com.sesa.salud.repository.NotificacionAdjuntoRepository;
import com.sesa.salud.repository.UsuarioRepository;
import com.sesa.salud.security.JwtPrincipal;
import com.sesa.salud.service.NotificacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/notificaciones")
@RequiredArgsConstructor
@Slf4j
public class NotificacionController {

    private final NotificacionService notificacionService;
    private final NotificacionAdjuntoRepository adjuntoRepository;
    private final UsuarioRepository usuarioRepository;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificacionDto> create(
            @Valid @RequestBody NotificacionCreateRequest request,
            Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        String nombre = usuarioRepository.findById(principal.userId())
                .map(Usuario::getNombreCompleto)
                .orElse(principal.username());
        NotificacionDto dto = notificacionService.create(request, principal.userId(), nombre);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/{id}/adjuntos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> uploadAdjunto(
            @PathVariable("id") Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        notificacionService.addAdjunto(
                id,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getBytes());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificacionDto> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(notificacionService.getById(id));
    }

    @GetMapping("/enviadas")
    @PreAuthorize("isAuthenticated()")
    public Page<NotificacionDto> listEnviadas(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        return notificacionService.listEnviadas(
                principal.userId(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaEnvio")));
    }

    @GetMapping("/recibidas")
    @PreAuthorize("isAuthenticated()")
    public Page<NotificacionDto> listRecibidas(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        return notificacionService.listRecibidas(
                principal.userId(),
                PageRequest.of(page, size));
    }

    @GetMapping("/recibidas/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Long> countNoLeidas(Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(notificacionService.countNoLeidas(principal.userId()));
    }

    @PutMapping("/{id}/leer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> marcarLeida(
            @PathVariable("id") Long id,
            Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        notificacionService.marcarLeida(id, principal.userId());
        return ResponseEntity.ok().build();
    }

    /**
     * Marca como leídas varias notificaciones para el usuario actual (solo donde es destinatario).
     */
    @PostMapping("/marcar-leidas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> marcarLeidas(
            @Valid @RequestBody MarcarLeidasRequest request,
            Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        if (request.getNotificacionIds() != null && !request.getNotificacionIds().isEmpty()) {
            notificacionService.marcarLeidas(request.getNotificacionIds(), principal.userId());
        }
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/no-leer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> marcarNoLeida(
            @PathVariable("id") Long id,
            Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        notificacionService.marcarNoLeida(id, principal.userId());
        return ResponseEntity.ok().build();
    }

    /**
     * Marca como no leídas varias notificaciones para el usuario actual (solo donde es destinatario).
     */
    @PostMapping("/marcar-no-leidas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> marcarNoLeidas(
            @Valid @RequestBody MarcarLeidasRequest request,
            Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        if (request.getNotificacionIds() != null && !request.getNotificacionIds().isEmpty()) {
            notificacionService.marcarNoLeidas(request.getNotificacionIds(), principal.userId());
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Elimina un adjunto de una notificación. Solo el remitente de la notificación puede hacerlo.
     */
    @DeleteMapping("/{notificacionId}/adjuntos/{adjuntoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteAdjunto(
            @PathVariable("notificacionId") Long notificacionId,
            @PathVariable("adjuntoId") Long adjuntoId,
            Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        notificacionService.deleteAdjunto(notificacionId, adjuntoId, principal.userId());
        return ResponseEntity.noContent().build();
    }

    // ── Destinatarios disponibles ──────────────────────────────────────────────

    /**
     * Devuelve los usuarios activos del schema actual que pueden recibir notificaciones.
     * Cualquier usuario autenticado puede consultarlo para seleccionar destinatarios.
     */
    @GetMapping("/destinatarios-disponibles")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DestinatarioDisponibleDto>> getDestinatariosDisponibles() {
        return ResponseEntity.ok(notificacionService.getDestinatariosDisponibles());
    }

    // ── Broadcast SUPERADMINISTRADOR ──────────────────────────────────────────

    /**
     * Envía una notificación al usuario ADMIN de cada empresa/schema activo.
     * Solo accesible para SUPERADMINISTRADOR.
     */
    @PostMapping("/broadcast-admins")
    @PreAuthorize("hasRole('SUPERADMINISTRADOR')")
    public ResponseEntity<NotificacionBroadcastResult> broadcastToAdmins(
            @Valid @RequestBody NotificacionCreateRequest request,
            Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        String nombre = usuarioRepository.findById(principal.userId())
                .map(Usuario::getNombreCompleto)
                .orElse(principal.username());
        NotificacionBroadcastResult result =
                notificacionService.broadcastToAdmins(request, principal.userId(), nombre);
        return ResponseEntity.ok(result);
    }

    // ── Adjunto: descarga ─────────────────────────────────────────────────────

    @GetMapping("/{notificacionId}/adjuntos/{adjuntoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadAdjunto(
            @PathVariable("notificacionId") Long notificacionId,
            @PathVariable("adjuntoId") Long adjuntoId) {
        NotificacionAdjunto adjunto = adjuntoRepository.findById(adjuntoId)
                .filter(a -> a.getNotificacion().getId().equals(notificacionId))
                .orElseThrow(() -> new RuntimeException(
                        "Adjunto no encontrado: " + adjuntoId + " en notificación " + notificacionId));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        adjunto.getContentType() != null ? adjunto.getContentType() : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + adjunto.getNombreArchivo() + "\"")
                .body(adjunto.getDatos());
    }
}
