/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.repository;

import com.sesa.salud.entity.AccesoAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccesoAuditoriaRepository extends JpaRepository<AccesoAuditoria, Long> {
    Page<AccesoAuditoria> findAllByOrderByFechaDesc(Pageable pageable);
}
