/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.config;

import com.sesa.salud.security.JwtAuthenticationFilter;
import com.sesa.salud.security.LoginRateLimitFilter;
import com.sesa.salud.security.TenantFilter;
import com.sesa.salud.security.ApiKeyAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final TenantFilter tenantFilter;
    private final LoginRateLimitFilter loginRateLimitFilter;
    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @Value("${sesa.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    /**
     * Registra filtros custom fuera de la cadena de Spring Security para evitar
     * "Filter does not have a registered order" en 6.2+. Se ejecutan antes de la cadena.
     */
    @Bean
    public FilterRegistrationBean<LoginRateLimitFilter> loginRateLimitFilterRegistration() {
        FilterRegistrationBean<LoginRateLimitFilter> reg = new FilterRegistrationBean<>(loginRateLimitFilter);
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        reg.addUrlPatterns("/*");
        return reg;
    }

    @Bean
    public FilterRegistrationBean<ApiKeyAuthenticationFilter> apiKeyFilterRegistration() {
        FilterRegistrationBean<ApiKeyAuthenticationFilter> reg = new FilterRegistrationBean<>(apiKeyAuthenticationFilter);
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        reg.addUrlPatterns("/*");
        return reg;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .headers(headers -> {
                        // Evita que la app sea embebida en iframes (clickjacking)
                        headers.frameOptions(frameOptions -> frameOptions.deny());
                        // Previene MIME-sniffing (scripts disfrazados de imagen, etc.)
                        headers.contentTypeOptions(opt -> {});
                        // HSTS: forzar HTTPS durante 2 años, incluyendo subdominios
                        headers.httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(63072000)
                                .preload(true));
                        // Política de referrer: no filtrar URLs internas al salir del sitio
                        headers.referrerPolicy(ref -> ref.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                        // Restringir APIs de hardware (cámara, mic, geolocalización, pago, USB)
                        headers.permissionsPolicy(perms -> perms.policy(
                                "camera=(), microphone=(), geolocation=(), payment=(), usb=(), interest-cohort=()"));
                        // Content Security Policy: solo permite cargas del mismo origen
                        headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; " +
                                "img-src 'self' data: blob:; " +
                                "style-src 'self' 'unsafe-inline'; " +
                                "script-src 'self'; " +
                                "connect-src 'self' https://fhir.minsalud.gov.co; " +
                                "font-src 'self' data:; " +
                                "frame-ancestors 'none'; " +
                                "base-uri 'self'; " +
                                "form-action 'self'"));
                        // X-XSS-Protection para navegadores legacy
                        headers.xssProtection(xss -> xss
                                .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK));
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/actuator/health", "/sync/status").permitAll()
                        .requestMatchers(HttpMethod.GET, "/cita/confirmar").permitAll()
                        .requestMatchers(HttpMethod.GET, "/cita/cancelar").permitAll()
                        .requestMatchers(HttpMethod.POST, "/cita/cancelar").permitAll()
                        .requestMatchers(HttpMethod.GET, "/recetas/verificar/**").permitAll()
                        // Videoconsulta: unirse por enlace y señalización sin login (enlace compartido)
                        .requestMatchers(HttpMethod.GET, "/videoconsulta/salas/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/videoconsulta/salas/*/signaling").permitAll()
                        .requestMatchers(HttpMethod.GET, "/videoconsulta/salas/*/signaling").permitAll()
                        .requestMatchers(HttpMethod.POST, "/videoconsulta/salas/*/habilitar-asistente").permitAll()
                        .requestMatchers(HttpMethod.POST, "/videoconsulta/salas/*/consentimiento-asistente").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/videoconsulta/salas/*/notas").permitAll()
                        .requestMatchers(HttpMethod.GET, "/videoconsulta/salas/*/notas").permitAll()
                        .requestMatchers(HttpMethod.GET, "/videoconsulta/salas/*/validar-asistente").permitAll()
                        .requestMatchers(HttpMethod.GET, "/videoconsulta/salas/*/solicitud-asistente-pendiente").permitAll()
                        // GET /archivos/{uuid} es público; la validación de acceso privado
                        // se hace a nivel de negocio en ArchivoController con el flag acceso_publico
                        .requestMatchers(HttpMethod.GET, "/archivos/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/empresas/current", "/empresas/logo").authenticated()
                        .requestMatchers("/empresas", "/empresas/**").hasAnyRole("ADMIN", "SUPERADMINISTRADOR")
                        // S12: API integradores (autenticación por X-API-Key)
                        .requestMatchers("/api/integracion/**").authenticated()
                        // /roles/usuario-actual: accesible para cualquier usuario autenticado
                        .requestMatchers(HttpMethod.GET, "/roles/usuario-actual").authenticated()
                        // El resto de /roles/** solo lo gestiona el SUPERADMINISTRADOR
                        .requestMatchers("/roles", "/roles/**").hasRole("SUPERADMINISTRADOR")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        // 401 para peticiones sin autenticar (token ausente o expirado)
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\":\"No autenticado\",\"status\":401}");
                        })
                        // 403 para peticiones autenticadas pero sin permiso suficiente
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\":\"Acceso denegado\",\"status\":403}");
                        }))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(tenantFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Parsear origins desde variable de entorno (soporta múltiples separados por
        // coma)
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        origins.replaceAll(String::trim);
        config.setAllowedOrigins(origins);

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Solo permitir headers específicos (más seguro). X-Tenant-Schema para
        // multi-tenancy.
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With",
                "X-CSRF-Token",
                "X-Tenant-Schema",
                "X-API-Key"));

        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // Cache de preflight por 1 hora

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
