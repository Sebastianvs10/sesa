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
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioDto> findAll(Pageable pageable) {
        return usuarioRepository.findAll(pageable).map(this::toDto);
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
        Set<String> roles = dto.getRoles() != null && !dto.getRoles().isEmpty()
                ? new HashSet<>(dto.getRoles())
                : Set.of("USER");
        Usuario usuario = Usuario.builder()
                .email(dto.getEmail().trim().toLowerCase())
                .nombreCompleto(dto.getNombreCompleto())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .activo(dto.getActivo() != null ? dto.getActivo() : true)
                .roles(roles)
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
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            String normalized = dto.getEmail().trim().toLowerCase();
            if (!normalized.equals(usuario.getEmail()) && usuarioRepository.existsByEmail(normalized)) {
                throw new RuntimeException("Ya existe un usuario con ese email");
            }
            usuario.setEmail(normalized);
        }
        if (dto.getNombreCompleto() != null) usuario.setNombreCompleto(dto.getNombreCompleto());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }
        if (dto.getActivo() != null) usuario.setActivo(dto.getActivo());
        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            usuario.setRoles(new HashSet<>(dto.getRoles()));
        }
        return toDto(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new RuntimeException("Usuario no encontrado: " + id);
        }
        usuarioRepository.deleteById(id);
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
