/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.repository;

import com.sesa.salud.entity.FacturacionElectronicaConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FacturacionElectronicaConfigRepository extends JpaRepository<FacturacionElectronicaConfig, Long> {

    Optional<FacturacionElectronicaConfig> findTopByOrderByIdAsc();
}

