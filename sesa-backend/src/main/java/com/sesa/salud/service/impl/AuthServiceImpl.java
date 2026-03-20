/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.sesa.salud.dto.auth.AccesoAuditoriaDto;
import com.sesa.salud.dto.auth.LoginRequest;
import com.sesa.salud.dto.auth.LoginResponse;
import com.sesa.salud.entity.AccesoAuditoria;
import com.sesa.salud.entity.PasswordResetToken;
import com.sesa.salud.entity.Usuario;
import com.sesa.salud.repository.AccesoAuditoriaRepository;
import com.sesa.salud.repository.PasswordResetTokenRepository;
import com.sesa.salud.repository.PersonalRepository;
import com.sesa.salud.repository.UsuarioRepository;
import com.sesa.salud.repository.master.EmpresaRepository;
import com.sesa.salud.repository.master.TenantUsuarioLoginRepository;
import com.sesa.salud.security.JwtTokenProvider;
import com.sesa.salud.service.AuthService;
import com.sesa.salud.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PersonalRepository personalRepository;
    private final TenantUsuarioLoginRepository tenantUsuarioLoginRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AccesoAuditoriaRepository accesoAuditoriaRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${sesa.jwt.expiration-ms}")
    private long jwtExpirationMs;

    /**
     * Login: determina el tenant (schema) del usuario desde tenant_usuario_login (public),
     * valida credenciales contra ese esquema y devuelve un JWT que incluye el schema.
     * Todas las peticiones posteriores enviarán ese JWT; el TenantFilter usará el schema
     * del token para apuntar siempre al mismo tenant.
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        String input = request.getEmail().trim();
        String email = resolveEmail(input);

        String schema = tenantUsuarioLoginRepository.findByEmail(email)
                .map(t -> t.getSchemaName())
                .orElseThrow(() -> {
                    logAcceso(email, "LOGIN_FAIL", "Email sin tenant asociado");
                    return new BadCredentialsException("Credenciales inválidas");
                });

        try {
            TenantContextHolder.setTenantSchema(schema);

            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        logAcceso(email, "LOGIN_FAIL", "Usuario no encontrado en tenant");
                        return new BadCredentialsException("Credenciales inválidas");
                    });

            if (!Boolean.TRUE.equals(usuario.getActivo())) {
                logAcceso(email, "LOGIN_FAIL", "Usuario inactivo");
                throw new BadCredentialsException("Usuario inactivo");
            }

            if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
                logAcceso(email, "LOGIN_FAIL", "Password inválido");
                throw new BadCredentialsException("Credenciales inválidas");
            }

            // Pasar todos los roles al JWT; el token almacena el array completo
            java.util.Set<String> userRoles = usuario.getRoles().isEmpty()
                    ? java.util.Set.of("USER") : usuario.getRoles();
            // Rol primario: SUPERADMINISTRADOR > ADMIN > primero
            String primaryRole = userRoles.contains("SUPERADMINISTRADOR") ? "SUPERADMINISTRADOR"
                    : userRoles.contains("ADMIN") ? "ADMIN"
                    : userRoles.iterator().next();
            String token = jwtTokenProvider.generateToken(
                    usuario.getEmail(), usuario.getId(), userRoles, schema);

            Long personalId = personalRepository.findByUsuario_Id(usuario.getId())
                    .map(p -> p.getId())
                    .orElse(null);

            LoginResponse.LoginResponseBuilder responseBuilder = LoginResponse.builder()
                    .accessToken(token)
                    .tokenType("Bearer")
                    .expiresInMs(jwtExpirationMs)
                    .userId(usuario.getId())
                    .personalId(personalId)
                    .email(usuario.getEmail())
                    .nombreCompleto(usuario.getNombreCompleto())
                    .role(primaryRole)
                    .roles(new java.util.ArrayList<>(userRoles))
                    .rolActivo(primaryRole)
                    .schema(schema);

            // Incluir nombre y logo de la empresa para que el frontend no necesite un GET extra
            TenantContextHolder.setTenantSchema(TenantContextHolder.PUBLIC);
            if (TenantContextHolder.PUBLIC.equals(schema)) {
                // SUPERADMINISTRADOR: no tiene empresa propia.
                // Buscar en BD la primera empresa activa con logo UUID para mostrarlo en el sidebar.
                empresaRepository.findAll().stream()
                        .filter(e -> e.getImagenUrl() != null && isUuid(e.getImagenUrl()))
                        .findFirst()
                        .ifPresent(emp -> {
                            responseBuilder.empresaNombre(emp.getRazonSocial());
                            responseBuilder.empresaLogoUuid(emp.getImagenUrl());
                        });
            } else {
                empresaRepository.findBySchemaName(schema).ifPresent(emp -> {
                    responseBuilder.empresaNombre(emp.getRazonSocial());
                    if (emp.getImagenUrl() != null && isUuid(emp.getImagenUrl())) {
                        responseBuilder.empresaLogoUuid(emp.getImagenUrl());
                    }
                });
            }

            logAcceso(email, "LOGIN_OK", "Inicio de sesión exitoso");
            return responseBuilder.build();

        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public String requestPasswordReset(String email) {
        String normalized = email.trim().toLowerCase();
        String schema = tenantUsuarioLoginRepository.findByEmail(normalized)
                .map(t -> t.getSchemaName())
                .orElseThrow(() -> new RuntimeException("No existe cuenta para ese correo"));

        try {
            TenantContextHolder.setTenantSchema(schema);
            usuarioRepository.findByEmail(normalized)
                    .orElseThrow(() -> new RuntimeException("No existe cuenta para ese correo"));

            String token = UUID.randomUUID().toString().replace("-", "");
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .email(normalized)
                    .token(token)
                    .expiraEn(Instant.now().plusSeconds(30 * 60))
                    .usado(false)
                    .build();
            passwordResetTokenRepository.save(resetToken);
            logAcceso(normalized, "RESET_REQUEST", "Solicitud de recuperación");
            return token;
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token inválido"));
        if (Boolean.TRUE.equals(resetToken.getUsado()) || resetToken.getExpiraEn().isBefore(Instant.now())) {
            throw new RuntimeException("Token expirado o usado");
        }
        String email = resetToken.getEmail();
        String schema = tenantUsuarioLoginRepository.findByEmail(email)
                .map(t -> t.getSchemaName())
                .orElseThrow(() -> new RuntimeException("No existe tenant asociado al correo"));
        try {
            TenantContextHolder.setTenantSchema(schema);
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            usuario.setPasswordHash(passwordEncoder.encode(newPassword));
            usuarioRepository.save(usuario);
            resetToken.setUsado(true);
            passwordResetTokenRepository.save(resetToken);
            logAcceso(email, "RESET_OK", "Contraseña restablecida");
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public Page<AccesoAuditoriaDto> listAuditoria(Pageable pageable) {
        return accesoAuditoriaRepository.findAllByOrderByFechaDesc(pageable)
                .map(a -> AccesoAuditoriaDto.builder()
                        .id(a.getId())
                        .email(a.getEmail())
                        .evento(a.getEvento())
                        .ip(a.getIp())
                        .detalle(a.getDetalle())
                        .fecha(a.getFecha())
                        .build());
    }

    /**
     * Resuelve el login (correo o número de identificación) al email del usuario.
     * Si el input contiene '@' se trata como email; si no, se busca Personal por identificacion
     * en cada tenant hasta encontrar una coincidencia.
     */
    private String resolveEmail(String input) {
        if (input.contains("@")) {
            return input;
        }
        String found = empresaRepository.findAll().stream()
                .map(e -> e.getSchemaName())
                .filter(s -> s != null && !s.isBlank())
                .map(schema -> {
                    try {
                        TenantContextHolder.setTenantSchema(schema);
                        return personalRepository.findByIdentificacionWithUsuario(input)
                                .filter(p -> p.getUsuario() != null)
                                .map(p -> p.getUsuario().getEmail())
                                .orElse(null);
                    } finally {
                        TenantContextHolder.clear();
                    }
                })
                .filter(e -> e != null)
                .findFirst()
                .orElse(null);
        if (found != null) return found;
        logAcceso(input, "LOGIN_FAIL", "Identificación sin usuario asociado");
        throw new BadCredentialsException("Credenciales inválidas");
    }

    private static boolean isUuid(String s) {
        return s != null && s.matches(
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    }

    private void logAcceso(String email, String evento, String detalle) {
        accesoAuditoriaRepository.save(AccesoAuditoria.builder()
                .email(email != null ? email : "desconocido")
                .evento(evento)
                .ip(null)
                .detalle(detalle)
                .fecha(Instant.now())
                .build());
    }
}
