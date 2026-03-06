/**
 * Repositorio EBS: hogares por territorio.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.EbsHousehold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EbsHouseholdRepository extends JpaRepository<EbsHousehold, Long> {

    List<EbsHousehold> findByTerritoryIdOrderByAddressText(Long territoryId);

    @Query("SELECT h FROM EbsHousehold h WHERE h.territory.id = :territoryId " +
           "AND (:riskLevel IS NULL OR :riskLevel = '' OR h.riskLevel = :riskLevel) " +
           "AND (:visitStatus IS NULL OR :visitStatus = '' OR h.state = :visitStatus) " +
           "ORDER BY h.addressText")
    List<EbsHousehold> findByTerritoryIdAndFilters(
        @Param("territoryId") Long territoryId,
        @Param("riskLevel") String riskLevel,
        @Param("visitStatus") String visitStatus
    );

    long countByTerritoryId(Long territoryId);

    long countByTerritoryIdAndState(Long territoryId, String state);

    @Query("SELECT COUNT(DISTINCT h.id) FROM EbsHousehold h WHERE h.territory.id = :territoryId AND " +
           "EXISTS (SELECT 1 FROM EbsHomeVisit v WHERE v.household.id = h.id)")
    long countVisitedHouseholdsByTerritoryId(@Param("territoryId") Long territoryId);

    long countByTerritoryIdAndRiskLevelIn(Long territoryId, List<String> riskLevels);

    long countByTerritoryIdAndRiskLevel(Long territoryId, String riskLevel);
}
