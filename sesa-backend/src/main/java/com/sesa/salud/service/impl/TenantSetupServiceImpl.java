/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.sesa.salud.dto.EmpresaCreateRequest;
import com.sesa.salud.entity.Usuario;
import com.sesa.salud.repository.UsuarioRepository;
import com.sesa.salud.service.TenantSetupService;
import com.sesa.salud.tenant.TenantContextHolder;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.SQLException;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantSetupServiceImpl implements TenantSetupService {

    private final EntityManager entityManager;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public String createTenantSchemaAndAdmin(String schemaName, EmpresaCreateRequest request) {
        TenantContextHolder.setTenantSchema(schemaName);
        // Usar la MISMA conexión de la transacción JPA para script y save (evita que el save vaya a public)
        Session session = entityManager.unwrap(Session.class);
        session.doWork(conn -> {
            try {
                conn.setSchema(schemaName);
                ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/tenant_schema.sql"));
            } catch (SQLException e) {
                log.error("Error estableciendo schema {} o ejecutando script", schemaName, e);
                throw new RuntimeException("Error creando tablas del tenant: " + e.getMessage());
            } catch (Exception e) {
                log.error("Error ejecutando script tenant {}", schemaName, e);
                throw new RuntimeException("Error creando tablas del tenant: " + e.getMessage());
            }
        });
        String nombreCompleto = buildNombreCompleto(request.getAdminUser());
        Usuario admin = Usuario.builder()
                .email(request.getAdminUser().getCorreo().trim())
                .passwordHash(passwordEncoder.encode(request.getAdminUser().getContraseña()))
                .nombreCompleto(nombreCompleto)
                .activo(true)
                .roles(java.util.Set.of("ADMIN"))
                .build();
        admin = usuarioRepository.save(admin);
        entityManager.flush();
        return admin.getEmail();
    }

    private static String buildNombreCompleto(EmpresaCreateRequest.AdminUserRequest admin) {
        StringBuilder sb = new StringBuilder();
        if (admin.getPrimerNombre() != null) sb.append(admin.getPrimerNombre());
        if (admin.getSegundoNombre() != null && !admin.getSegundoNombre().isBlank()) sb.append(" ").append(admin.getSegundoNombre());
        if (admin.getPrimerApellido() != null) sb.append(" ").append(admin.getPrimerApellido());
        if (admin.getSegundoApellido() != null && !admin.getSegundoApellido().isBlank()) sb.append(" ").append(admin.getSegundoApellido());
        return sb.toString().trim();
    }
}
