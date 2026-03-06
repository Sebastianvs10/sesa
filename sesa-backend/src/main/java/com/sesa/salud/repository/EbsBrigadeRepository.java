/**
 * Repositorio EBS: brigadas.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.EbsBrigade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EbsBrigadeRepository extends JpaRepository<EbsBrigade, Long> {

    List<EbsBrigade> findByTerritoryIdOrderByDateStartDesc(Long territoryId);

    List<EbsBrigade> findAllByOrderByDateStartDesc();
}
