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

import java.util.Optional;

public interface PersonalRepository extends JpaRepository<Personal, Long> {

    Optional<Personal> findByUsuario_Id(Long usuarioId);

    Optional<Personal> findByIdentificacion(String identificacion);

    /** Carga Personal con Usuario (eager) para evitar LazyInitializationException fuera de sesión */
    @Query("SELECT p FROM Personal p LEFT JOIN FETCH p.usuario WHERE p.identificacion = :identificacion")
    Optional<Personal> findByIdentificacionWithUsuario(@Param("identificacion") String identificacion);

    Page<Personal> findByActivoTrue(Pageable pageable);

    Page<Personal> findByNombresContainingIgnoreCaseOrApellidosContainingIgnoreCaseOrCargoContainingIgnoreCase(
            String nombres, String apellidos, String cargo, Pageable pageable);
}
