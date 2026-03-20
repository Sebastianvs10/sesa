/**
 * S12: Filtro de autenticación por API Key para /api/integracion/**.
 * Lee X-API-Key y X-Tenant-Schema, valida la clave y establece SecurityContext y tenant.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.security;

import com.sesa.salud.entity.ApiKey;
import com.sesa.salud.tenant.TenantContextHolder;
import com.sesa.salud.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter implements Ordered {

    private static final int ORDER = 0;
    private static final String INTEGRACION_PATH = "/api/integracion/";
    private static final String HEADER_API_KEY = "X-API-Key";
    private static final String HEADER_TENANT = "X-Tenant-Schema";
    private static final String ROLE_INTEGRADOR = "ROLE_INTEGRADOR";

    private final ApiKeyService apiKeyService;

    @Override
    public int getOrder() { return ORDER; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.contains(INTEGRACION_PATH);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String rawKey = request.getHeader(HEADER_API_KEY);
        String tenantSchema = request.getHeader(HEADER_TENANT);
        if (!StringUtils.hasText(rawKey) || !StringUtils.hasText(tenantSchema)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"X-API-Key y X-Tenant-Schema son obligatorios\",\"status\":401}");
            return;
        }
        tenantSchema = tenantSchema.trim();
        TenantContextHolder.setTenantSchema(tenantSchema);
        request.setAttribute("tenantSchema", tenantSchema);
        try {
            ApiKey apiKey = apiKeyService.validar(rawKey.trim());
            if (apiKey == null) {
                request.removeAttribute("tenantSchema");
                TenantContextHolder.clear();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"API Key inválida o inactiva\",\"status\":401}");
                return;
            }
            List<String> perms = Arrays.stream(apiKey.getPermisos().split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).toList();
            ApiKeyPrincipal principal = new ApiKeyPrincipal(
                    apiKey.getNombreIntegrador(), tenantSchema, perms.stream().collect(Collectors.toSet()));
            List<GrantedAuthority> authorities = perms.stream()
                    .map(p -> (GrantedAuthority) new SimpleGrantedAuthority("PERMISO_" + p))
                    .collect(Collectors.toList());
            authorities.add(new SimpleGrantedAuthority(ROLE_INTEGRADOR));
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);
        } finally {
            // No clear tenant aquí; TenantFilter lo hará al final de la petición
        }
    }
}
