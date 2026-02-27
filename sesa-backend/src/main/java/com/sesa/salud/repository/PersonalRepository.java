/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.Personal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import java.util.Optional;

public interface PersonalRepository extends JpaRepository<Personal, Long> {

    Optional<Personal> findByUsuario_Id(Long usuarioId);

    Optional<Personal> findByIdentificacion(String identificacion);

    /** Carga Personal con Usuario (eager) para evitar LazyInitializationException fuera de sesión */
    @Query("SELECT p FROM Personal p LEFT JOIN FETCH p.usuario WHERE p.identificacion = :identificacion")
    Optional<Personal> findByIdentificacionWithUsuario(@Param("identificacion") String identificacion);

    Page<Personal> findByActivoTrue(Pageable pageable);

    Page<Personal> findByNombresContainingIgnoreCaseOrApellidosContainingIgnoreCase(
            String nombres, String apellidos, Pageable pageable);

    /** Obtiene el personal activo con alguno de los roles indicados (para filtro de profesionales). */
    @Query("SELECT p FROM Personal p WHERE p.activo = true AND p.rol IN :roles ORDER BY p.nombres")
    List<Personal> findByRolIn(@Param("roles") java.util.List<String> roles);
}
