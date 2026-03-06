/**
 * Repositorio EBS: microterritorios.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.EbsTerritory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EbsTerritoryRepository extends JpaRepository<EbsTerritory, Long> {

    List<EbsTerritory> findByActiveTrueOrderByName();

    Optional<EbsTerritory> findByCode(String code);
}
