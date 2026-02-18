/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.SyncDeduplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public interface SyncDeduplicationRepository extends JpaRepository<SyncDeduplication, Long> {

    Optional<SyncDeduplication> findByClientId(String clientId);

    boolean existsByClientId(String clientId);

    /** Chequeo en lote: devuelve clientIds que ya existen */
    @Query("SELECT d.clientId FROM SyncDeduplication d WHERE d.clientId IN :clientIds")
    Set<String> findExistingClientIds(@Param("clientIds") Set<String> clientIds);

    /** Limpiar registros antiguos (retención configurable) */
    @Modifying
    @Query("DELETE FROM SyncDeduplication d WHERE d.processedAt < :before")
    int deleteOlderThan(@Param("before") Instant before);
}
