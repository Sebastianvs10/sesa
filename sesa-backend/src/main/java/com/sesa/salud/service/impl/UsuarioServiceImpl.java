/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.service.impl;

import com.sesa.salud.dto.UsuarioDto;
import com.sesa.salud.dto.UsuarioRequestDto;
import com.sesa.salud.entity.Usuario;
import com.sesa.salud.repository.UsuarioRepository;
import com.sesa.salud.event.email.NewUserWelcomeEmailEvent;
import com.sesa.salud.service.UsuarioService;
import com.sesa.salud.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    /** Roles permitidos en el módulo Usuarios Adm (el personal clínico va en /personal). */
    private static final Set<String> ADM_SCOPE_ROLES = Set.of("ADMIN", "SUPERADMINISTRADOR");

    private final UsuarioRepository usuarioRepository;
    private final TenantUsuarioLoginSyncService tenantUsuarioLoginSyncService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioDto> findAll(Pageable pageable) {
        return usuarioRepository.findWithAdministrativeRole(pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDto findById(Long id) {
        return toDto(usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + id)));
    }

    @Override
    @Transactional
    public UsuarioDto create(UsuarioRequestDto dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Ya existe un usuario con ese email");
        }
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new RuntimeException("La contraseña es obligatoria para crear usuario");
        }
        Authentication auth = requireAuthenticated();
        Set<String> normalized = normalizeAdministrativeRoles(dto.getRoles());
        validateAdministrativeAssignment(null, normalized, auth);
        Usuario usuario = Usuario.builder()
                .email(dto.getEmail().trim().toLowerCase(Locale.ROOT))
                .nombreCompleto(dto.getNombreCompleto())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .activo(dto.getActivo() != null ? dto.getActivo() : true)
                .roles(normalized)
                .build();
        Usuario saved = usuarioRepository.save(usuario);
        eventPublisher.publishEvent(
                new NewUserWelcomeEmailEvent(saved.getEmail(), saved.getNombreCompleto(), null));
        return toDto(saved);
    }

    @Override
    @Transactional
    public UsuarioDto update(Long id, UsuarioRequestDto dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + id));
        Authentication auth = requireAuthenticated();
        Set<String> effectiveRoles;
        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            effectiveRoles = normalizeAdministrativeRoles(dto.getRoles());
        } else {
            if (usuario.getRoles() == null || usuario.getRoles().isEmpty()) {
                throw new IllegalArgumentException(
                        "La cuenta debe conservar al menos un rol ADMIN o SUPERADMINISTRADOR");
            }
            effectiveRoles = normalizeAdministrativeRoles(new HashSet<>(usuario.getRoles()));
        }
        validateAdministrativeAssignment(usuario, effectiveRoles, auth);
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            String normalized = dto.getEmail().trim().toLowerCase(Locale.ROOT);
            if (!normalized.equals(usuario.getEmail())) {
                if (usuarioRepository.existsByEmailAndIdNot(normalized, usuario.getId())) {
                    throw new RuntimeException("Ya existe un usuario con ese email");
                }
                tenantUsuarioLoginSyncService.renameLoginEmail(
                        usuario.getEmail(), normalized, TenantContextHolder.getTenantSchema());
                usuario.setEmail(normalized);
            }
        }
        if (dto.getNombreCompleto() != null) usuario.setNombreCompleto(dto.getNombreCompleto());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }
        if (dto.getActivo() != null) usuario.setActivo(dto.getActivo());
        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            usuario.setRoles(effectiveRoles);
        }
        return toDto(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        Usuario target = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + id));
        Authentication auth = requireAuthenticated();
        assertCanDeleteAdministrativeUser(target, auth);
        usuarioRepository.deleteById(id);
    }

    private static Authentication requireAuthenticated() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated()) {
            throw new IllegalArgumentException("Sesión no válida");
        }
        return a;
    }

    private static boolean hasRole(Authentication auth, String rolSinPrefijo) {
        String expected = "ROLE_" + rolSinPrefijo;
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (expected.equals(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSuperAdminCaller(Authentication auth) {
        return hasRole(auth, "SUPERADMINISTRADOR");
    }

    private static boolean isTenantAdminCaller(Authentication auth) {
        return hasRole(auth, "ADMIN");
    }

    /**
     * Normaliza a mayúsculas y valida que solo existan roles de alcance administrativo.
     */
    private static Set<String> normalizeAdministrativeRoles(Set<String> raw) {
        if (raw == null || raw.isEmpty()) {
            throw new IllegalArgumentException(
                    "Debe indicar al menos un rol ADMIN o SUPERADMINISTRADOR. El personal clínico se crea en Gestión de personal.");
        }
        Set<String> out = new HashSet<>();
        for (String r : raw) {
            if (r == null || r.isBlank()) {
                continue;
            }
            String u = r.trim().toUpperCase(Locale.ROOT);
            if (!ADM_SCOPE_ROLES.contains(u)) {
                throw new IllegalArgumentException(
                        "Solo se permiten los roles ADMIN y SUPERADMINISTRADOR. El personal clínico se crea en Gestión de personal.");
            }
            out.add(u);
        }
        if (out.isEmpty()) {
            throw new IllegalArgumentException(
                    "Debe indicar al menos un rol ADMIN o SUPERADMINISTRADOR. El personal clínico se crea en Gestión de personal.");
        }
        return out;
    }

    /**
     * @param targetUsuario null en creación; no null en actualización
     */
    private static void validateAdministrativeAssignment(Usuario targetUsuario, Set<String> normalizedRoles,
            Authentication auth) {
        if (!isSuperAdminCaller(auth) && !isTenantAdminCaller(auth)) {
            throw new IllegalArgumentException("No autorizado para gestionar usuarios administrativos");
        }
        if (!isSuperAdminCaller(auth)) {
            if (normalizedRoles.contains("SUPERADMINISTRADOR")) {
                throw new IllegalArgumentException("Solo un superadministrador puede asignar el rol SUPERADMINISTRADOR");
            }
            if (targetUsuario != null && targetUsuario.getRoles() != null
                    && targetUsuario.getRoles().contains("SUPERADMINISTRADOR")) {
                throw new IllegalArgumentException(
                        "Solo un superadministrador puede modificar cuentas de superadministrador");
            }
        }
    }

    private static void assertCanDeleteAdministrativeUser(Usuario target, Authentication auth) {
        if (!isSuperAdminCaller(auth) && !isTenantAdminCaller(auth)) {
            throw new IllegalArgumentException("No autorizado para eliminar este usuario");
        }
        if (!isSuperAdminCaller(auth) && target.getRoles() != null
                && target.getRoles().contains("SUPERADMINISTRADOR")) {
            throw new IllegalArgumentException("Solo un superadministrador puede eliminar cuentas de superadministrador");
        }
    }

    private UsuarioDto toDto(Usuario u) {
        return UsuarioDto.builder()
                .id(u.getId())
                .email(u.getEmail())
                .nombreCompleto(u.getNombreCompleto())
                .activo(u.getActivo())
                .roles(u.getRoles())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
