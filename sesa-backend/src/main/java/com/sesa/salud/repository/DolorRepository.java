/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.repository;

import com.sesa.salud.entity.Dolor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DolorRepository extends JpaRepository<Dolor, Long> {

    List<Dolor> findByPaciente_IdOrderByFechaInicioDesc(Long pacienteId);

    List<Dolor> findByHistoriaClinica_IdOrderByFechaInicioDesc(Long historiaClinicaId);
}
