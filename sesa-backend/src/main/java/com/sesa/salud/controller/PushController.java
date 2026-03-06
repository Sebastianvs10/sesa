/**
 * Registro de dispositivos para notificaciones push (recordatorios, alertas).
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller;

import com.sesa.salud.dto.RegistroPushRequestDto;
import com.sesa.salud.entity.DispositivoPush;
import com.sesa.salud.repository.DispositivoPushRepository;
import com.sesa.salud.security.JwtPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/push")
@RequiredArgsConstructor
public class PushController {

    private final DispositivoPushRepository dispositivoPushRepository;

    /**
     * Registra o actualiza el token FCM/Web Push del usuario actual (portal/móvil).
     * Permite enviar recordatorios de cita y notificaciones al dispositivo.
     */
    @PostMapping("/register")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> register(
            @Valid @RequestBody RegistroPushRequestDto request,
            Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        Long usuarioId = principal.userId();
        String plataforma = request.getPlataforma() != null && !request.getPlataforma().isBlank()
                ? request.getPlataforma() : "WEB";
        if (plataforma.length() > 20) {
            plataforma = plataforma.substring(0, 20);
        }
        if (dispositivoPushRepository.findByUsuarioIdAndToken(usuarioId, request.getToken()).isEmpty()) {
            DispositivoPush d = DispositivoPush.builder()
                    .usuarioId(usuarioId)
                    .token(request.getToken())
                    .plataforma(plataforma)
                    .build();
            dispositivoPushRepository.save(d);
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Elimina el token del dispositivo (logout o desactivar notificaciones).
     */
    @DeleteMapping("/register")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unregister(
            @RequestParam("token") String token,
            Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        dispositivoPushRepository.deleteByUsuarioIdAndToken(principal.userId(), token);
        return ResponseEntity.noContent().build();
    }
}
