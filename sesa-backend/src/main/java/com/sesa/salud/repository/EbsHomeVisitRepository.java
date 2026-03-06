/**
 * Repositorio EBS: visitas domiciliarias.
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.EbsHomeVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EbsHomeVisitRepository extends JpaRepository<EbsHomeVisit, Long> {

    List<EbsHomeVisit> findByHouseholdIdOrderByVisitDateDesc(Long householdId);

    @Query("SELECT v FROM EbsHomeVisit v WHERE v.household.id = :householdId ORDER BY v.visitDate DESC")
    List<EbsHomeVisit> findLatestByHousehold(@Param("householdId") Long householdId);

    @Query("SELECT MAX(v.visitDate) FROM EbsHomeVisit v WHERE v.household.id = :householdId")
    Optional<Instant> findLatestVisitDateByHouseholdId(@Param("householdId") Long householdId);

    @Query("SELECT COUNT(DISTINCT v.household.id) FROM EbsHomeVisit v WHERE v.household.territory.id = :territoryId")
    long countDistinctHouseholdsWithVisitsByTerritoryId(@Param("territoryId") Long territoryId);

    @Query("SELECT v FROM EbsHomeVisit v WHERE v.household.territory.id = :territoryId ORDER BY v.visitDate DESC")
    List<EbsHomeVisit> findByTerritoryIdOrderByVisitDateDesc(@Param("territoryId") Long territoryId);

    @Query("SELECT v FROM EbsHomeVisit v WHERE v.professional.id = :professionalId ORDER BY v.visitDate DESC")
    List<EbsHomeVisit> findByProfessionalIdOrderByVisitDateDesc(@Param("professionalId") Long professionalId);

    @Query("SELECT COUNT(v) FROM EbsHomeVisit v WHERE v.visitDate >= :from AND v.visitDate < :to")
    long countByVisitDateBetween(@Param("from") Instant from, @Param("to") Instant to);
}
