/**
 * S12: Repositorio API Keys para integradores.
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.repository;

import com.sesa.salud.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    List<ApiKey> findAllByOrderByCreatedAtDesc();

    Optional<ApiKey> findByApiKeyIndexAndActivoTrue(String apiKeyIndex);
}
