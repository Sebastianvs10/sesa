/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtra los intentos de login para prevenir ataques de fuerza bruta.
 * Implementación de ventana deslizante sin dependencias externas:
 * máximo {@link #MAX_ATTEMPTS} intentos por IP dentro de {@link #WINDOW_MS} ms.
 *
 * Nota: el mapa de IPs vive en memoria de la instancia JVM.
 * En despliegues multi-instancia usar Redis para compartir el estado.
 *
 * Registrado como filtro del contenedor en SecurityConfig (no en la cadena de Spring Security)
 * para evitar el requisito de orden registrado en 6.2+.
 */
@Component
@Slf4j
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final int  MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS    = 60_000L; // 1 minuto
    private static final String LOGIN_SUFFIX = "/auth/login";

    /** IP → [intentos, inicio-de-ventana-ms] */
    private final ConcurrentHashMap<String, long[]> windowMap = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        if (!isLoginRequest(request)) {
            chain.doFilter(request, response);
            return;
        }

        String ip = resolveClientIp(request);

        if (isBlocked(ip)) {
            long waitSec = remainingWindowSeconds(ip);
            log.warn("Rate limit de login superado para IP: {} — esperar {} s", ip, waitSec);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("Retry-After", String.valueOf(waitSec));
            response.getWriter().write(
                    "{\"error\":\"Demasiados intentos. Intenta de nuevo en " + waitSec + " segundos.\",\"status\":429}"
            );
            return;
        }

        chain.doFilter(request, response);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean isLoginRequest(HttpServletRequest req) {
        return "POST".equalsIgnoreCase(req.getMethod())
                && req.getRequestURI().endsWith(LOGIN_SUFFIX);
    }

    /**
     * Extrae la IP real respetando proxies inversos (X-Forwarded-For).
     * Solo se confía en el primer valor para evitar spoofing.
     */
    private String resolveClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    /**
     * Registra el intento y devuelve true si la IP superó el límite.
     * Reinicia la ventana si ya expiró el período anterior.
     */
    private boolean isBlocked(String ip) {
        long now = System.currentTimeMillis();
        long[] window = windowMap.compute(ip, (k, v) -> {
            if (v == null || now - v[1] > WINDOW_MS) {
                // Nueva ventana
                return new long[]{1L, now};
            }
            v[0]++;
            return v;
        });
        return window[0] > MAX_ATTEMPTS;
    }

    private long remainingWindowSeconds(String ip) {
        long[] window = windowMap.get(ip);
        if (window == null) return 0;
        long elapsed = System.currentTimeMillis() - window[1];
        long remaining = WINDOW_MS - elapsed;
        return Math.max(0, remaining / 1000);
    }
}
