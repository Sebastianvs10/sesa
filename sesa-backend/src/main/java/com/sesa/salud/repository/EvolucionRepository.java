/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.Evolucion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvolucionRepository extends JpaRepository<Evolucion, Long> {

    List<Evolucion> findByAtencion_IdOrderByFechaDesc(Long atencionId, Pageable pageable);

    /** Evoluciones de todas las atenciones (urgencias, etc.) de la historia del paciente. */
    List<Evolucion> findByAtencion_HistoriaClinica_Paciente_IdOrderByFechaDesc(Long pacienteId, Pageable pageable);
}
