/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.security;

import com.sesa.salud.entity.Usuario;
import com.sesa.salud.repository.master.TenantUsuarioLoginRepository;
import com.sesa.salud.service.UsuarioLoadService;
import com.sesa.salud.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Carga el usuario para validar el JWT. Busca primero en el esquema de la empresa
 * en la que está logueado (tenant del JWT); si no está ahí, intenta por tenant_usuario_login.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final TenantUsuarioLoginRepository tenantUsuarioLoginRepository;
    private final UsuarioLoadService usuarioLoadService;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String tenantOriginal = TenantContextHolder.getTenantSchema();
        try {
            // 1) Primero buscar en el esquema de la empresa en la que está logueado (JWT). Nueva transacción = conexión con ese schema.
            Usuario usuario = usuarioLoadService.loadByEmailInCurrentTenant(username).orElse(null);
            if (usuario != null) {
                return buildUserDetails(usuario);
            }
            // 2) Fallback: usuario en tenant_usuario_login (public) y cargar desde ese esquema con nueva conexión
            TenantContextHolder.setTenantSchema(TenantContextHolder.PUBLIC);
            var optLogin = tenantUsuarioLoginRepository.findByEmail(username);
            if (optLogin.isPresent()) {
                TenantContextHolder.setTenantSchema(optLogin.get().getSchemaName());
                usuario = usuarioLoadService.loadByEmailInCurrentTenant(username)
                        .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
                return buildUserDetails(usuario);
            }
            throw new UsernameNotFoundException("Usuario no encontrado: " + username);
        } finally {
            TenantContextHolder.setTenantSchema(tenantOriginal);
        }
    }

    private UserDetails buildUserDetails(Usuario usuario) {
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new UsernameNotFoundException("Usuario inactivo: " + usuario.getEmail());
        }
        var roles = usuario.getRoles();
        var authorities = (roles != null && !roles.isEmpty() ? roles.stream() : Stream.<String>empty())
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .collect(Collectors.toList());
        if (authorities.isEmpty() && "admin@sesa.local".equalsIgnoreCase(usuario.getEmail())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return new User(
                usuario.getEmail(),
                usuario.getPasswordHash(),
                authorities
        );
    }
}
