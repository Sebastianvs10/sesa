package com.sesa.salud.security;

import com.sesa.salud.tenant.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Autor: Ing. J Sebastian Vargas S
 * Establece el tenant (esquema) para cada petición.
 * - En /auth/login: usa public para buscar en tenant_usuario_login y validar credenciales.
 * - En el resto: extrae el schema del JWT (donde se guardó en el login) y lo usa para toda la petición.
 * Así, un usuario administrador de un esquema distinto a public siempre opera sobre su tenant.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/auth/login";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String path = request.getRequestURI();
            boolean isLogin = path != null && path.contains(LOGIN_PATH);
            if (isLogin) {
                TenantContextHolder.setTenantSchema(TenantContextHolder.PUBLIC);
            } else {
                // S12: Si ApiKeyAuthenticationFilter ya estableció el tenant, no sobrescribir
                String apiKeyTenant = (String) request.getAttribute("tenantSchema");
                if (apiKeyTenant != null) {
                    TenantContextHolder.setTenantSchema(apiKeyTenant);
                    log.trace("Tenant desde API Key: {}", apiKeyTenant);
                } else {
                    String jwt = getJwtFromRequest(request);
                    if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                        String schema = jwtTokenProvider.getSchemaFromToken(jwt);
                        TenantContextHolder.setTenantSchema(schema != null ? schema : TenantContextHolder.PUBLIC);
                        log.trace("Tenant para petición: {}", TenantContextHolder.getTenantSchema());
                    } else {
                        TenantContextHolder.setTenantSchema(TenantContextHolder.PUBLIC);
                    }
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
