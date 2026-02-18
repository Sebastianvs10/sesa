/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository.master;

import com.sesa.salud.entity.master.TenantUsuarioLogin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantUsuarioLoginRepository extends JpaRepository<TenantUsuarioLogin, String> {

    Optional<TenantUsuarioLogin> findByEmail(String email);

    boolean existsByEmail(String email);

    List<TenantUsuarioLogin> findBySchemaName(String schemaName);
}
