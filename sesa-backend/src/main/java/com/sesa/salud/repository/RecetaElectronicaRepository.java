/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.repository;

import com.sesa.salud.entity.RecetaElectronica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecetaElectronicaRepository extends JpaRepository<RecetaElectronica, Long> {
    Optional<RecetaElectronica> findByTokenVerificacion(String tokenVerificacion);
}
