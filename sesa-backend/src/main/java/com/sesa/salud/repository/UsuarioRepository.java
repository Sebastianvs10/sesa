/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    List<Usuario> findByActivoTrue();

    /** Listado de gestión administrativa: solo cuentas con rol ADMIN o SUPERADMINISTRADOR. */
    @Query("""
            SELECT u FROM Usuario u
            WHERE 'ADMIN' MEMBER OF u.roles OR 'SUPERADMINISTRADOR' MEMBER OF u.roles
            """)
    Page<Usuario> findWithAdministrativeRole(Pageable pageable);
}
