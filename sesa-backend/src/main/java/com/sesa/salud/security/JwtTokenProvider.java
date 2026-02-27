/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${sesa.jwt.secret:dev-secret-key-only-for-development-minimum-256-bits-dev-secret-key-only-for-development}")
    private String jwtSecret;

    @Value("${sesa.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Genera un JWT con múltiples roles.
     * El claim "roles" es un array JSON; "role" conserva el rol primario (SUPERADMINISTRADOR
     * si está presente, o el primero de la colección) para retrocompatibilidad con
     * tokens existentes y con @PreAuthorize que usa la authority ROLE_X.
     */
    public String generateToken(String username, Long userId, Collection<String> roles, String schema) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        String primaryRole = resolvePrimaryRole(roles);

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("role",  primaryRole)
                .claim("roles", new ArrayList<>(roles))
                .claim("schema", schema != null ? schema : "public")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /** Backward-compat: acepta rol único y lo envuelve en colección. */
    public String generateToken(String username, Long userId, String role, String schema) {
        return generateToken(username, userId,
                role != null ? List.of(role) : List.of(),
                schema);
    }

    public String getUsernameFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public String getSchemaFromToken(String token) {
        String schema = getClaims(token).get("schema", String.class);
        return schema != null ? schema : "public";
    }

    /** Devuelve el rol primario (primer elemento o SUPERADMINISTRADOR si está presente). */
    public String getRoleFromToken(String token) {
        Claims claims = getClaims(token);
        String primary = claims.get("role", String.class);
        if (primary != null) return primary;
        // Fallback: leer del array "roles"
        Set<String> roles = getRolesFromToken(token);
        return resolvePrimaryRole(roles);
    }

    /** Devuelve todos los roles del JWT como Set<String>. */
    @SuppressWarnings("unchecked")
    public Set<String> getRolesFromToken(String token) {
        Claims claims = getClaims(token);
        Object raw = claims.get("roles");
        if (raw instanceof List<?> list) {
            Set<String> result = new LinkedHashSet<>();
            for (Object item : list) {
                if (item instanceof String s) result.add(s);
            }
            return result;
        }
        // Fallback: JWT antiguo con un único "role"
        String single = claims.get("role", String.class);
        return single != null ? Set.of(single) : Set.of();
    }

    public Long getUserIdFromToken(String token) {
        Object val = getClaims(token).get("userId");
        if (val instanceof Number) return ((Number) val).longValue();
        return null;
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (SignatureException e) {
            log.debug("Firma JWT inválida");
        } catch (MalformedJwtException e) {
            log.debug("Token JWT inválido");
        } catch (ExpiredJwtException e) {
            log.debug("Token JWT expirado");
        } catch (IllegalArgumentException e) {
            log.debug("JWT claims vacío");
        }
        return false;
    }

    /**
     * Rol primario: SUPERADMINISTRADOR > ADMIN > primero en la colección.
     */
    private String resolvePrimaryRole(Collection<String> roles) {
        if (roles == null || roles.isEmpty()) return "USER";
        if (roles.contains("SUPERADMINISTRADOR")) return "SUPERADMINISTRADOR";
        if (roles.contains("ADMIN")) return "ADMIN";
        return roles.iterator().next();
    }
}
