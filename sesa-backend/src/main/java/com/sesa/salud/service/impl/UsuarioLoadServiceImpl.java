/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service.impl;

import com.sesa.salud.entity.Usuario;
import com.sesa.salud.repository.UsuarioRepository;
import com.sesa.salud.service.UsuarioLoadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsuarioLoadServiceImpl implements UsuarioLoadService {

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<Usuario> loadByEmailInCurrentTenant(String email) {
        return usuarioRepository.findByEmail(email);
    }
}
