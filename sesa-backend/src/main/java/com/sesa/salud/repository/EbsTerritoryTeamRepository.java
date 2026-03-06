/**
 * Repositorio EBS: equipo por territorio.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.EbsTerritoryTeam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EbsTerritoryTeamRepository extends JpaRepository<EbsTerritoryTeam, EbsTerritoryTeam.TerritoryTeamId> {

    List<EbsTerritoryTeam> findByTerritory_Id(Long territoryId);

    Optional<EbsTerritoryTeam> findByTerritory_IdAndPersonal_Id(Long territoryId, Long personalId);

    void deleteByTerritory_Id(Long territoryId);
}
