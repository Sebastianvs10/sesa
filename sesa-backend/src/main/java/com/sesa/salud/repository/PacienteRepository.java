/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.Paciente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    boolean existsByDocumento(String documento);

    Optional<Paciente> findByDocumento(String documento);

    Page<Paciente> findByActivoTrue(Pageable pageable);

    Page<Paciente> findByActivo(Boolean activo, Pageable pageable);

    Page<Paciente> findByNombresContainingIgnoreCaseOrApellidosContainingIgnoreCaseOrDocumentoContaining(
            String nombres, String apellidos, String documento, Pageable pageable);

    java.util.Optional<Paciente> findByUsuarioId(Long usuarioId);
}
