/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.repository;

import com.sesa.salud.entity.ReconciliacionAtencion;

import java.util.Optional;

public interface ReconciliacionAtencionRepository extends org.springframework.data.jpa.repository.JpaRepository<ReconciliacionAtencion, Long> {

    Optional<ReconciliacionAtencion> findByAtencion_Id(Long atencionId);
}
