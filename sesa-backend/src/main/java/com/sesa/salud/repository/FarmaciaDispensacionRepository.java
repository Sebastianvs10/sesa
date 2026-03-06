/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.repository;

import com.sesa.salud.entity.FarmaciaDispensacion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FarmaciaDispensacionRepository extends JpaRepository<FarmaciaDispensacion, Long> {
    List<FarmaciaDispensacion> findByPaciente_IdOrderByFechaDispensacionDesc(Long pacienteId, Pageable pageable);
    List<FarmaciaDispensacion> findByOrdenClinica_IdOrderByCreatedAtAsc(Long ordenClinicaId);
}
