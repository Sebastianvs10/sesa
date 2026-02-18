/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.repository;

import com.sesa.salud.entity.Eps;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EpsRepository extends JpaRepository<Eps, Long> {
    List<Eps> findByActivoTrue();
}
