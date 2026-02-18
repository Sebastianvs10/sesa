/**
 * Autor: Ing. J Sebastian Vargas S
 */
package com.sesa.salud.repository;

import com.sesa.salud.entity.Hospitalizacion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HospitalizacionRepository extends JpaRepository<Hospitalizacion, Long> {
    List<Hospitalizacion> findByPaciente_IdOrderByFechaIngresoDesc(Long pacienteId, Pageable pageable);
    List<Hospitalizacion> findByEstadoOrderByFechaIngresoDesc(String estado, Pageable pageable);
}
