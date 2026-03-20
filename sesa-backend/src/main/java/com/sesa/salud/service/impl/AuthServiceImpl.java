/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.sesa.salud.dto.auth.AccesoAuditoriaDto;
import com.sesa.salud.dto.auth.LoginRequest;
import com.sesa.salud.dto.auth.LoginResponse;
import com.sesa.salud.dto.auth.PasswordResetRequestResponse;
import com.sesa.salud.entity.AccesoAuditoria;
import com.sesa.salud.entity.Usuario;
import com.sesa.salud.entity.master.PasswordResetTokenPublic;
import com.sesa.salud.exception.PasswordResetException;
import com.sesa.salud.repository.AccesoAuditoriaRepository;
import com.sesa.salud.repository.PersonalRepository;
import com.sesa.salud.repository.UsuarioRepository;
import com.sesa.salud.repository.master.EmpresaRepository;
import com.sesa.salud.repository.master.PasswordResetTokenPublicRepository;
import com.sesa.salud.repository.master.TenantUsuarioLoginRepository;
import com.sesa.salud.security.JwtTokenProvider;
import com.sesa.salud.service.AuthService;
import com.sesa.salud.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final String RESET_OK_MESSAGE = "Si el correo está registrado en SESA, recibirás instrucciones para restablecer tu contraseña.";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final PersonalRepository personalRepository;
    private final TenantUsuarioLoginRepository tenantUsuarioLoginRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AccesoAuditoriaRepository accesoAuditoriaRepository;
    private final PasswordResetTokenPublicRepository passwordResetTokenPublicRepository;

    @Value("${sesa.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${sesa.auth.password-reset.expose-token:false}")
    private boolean exposePasswordResetToken;

    @Value("${sesa.auth.password-reset.token-ttl-minutes:30}")
    private long passwordResetTokenTtlMinutes;

    @Value("${sesa.auth.password-reset.min-processing-ms:400}")
    private long passwordResetMinProcessingMs;

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
    @Transactional
    public PasswordResetRequestResponse requestPasswordReset(String email) {
        long start = System.nanoTime();
        String normalized = email == null ? "" : email.trim().toLowerCase();
        String issuedToken = null;

        try {
            if (normalized.isEmpty()) {
                return buildResetRequestResponse(null);
            }

            var tenantOpt = tenantUsuarioLoginRepository.findByEmail(normalized);
            if (tenantOpt.isEmpty()) {
                log.debug("Recuperación: correo sin registro de tenant (respuesta uniforme)");
                return buildResetRequestResponse(null);
            }

            String schema = tenantOpt.get().getSchemaName();
            try {
                TenantContextHolder.setTenantSchema(schema);
                var usuarioOpt = usuarioRepository.findByEmail(normalized);
                if (usuarioOpt.isEmpty() || !Boolean.TRUE.equals(usuarioOpt.get().getActivo())) {
                    log.debug("Recuperación: usuario inexistente o inactivo en tenant (respuesta uniforme)");
                    return buildResetRequestResponse(null);
                }
            } finally {
                TenantContextHolder.clear();
            }

            passwordResetTokenPublicRepository.deletePendingByEmail(normalized);
            issuedToken = generateSecureToken();
            long ttlMinutes = Math.max(5, Math.min(passwordResetTokenTtlMinutes, 120));
            PasswordResetTokenPublic entity = PasswordResetTokenPublic.builder()
                    .email(normalized)
                    .tenantSchema(schema)
                    .token(issuedToken)
                    .expiraEn(Instant.now().plusSeconds(ttlMinutes * 60))
                    .usado(false)
                    .build();
            passwordResetTokenPublicRepository.save(entity);
            logAcceso(normalized, "RESET_REQUEST", "Solicitud de recuperación (token en public)");
            log.info("Recuperación: token emitido para tenant {}", schema);
        } catch (Exception ex) {
            log.error("Recuperación: error interno al generar token: {}", ex.getMessage());
            issuedToken = null;
        } finally {
            ensureMinProcessingTime(start);
        }

        return buildResetRequestResponse(issuedToken);
    }

    private PasswordResetRequestResponse buildResetRequestResponse(String token) {
        PasswordResetRequestResponse.PasswordResetRequestResponseBuilder b = PasswordResetRequestResponse.builder()
                .message(RESET_OK_MESSAGE);
        if (exposePasswordResetToken && token != null) {
            b.devToken(token);
        }
        return b.build();
    }

    private void ensureMinProcessingTime(long startNanos) {
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
        long wait = passwordResetMinProcessingMs - elapsedMs;
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        String rawToken = token == null ? "" : token.trim();
        if (rawToken.isEmpty()) {
            throw new PasswordResetException(HttpStatus.BAD_REQUEST, "Código de verificación inválido.");
        }

        PasswordResetTokenPublic resetRow = passwordResetTokenPublicRepository.findByToken(rawToken)
                .orElseThrow(() -> new PasswordResetException(
                        HttpStatus.BAD_REQUEST,
                        "El código no es válido o ya fue utilizado. Solicita uno nuevo."));

        if (Boolean.TRUE.equals(resetRow.getUsado()) || resetRow.getExpiraEn().isBefore(Instant.now())) {
            throw new PasswordResetException(
                    HttpStatus.BAD_REQUEST,
                    "El código expiró o ya fue usado. Solicita una nueva recuperación.");
        }

        String email = resetRow.getEmail();
        String schema = resetRow.getTenantSchema();
        if (schema == null || schema.isBlank()) {
            throw new PasswordResetException(
                    HttpStatus.BAD_REQUEST, "No se pudo completar la operación.");
        }

        try {
            TenantContextHolder.setTenantSchema(schema);
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new PasswordResetException(
                            HttpStatus.BAD_REQUEST, "No se pudo completar la operación."));
            usuario.setPasswordHash(passwordEncoder.encode(newPassword));
            usuarioRepository.save(usuario);
            resetRow.setUsado(true);
            passwordResetTokenPublicRepository.save(resetRow);
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
