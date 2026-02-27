/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.repository;

import com.sesa.salud.entity.Eps;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EpsRepository extends JpaRepository<Eps, Long> {
    List<Eps> findByActivoTrue();
    Optional<Eps> findByCodigo(String codigo);
}
