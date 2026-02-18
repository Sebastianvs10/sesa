/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.service;

import com.sesa.salud.entity.Usuario;

import java.util.Optional;

/**
 * Servicio para cargar usuario por email en el tenant actual.
 * Usado con REQUIRES_NEW para obtener una conexión con el schema correcto.
 */
public interface UsuarioLoadService {

    Optional<Usuario> loadByEmailInCurrentTenant(String email);
}
