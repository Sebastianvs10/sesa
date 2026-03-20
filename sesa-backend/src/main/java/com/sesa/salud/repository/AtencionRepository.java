/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.repository;

import com.sesa.salud.entity.Atencion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AtencionRepository extends JpaRepository<Atencion, Long> {
    Page<Atencion> findByHistoriaClinicaId(Long historiaId, Pageable pageable);

    /** S4: atenciones en un rango de fechas (cumplimiento normativo). */
    List<Atencion> findByFechaAtencionBetween(Instant desde, Instant hasta);

    List<Atencion> findByFechaAtencionBetweenAndProfesional_Id(Instant desde, Instant hasta, Long profesionalId);
}
