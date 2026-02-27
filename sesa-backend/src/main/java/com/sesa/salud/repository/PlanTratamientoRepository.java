/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.PlanTratamiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanTratamientoRepository extends JpaRepository<PlanTratamiento, Long> {

    List<PlanTratamiento> findByPaciente_IdOrderByCreatedAtDesc(Long pacienteId);

    List<PlanTratamiento> findByPaciente_IdAndEstadoOrderByFase(Long pacienteId, String estado);
}
