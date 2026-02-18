/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.controller;

import com.sesa.salud.dto.auth.AccesoAuditoriaDto;
import com.sesa.salud.dto.auth.PasswordResetConfirmDto;
import com.sesa.salud.dto.auth.PasswordResetRequestDto;
import com.sesa.salud.dto.auth.LoginRequest;
import com.sesa.salud.dto.auth.LoginResponse;
import com.sesa.salud.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/password/request-reset")
    public ResponseEntity<Map<String, String>> requestReset(@Valid @RequestBody PasswordResetRequestDto dto) {
        String token = authService.requestPasswordReset(dto.getEmail());
        return ResponseEntity.ok(Map.of(
                "message", "Solicitud procesada. Usa el token para restablecer contraseña.",
                "token", token
        ));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody PasswordResetConfirmDto dto) {
        authService.resetPassword(dto.getToken(), dto.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente"));
    }

    @GetMapping("/auditoria")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMINISTRADOR')")
    public Page<AccesoAuditoriaDto> auditoria(@PageableDefault(size = 30) Pageable pageable) {
        return authService.listAuditoria(pageable);
    }
}
